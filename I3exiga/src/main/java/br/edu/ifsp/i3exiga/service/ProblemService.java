package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.Article;
import br.edu.ifsp.i3exiga.model.ArticleType;
import br.edu.ifsp.i3exiga.model.Comment;
import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.model.ReviewStatus;
import br.edu.ifsp.i3exiga.model.TestCase;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.store.Data;
import br.edu.ifsp.i3exiga.store.Sequence;
import br.edu.ifsp.i3exiga.store.Store;
import br.edu.ifsp.i3exiga.util.Text;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cadastro de problemas. Um problema novo só aparece para os usuários depois da aprovação unânime dos
 * administradores (veja {@link ReviewService}); até lá só os autores e os administradores o enxergam.
 */
public final class ProblemService {

    public static final int DEFAULT_POINTS = 10;

    private static final int MAX_TITLE = 120;
    private static final int MAX_STATEMENT = 20_000;
    private static final int MAX_EXAMPLE = 5_000;
    private static final int MAX_TEST_DATA = 100_000;
    private static final int MAX_TESTS = 50;
    private static final int MAX_TOPICS = 5;
    private static final int MAX_TOPIC_LENGTH = 30;

    private final Store store;
    private final ArticleService articles;
    private final ReviewService reviews;

    public ProblemService(Store store, ArticleService articles, ReviewService reviews) {
        this.store = store;
        this.articles = articles;
        this.reviews = reviews;
    }

    public List<Problem> all() {
        return store.data().problems.values().stream().sorted(Comparator.comparing(Problem::getId)).toList();
    }

    public Problem get(long id) {
        Problem problem = store.data().problems.get(id);
        if (problem == null) {
            throw new NotFoundException();
        }
        return problem;
    }

    /** Autores e administradores gerenciam o problema (editar, remover). */
    public boolean canManage(User user, Problem problem) {
        return user.isAdmin() || problem.getAuthorIds().contains(user.getId());
    }

    /** Problemas aprovados são públicos; os demais só aparecem para os autores e administradores. */
    public boolean canView(User viewer, Problem problem) {
        return problem.getStatus() == ReviewStatus.APPROVED || (viewer != null && canManage(viewer, problem));
    }

    public Problem getVisible(User viewer, long id) {
        Problem problem = get(id);
        if (!canView(viewer, problem)) {
            throw new NotFoundException();
        }
        return problem;
    }

    public List<Problem> visibleTo(User viewer) {
        return all().stream().filter(p -> canView(viewer, p)).toList();
    }

    /** Problemas ainda não aprovados (em votação ou rejeitados). */
    public List<Problem> awaitingReview() {
        return all().stream().filter(p -> p.getStatus() != ReviewStatus.APPROVED).toList();
    }

    /**
     * Filtra os problemas visíveis ao usuário; os critérios informados se combinam (E). O nome ignora
     * maiúsculas e acentos.
     *
     * @param name  trecho do título (vazio = qualquer)
     * @param id    número do problema ({@code null} = qualquer)
     * @param topic tópico exato (vazio = qualquer)
     */
    public List<Problem> search(User viewer, String name, Long id, String topic) {
        String needle = Text.fold(name);
        String wanted = Text.trim(topic).toLowerCase(Locale.ROOT);
        return visibleTo(viewer).stream()
                .filter(p -> id == null || p.getId().equals(id))
                .filter(p -> needle.isEmpty() || Text.fold(p.getTitle()).contains(needle))
                .filter(p -> wanted.isEmpty() || p.getTopics().contains(wanted))
                .toList();
    }

    /** Tópicos dos problemas aprovados, em ordem alfabética (alimenta o filtro). */
    public List<String> topics() {
        return all().stream().filter(p -> p.getStatus() == ReviewStatus.APPROVED)
                .flatMap(p -> p.getTopics().stream()).distinct().sorted().toList();
    }

    /** Valida a lista de tópicos de um problema ou artigo. */
    static void checkTopics(List<String> topics) {
        if (topics.size() > MAX_TOPICS || topics.stream().anyMatch(t -> t.length() > MAX_TOPIC_LENGTH)) {
            throw new ValidationException("Use até " + MAX_TOPICS + " tópicos de até " + MAX_TOPIC_LENGTH + " caracteres.");
        }
    }

