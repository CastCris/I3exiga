package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.judge.Judge;
import br.edu.ifsp.i3exiga.judge.JudgeResult;
import br.edu.ifsp.i3exiga.model.Language;
import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.model.Submission;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.model.Verdict;
import br.edu.ifsp.i3exiga.store.Sequence;
import br.edu.ifsp.i3exiga.store.Store;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Recebe submissões e as corrige, uma de cada vez, em uma fila. */
public final class SubmissionService implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(SubmissionService.class.getName());
    private static final int MAX_CODE_CHARS = 100_000;
    private static final int MAX_PENDING_PER_USER = 3;

    private final Store store;
    private final Judge judge;
    private final ExecutorService queue = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "i3exiga-judge");
        thread.setDaemon(true);
        return thread;
    });

    public SubmissionService(Store store, Judge judge) {
        this.store = store;
        this.judge = judge;
    }

    public Submission submit(User user, long problemId, String language, String code) {
        Language parsed = parseLanguage(language);
        if (code == null || code.isBlank()) {
            throw new ValidationException("Escreva o código da solução antes de enviar.");
        }
        if (code.length() > MAX_CODE_CHARS) {
            throw new ValidationException("O código deve ter no máximo " + MAX_CODE_CHARS + " caracteres.");
        }
        Submission submission = store.transactionWithResult(() -> {
            if (!store.data().problems.containsKey(problemId)) {
                throw new NotFoundException();
            }
            long pending = store.data().submissions.values().stream()
                    .filter(s -> s.getUserId().equals(user.getId()) && s.getVerdict().isPending()).count();
            if (pending >= MAX_PENDING_PER_USER) {
                throw new ValidationException("Aguarde a correção das suas submissões anteriores antes de enviar outra.");
            }
            Submission created = new Submission();
            created.setId(store.nextId(Sequence.SUBMISSION));
            created.setUserId(user.getId());
            created.setProblemId(problemId);
            created.setLanguage(parsed);
            created.setSourceCode(code.replace("\r\n", "\n"));
            created.setVerdict(Verdict.PENDING);
            created.setSubmittedAt(Instant.now());
            store.data().submissions.put(created.getId(), created);
            return created;
        });
        queue.execute(() -> judgeSubmission(submission.getId()));
        return submission;
    }

    /** Após uma reinicialização, volta a corrigir as submissões que ficaram na fila. */
    public void requeuePending() {
        store.data().submissions.values().stream()
                .filter(s -> s.getVerdict().isPending())
                .sorted(Comparator.comparing(Submission::getId))
                .forEach(s -> queue.execute(() -> judgeSubmission(s.getId())));
    }

    public Submission get(long id) {
        Submission submission = store.data().submissions.get(id);
        if (submission == null) {
            throw new NotFoundException();
        }
        return submission;
    }

    public List<Submission> byUserAndProblem(long userId, long problemId) {
        return store.data().submissions.values().stream()
                .filter(s -> s.getUserId() == userId && s.getProblemId() == problemId)
                .sorted(Comparator.comparing(Submission::getId).reversed()).toList();
    }

    public List<Submission> byUser(long userId) {
        return store.data().submissions.values().stream()
                .filter(s -> s.getUserId() == userId).toList();
    }

    /**
     * Busca submissões de todos os usuários; os critérios informados se combinam (E). Mais recentes primeiro.
     *
     * @param problemIds problemas que o solicitante pode ver (restringe o resultado)
     * @param problemId  problema específico ({@code null} = todos)
     * @param user       trecho do prontuário ou do nome (vazio = todos)
     * @param from       início do período, inclusive ({@code null} = sem limite)
     * @param to         fim do período, exclusive ({@code null} = sem limite)
     * @param verdict    tipo de resultado ({@code null} = todos)
     * @param language   linguagem ({@code null} = todas)
     */
    public List<Submission> search(java.util.Set<Long> problemIds, Long problemId, String user, Instant from, Instant to,
                                   Verdict verdict, Language language) {
        String needle = br.edu.ifsp.i3exiga.util.Text.fold(user);
        return store.data().submissions.values().stream()
                .filter(s -> problemIds.contains(s.getProblemId()))
                .filter(s -> problemId == null || s.getProblemId().equals(problemId))
                .filter(s -> from == null || !s.getSubmittedAt().isBefore(from))
                .filter(s -> to == null || s.getSubmittedAt().isBefore(to))
                .filter(s -> verdict == null || s.getVerdict() == verdict)
                .filter(s -> language == null || s.getLanguage() == language)
                .filter(s -> needle.isEmpty() || matchesUser(s.getUserId(), needle))
                .sorted(Comparator.comparing(Submission::getId).reversed()).toList();
    }

    private boolean matchesUser(Long userId, String needle) {
        User user = store.data().users.get(userId);
        return user != null && (br.edu.ifsp.i3exiga.util.Text.fold(user.getProntuario()).contains(needle)
                || br.edu.ifsp.i3exiga.util.Text.fold(user.getDisplayName()).contains(needle));
    }

    public boolean hasSolved(Long userId, Long problemId) {
        return store.data().submissions.values().stream().anyMatch(s -> s.getVerdict().isAccepted()
                && s.getUserId().equals(userId) && s.getProblemId().equals(problemId));
    }

    @Override
    public void close() {
        queue.shutdownNow();
    }

    private void judgeSubmission(long id) {
        Submission submission = store.data().submissions.get(id);
        Problem problem = submission == null ? null : store.data().problems.get(submission.getProblemId());
        if (problem == null) {
            return;
        }
        JudgeResult result;
        try {
            result = judge.judge(problem, submission);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Falha ao corrigir a submissão " + id, e);
            result = JudgeResult.internalError();
        }
        JudgeResult finalResult = result;
        store.transaction(() -> {
            Submission current = store.data().submissions.get(id);
            if (current != null) {
                current.setVerdict(finalResult.verdict());
                current.setFailedTestCase(finalResult.failedTestCase());
                current.setExpectedOutput(finalResult.expectedOutput());
                current.setReceivedOutput(finalResult.receivedOutput());
                current.setDetails(finalResult.details());
            }
        });
    }

    private static Language parseLanguage(String value) {
        try {
            return Language.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException("Escolha uma das linguagens disponíveis.");
        }
    }
}
