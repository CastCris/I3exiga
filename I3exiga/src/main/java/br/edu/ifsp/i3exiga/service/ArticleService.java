package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.Article;
import br.edu.ifsp.i3exiga.model.ArticleType;
import br.edu.ifsp.i3exiga.model.ReviewStatus;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.store.Data;
import br.edu.ifsp.i3exiga.store.Sequence;
import br.edu.ifsp.i3exiga.store.Store;
import br.edu.ifsp.i3exiga.util.Text;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Artigos em PDF: submissão pelos docentes/administradores, avaliação pelos administradores e
 * regras de visibilidade (conhecimento geral é público; solução só após resolver o problema).
 */
public final class ArticleService {

    private static final long MAX_PDF_BYTES = 10L * 1024 * 1024;
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_TITLE = 150;
    private static final int MAX_FEEDBACK = 2000;

    private static final List<String> COMMON_CRITERIA = List.of(
            "Gramática correta",
            "Coerência semântica",
            "Coesão textual",
            "Segue o modelo LaTeX fornecido pelo sistema",
            "Sem termos sugestivos ou ofensivos",
            "Códigos (se houver) em C, C++, Java ou Python");

    private final Store store;
    private final SubmissionService submissions;
    private final Path directory;

    public ArticleService(Store store, SubmissionService submissions, Path directory) {
        this.store = store;
        this.submissions = submissions;
        this.directory = directory;
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Critérios que os administradores confirmam ao aprovar cada tipo de artigo. */
    public Map<ArticleType, List<String>> criteria() {
        Map<ArticleType, List<String>> criteria = new EnumMap<>(ArticleType.class);
        List<String> general = new ArrayList<>(COMMON_CRITERIA);
        general.add("Não apresenta solução de nenhum problema da plataforma");
        List<String> solution = new ArrayList<>(COMMON_CRITERIA);
        solution.add("Traz explicação lógica e explícita para cada solução");
        criteria.put(ArticleType.GENERAL, general);
        criteria.put(ArticleType.SOLUTION, solution);
        return criteria;
    }

    public Article get(long id) {
        Article article = store.data().articles.get(id);
        if (article == null) {
            throw new NotFoundException();
        }
        return article;
    }

    public Path pdfPath(long id) {
        return directory.resolve(id + ".pdf");
    }

    public boolean canManage(User user, Article article) {
        return user.isAdmin() || article.getAuthorIds().contains(user.getId());
    }

    /** Regras de visibilidade do PDF. O visitante pode ser {@code null} (não autenticado). */
    public boolean canView(User viewer, Article article) {
        if (viewer != null && canManage(viewer, article)) {
            return true;
        }
        if (article.getStatus() != ReviewStatus.APPROVED) {
            return false;
        }
        if (article.getType() == ArticleType.GENERAL) {
            return true;
        }
        return viewer != null && (viewer.isStaff()
                || submissions.hasSolved(viewer.getId(), article.getProblemId())
                || hasUnlocked(viewer, article.getProblemId()));
    }

    public boolean hasUnlocked(User user, Long problemId) {
        return store.data().unlocks.contains(Data.unlockKey(user.getId(), problemId));
    }

    /** Artigos de conhecimento geral aprovados (públicos). */
    public List<Article> approvedGeneral() {
        return store.data().articles.values().stream()
                .filter(a -> a.getType() == ArticleType.GENERAL && a.getStatus() == ReviewStatus.APPROVED)
                .sorted(Comparator.comparing(Article::getId).reversed()).toList();
    }

    /** Artigos de conhecimento geral aprovados, filtrados como em {@link ProblemService#search}. */
    public List<Article> searchGeneral(String name, Long id, String topic) {
        String needle = Text.fold(name);
        String wanted = Text.trim(topic).toLowerCase(Locale.ROOT);
        return approvedGeneral().stream()
                .filter(a -> id == null || a.getId().equals(id))
                .filter(a -> needle.isEmpty() || Text.fold(a.getTitle()).contains(needle))
                .filter(a -> wanted.isEmpty() || a.getTopics().contains(wanted))
                .toList();
    }

    /** Tópicos dos artigos de conhecimento geral aprovados. */
    public List<String> generalTopics() {
        return approvedGeneral().stream().flatMap(a -> a.getTopics().stream()).distinct().sorted().toList();
    }

    /** Artigos de solução de um problema: os aprovados e, para os autores/administradores, também os demais. */
    public List<Article> forProblem(User viewer, long problemId) {
        return store.data().articles.values().stream()
                .filter(a -> a.getType() == ArticleType.SOLUTION && a.getProblemId() == problemId)
                .filter(a -> a.getStatus() == ReviewStatus.APPROVED || canManage(viewer, a))
                .sorted(Comparator.comparing(Article::getId)).toList();
    }

    public List<Article> byAuthor(long userId) {
        return store.data().articles.values().stream()
                .filter(a -> a.getAuthorIds().contains(userId))
                .sorted(Comparator.comparing(Article::getId).reversed()).toList();
    }

    public List<Article> pendingReview() {
        return store.data().articles.values().stream()
                .filter(a -> a.getStatus() == ReviewStatus.PENDING)
                .sorted(Comparator.comparing(Article::getId)).toList();
    }

    /**
     * Cria ou atualiza um artigo. Todo envio volta o artigo para "em avaliação".
     *
     * @param authors autores do artigo (docentes ou administradores)
     * @param pdf     conteúdo do PDF; obrigatório na criação e opcional na edição
     */
    public Article save(Article draft, List<User> authors, User actor, byte[] pdf) {
        String title = Text.trim(draft.getTitle());
        if (title.isEmpty() || title.length() > MAX_TITLE) {
            throw new ValidationException("Informe um título de até " + MAX_TITLE + " caracteres.");
        }
        if (draft.getType() == null) {
            throw new ValidationException("Escolha o tipo do artigo.");
        }
        Long problemId = draft.getType() == ArticleType.SOLUTION ? draft.getProblemId() : null;
        if (draft.getType() == ArticleType.SOLUTION
                && (problemId == null || !store.data().problems.containsKey(problemId))) {
            throw new ValidationException("Informe o link de um problema existente para o artigo de solução.");
        }
        ProblemService.checkTopics(draft.getTopics());
        if (authors.isEmpty()) {
            throw new ValidationException("Informe ao menos um autor.");
        }
        for (User author : authors) {
            if (!author.isStaff()) {
                throw new ValidationException(author.getProntuario() + " não é docente nem administrador.");
            }
        }
        if (!actor.isAdmin() && authors.stream().noneMatch(a -> a.getId().equals(actor.getId()))) {
            throw new ValidationException("Você precisa permanecer na lista de autores.");
        }
        boolean creating = draft.getId() == null;
        boolean hasPdf = pdf != null && pdf.length > 0;
        if (creating && !hasPdf) {
            throw new ValidationException("Anexe o arquivo PDF do artigo.");
        }
        if (hasPdf) {
            validatePdf(pdf);
        }

        return store.transactionWithResult(() -> {
            Article target;
            if (creating) {
                target = new Article();
                target.setId(store.nextId(Sequence.ARTICLE));
            } else {
                target = get(draft.getId());
                if (!canManage(actor, target)) {
                    throw new AccessDeniedException();
                }
            }
            target.setTitle(title);
            target.setType(draft.getType());
            target.setProblemId(problemId);
            target.setTopics(new ArrayList<>(draft.getTopics()));
            target.setAuthorIds(authors.stream().map(User::getId).toList());
            target.setStatus(ReviewStatus.PENDING);
            target.setFeedback(null);
            target.setReviewerId(null);
            target.setSubmittedAt(Instant.now());
            if (hasPdf) {
                writePdf(target.getId(), pdf);
            }
            store.data().articles.put(target.getId(), target);
            return target;
        });
    }

    /** Avaliação feita por um administrador com base nos critérios do tipo do artigo. */
    public void review(User reviewer, long articleId, boolean approve, int confirmedCriteria, String feedback) {
        Article article = get(articleId);
        String text = Text.trim(feedback);
        if (text.length() > MAX_FEEDBACK) {
            throw new ValidationException("O parecer deve ter no máximo " + MAX_FEEDBACK + " caracteres.");
        }
        if (approve && confirmedCriteria < criteria().get(article.getType()).size()) {
            throw new ValidationException("Para aprovar, confirme todos os critérios de avaliação.");
        }
        if (!approve && text.isEmpty()) {
            throw new ValidationException("Explique aos autores o motivo da rejeição.");
        }
        store.transaction(() -> {
            article.setStatus(approve ? ReviewStatus.APPROVED : ReviewStatus.REJECTED);
            article.setFeedback(text.isEmpty() ? null : text);
            article.setReviewerId(reviewer.getId());
        });
    }

    /**
     * O usuário aceita ver os artigos de solução sem ter resolvido o problema. Em troca, o problema
     * deixa de pontuar para ele (veja {@link ScoreService}).
     */
    public void unlock(User user, long articleId) {
        Article article = get(articleId);
        if (article.getType() != ArticleType.SOLUTION || article.getStatus() != ReviewStatus.APPROVED) {
            throw new AccessDeniedException();
        }
        if (canView(user, article)) {
            return;
        }
        store.transaction(() -> store.data().unlocks.add(Data.unlockKey(user.getId(), article.getProblemId())));
    }

    public void delete(User actor, long id) {
        Article article = get(id);
        if (!canManage(actor, article)) {
            throw new AccessDeniedException();
        }
        store.transaction(() -> store.data().articles.remove(id));
        deleteFile(id);
    }

    public void deleteFile(long id) {
        try {
            Files.deleteIfExists(pdfPath(id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writePdf(long id, byte[] pdf) {
        try {
            Files.write(pdfPath(id), pdf);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void validatePdf(byte[] pdf) {
        if (pdf.length > MAX_PDF_BYTES) {
            throw new ValidationException("O PDF deve ter no máximo 10 MB.");
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (pdf.length <= i || pdf[i] != PDF_MAGIC[i]) {
                throw new ValidationException("O arquivo enviado não é um PDF válido.");
            }
        }
    }
}