    /**
     * Cria ou atualiza um problema. Problemas novos (e os ainda não aprovados que forem editados) entram em
     * votação. Editar um problema já aprovado mantém a aprovação. Os pontos são definidos pelos administradores:
     * um problema criado por docente começa com {@link #DEFAULT_POINTS}.
     */
    public Problem save(Problem draft, List<User> authors, User actor) {
        List<TestCase> tests = cleanTestCases(draft.getTestCases());
        validate(draft, tests, actor);
        validateAuthors(authors, actor);
        List<Long> authorIds = authors.stream().map(User::getId).toList();
        return store.transactionWithResult(() -> {
            Problem target;
            if (draft.getId() == null) {
                target = new Problem();
                target.setId(store.nextId(Sequence.PROBLEM));
                target.setCreatedAt(Instant.now());
                target.setPoints(actor.isAdmin() ? draft.getPoints() : DEFAULT_POINTS);
                target.setStatus(ReviewStatus.PENDING);
            } else {
                target = get(draft.getId());
                if (!canManage(actor, target)) {
                    throw new AccessDeniedException();
                }
                if (actor.isAdmin()) {
                    target.setPoints(draft.getPoints());
                }
                if (target.getStatus() != ReviewStatus.APPROVED) {
                    target.setVotes(new ArrayList<>()); // novo envio: a votação recomeça
                    target.setStatus(ReviewStatus.PENDING);
                }
            }
            target.setAuthorIds(authorIds);
            target.setTitle(draft.getTitle().strip());
            target.setStatement(Text.normalizeNewlines(draft.getStatement()).strip());
            target.setExampleInput(Text.normalizeNewlines(draft.getExampleInput()));
            target.setExampleOutput(Text.normalizeNewlines(draft.getExampleOutput()));
            target.setTimeLimitMs(draft.getTimeLimitMs());
            target.setMemoryLimitMb(draft.getMemoryLimitMb());
            target.setTestCases(tests);
            target.setTopics(new ArrayList<>(draft.getTopics()));
            store.data().problems.put(target.getId(), target);
            return target;
        });
    }

    /** Voto de um administrador na aprovação do problema. */
    public void vote(User admin, long problemId, boolean approve, String comment) {
        reviews.cast(admin, get(problemId), approve, comment);
    }

    /** Remove o problema e tudo que depende dele (submissões, comentários, denúncias e artigos de solução). */
    public void delete(User actor, long id) {
        if (!canManage(actor, get(id))) {
            throw new AccessDeniedException();
        }
        List<Long> removedArticles = store.transactionWithResult(() -> {
            Data data = store.data();
            data.problems.remove(id);
            data.submissions.values().removeIf(s -> s.getProblemId() == id);
            Set<Long> commentIds = data.comments.values().stream()
                    .filter(c -> c.getProblemId() == id).map(Comment::getId).collect(Collectors.toSet());
            data.comments.keySet().removeAll(commentIds);
            data.reports.values().removeIf(r -> commentIds.contains(r.getCommentId()));
            data.unlocks.removeIf(key -> key.endsWith(":" + id));
            List<Long> articleIds = data.articles.values().stream()
                    .filter(a -> a.getType() == ArticleType.SOLUTION && Objects.equals(a.getProblemId(), id))
                    .map(Article::getId).toList();
            data.articles.keySet().removeAll(articleIds);
            return articleIds;
        });
        removedArticles.forEach(articles::deleteFile);
    }

    // ---- Casos de teste em texto: uma linha por caso -------------------------------------------------------

    /**
     * Converte as duas caixas de texto do formulário em casos de teste: a linha N das entradas combina com a
     * linha N das saídas. Dentro de uma linha, {@code \n} vira quebra de linha e {@code \\} vira uma barra.
     */
    public static List<TestCase> parseTestCases(String inputs, String outputs) {
        List<String> inputLines = lines(inputs);
        List<String> outputLines = lines(outputs);
        if (inputLines.size() != outputLines.size()) {
            throw new ValidationException("Há " + inputLines.size() + " linha(s) de entrada e " + outputLines.size()
                    + " de saída: cada caso de teste precisa de uma entrada e de uma saída, na mesma linha.");
        }
        List<TestCase> tests = new ArrayList<>();
        for (int i = 0; i < inputLines.size(); i++) {
            tests.add(new TestCase(unescape(inputLines.get(i)), unescape(outputLines.get(i))));
        }
        return tests;
    }

