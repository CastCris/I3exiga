package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.Article;
import br.edu.ifsp.i3exiga.model.ArticleType;
import br.edu.ifsp.i3exiga.model.Comment;
import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.model.TestCase;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.store.Data;
import br.edu.ifsp.i3exiga.store.Sequence;
import br.edu.ifsp.i3exiga.store.Store;
import br.edu.ifsp.i3exiga.util.Text;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Cadastro de problemas (somente administradores e docentes). */
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

    public ProblemService(Store store, ArticleService articles) {
        this.store = store;
        this.articles = articles;
    }

    public List<Problem> all() {
        return store.data().problems.values().stream().sorted(Comparator.comparing(Problem::getId)).toList();
    }

    /**
     * Filtra os problemas; os critérios informados se combinam (E). O nome ignora maiúsculas e acentos.
     *
     * @param name  trecho do título (vazio = qualquer)
     * @param id    número do problema ({@code null} = qualquer)
     * @param topic tópico exato (vazio = qualquer)
     */
    public List<Problem> search(String name, Long id, String topic) {
        String needle = Text.fold(name);
        String wanted = Text.trim(topic).toLowerCase(Locale.ROOT);
        return all().stream()
                .filter(p -> id == null || p.getId().equals(id))
                .filter(p -> needle.isEmpty() || Text.fold(p.getTitle()).contains(needle))
                .filter(p -> wanted.isEmpty() || p.getTopics().contains(wanted))
                .toList();
    }

    /** Todos os tópicos em uso, em ordem alfabética (alimenta o filtro). */
    public List<String> topics() {
        return store.data().problems.values().stream().flatMap(p -> p.getTopics().stream())
                .distinct().sorted().toList();
    }

    /** Valida a lista de tópicos de um problema ou artigo. */
    static void checkTopics(List<String> topics) {
        if (topics.size() > MAX_TOPICS || topics.stream().anyMatch(t -> t.length() > MAX_TOPIC_LENGTH)) {
            throw new ValidationException("Use até " + MAX_TOPICS + " tópicos de até " + MAX_TOPIC_LENGTH + " caracteres.");
        }
    }

    public Problem get(long id) {
        Problem problem = store.data().problems.get(id);
        if (problem == null) {
            throw new NotFoundException();
        }
        return problem;
    }

    /**
     * Cria ou atualiza um problema. Os pontos são definidos pelos administradores: quando um docente
     * cria um problema ele recebe {@link #DEFAULT_POINTS} até que um administrador ajuste o valor.
     */
    public Problem save(Problem draft, User actor) {
        List<TestCase> tests = cleanTestCases(draft.getTestCases());
        validate(draft, tests, actor);
        return store.transactionWithResult(() -> {
            Problem target;
            if (draft.getId() == null) {
                target = new Problem();
                target.setId(store.nextId(Sequence.PROBLEM));
                target.setAuthorId(actor.getId());
                target.setCreatedAt(Instant.now());
                target.setPoints(actor.isAdmin() ? draft.getPoints() : DEFAULT_POINTS);
            } else {
                target = get(draft.getId());
                if (actor.isAdmin()) {
                    target.setPoints(draft.getPoints());
                }
            }
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

    /** Remove o problema e tudo que depende dele (submissões, comentários, denúncias e artigos de solução). */
    public void delete(long id) {
        get(id);
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