    public static String formatInputs(List<TestCase> tests) {
        return tests.stream().map(t -> escape(t.getInput())).collect(Collectors.joining("\n"));
    }

    public static String formatOutputs(List<TestCase> tests) {
        return tests.stream().map(t -> escape(t.getExpectedOutput())).collect(Collectors.joining("\n"));
    }

    private static List<String> lines(String text) {
        List<String> lines = new ArrayList<>(Arrays.asList(Text.normalizeNewlines(text).split("\n", -1)));
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    private static String unescape(String line) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length() && (line.charAt(i + 1) == 'n' || line.charAt(i + 1) == '\\')) {
                out.append(line.charAt(++i) == 'n' ? '\n' : '\\');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String escape(String text) {
        String trimmed = text.replaceAll("\n+$", "");
        return trimmed.replace("\\", "\\\\").replace("\n", "\\n");
    }

    // ---- Validação ------------------------------------------------------------------------------------------

    private static List<TestCase> cleanTestCases(List<TestCase> raw) {
        List<TestCase> clean = new ArrayList<>();
        for (TestCase test : raw) {
            String input = Text.normalizeNewlines(test.getInput());
            String output = Text.normalizeNewlines(test.getExpectedOutput());
            if (input.isBlank() && output.isBlank()) {
                continue;
            }
            boolean endsWithNewline = input.isEmpty() || input.endsWith("\n");
            clean.add(new TestCase(endsWithNewline ? input : input + "\n", output));
        }
        return clean;
    }

    private static void validateAuthors(List<User> authors, User actor) {
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
    }

    private static void validate(Problem draft, List<TestCase> tests, User actor) {
        if (Text.trim(draft.getTitle()).isEmpty() || draft.getTitle().strip().length() > MAX_TITLE) {
            throw new ValidationException("Informe um título de até " + MAX_TITLE + " caracteres.");
        }
        if (Text.trim(draft.getStatement()).isEmpty() || draft.getStatement().length() > MAX_STATEMENT) {
            throw new ValidationException("Informe o enunciado (até " + MAX_STATEMENT + " caracteres).");
        }
        if (Text.trim(draft.getExampleInput()).length() > MAX_EXAMPLE || Text.trim(draft.getExampleOutput()).length() > MAX_EXAMPLE) {
            throw new ValidationException("Os exemplos devem ter no máximo " + MAX_EXAMPLE + " caracteres.");
        }
        checkTopics(draft.getTopics());
        if (draft.getTimeLimitMs() < 100 || draft.getTimeLimitMs() > 10_000) {
            throw new ValidationException("O limite de tempo deve ficar entre 100 e 10000 ms.");
        }
        if (draft.getMemoryLimitMb() < 16 || draft.getMemoryLimitMb() > 512) {
            throw new ValidationException("O limite de memória deve ficar entre 16 e 512 MB.");
        }
        if (actor.isAdmin() && (draft.getPoints() < 1 || draft.getPoints() > 1000)) {
            throw new ValidationException("A pontuação deve ficar entre 1 e 1000.");
        }
        if (tests.isEmpty() || tests.size() > MAX_TESTS) {
            throw new ValidationException("Cadastre de 1 a " + MAX_TESTS + " casos de teste.");
        }
        for (int i = 0; i < tests.size(); i++) {
            TestCase test = tests.get(i);
            if (test.getExpectedOutput().isBlank()) {
                throw new ValidationException("O caso de teste " + (i + 1) + " precisa de uma saída esperada.");
            }
            if (test.getInput().length() > MAX_TEST_DATA || test.getExpectedOutput().length() > MAX_TEST_DATA) {
                throw new ValidationException("O caso de teste " + (i + 1) + " excede " + MAX_TEST_DATA + " caracteres.");
            }
        }
    }
}
