package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.Comment;
import br.edu.ifsp.i3exiga.model.Report;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.store.Data;
import br.edu.ifsp.i3exiga.store.Sequence;
import br.edu.ifsp.i3exiga.store.Store;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Comentários dos problemas e fluxo de denúncia e moderação. */
public final class CommentService {

    public static final List<String> REPORT_REASONS = List.of(
            "Termos profanos ou ofensivos",
            "Contém código com a solução",
            "Não está relacionado à solução do problema");

    private final Store store;
    private final UserService users;
    private final CommentPolicy policy = new CommentPolicy();

    public CommentService(Store store, UserService users) {
        this.store = store;
        this.users = users;
    }

    public List<Comment> list(long problemId) {
        return store.data().comments.values().stream()
                .filter(c -> c.getProblemId() == problemId)
                .sorted(Comparator.comparing(Comment::getId)).toList();
    }

    public Comment add(User author, long problemId, String text) {
        String clean = policy.check(text);
        return store.transactionWithResult(() -> {
            if (!store.data().problems.containsKey(problemId)) {
                throw new NotFoundException();
            }
            Comment comment = new Comment();
            comment.setId(store.nextId(Sequence.COMMENT));
            comment.setProblemId(problemId);
            comment.setAuthorId(author.getId());
            comment.setText(clean);
            comment.setCreatedAt(Instant.now());
            store.data().comments.put(comment.getId(), comment);
            return comment;
        });
    }

    public void report(User reporter, long commentId, String reason) {
        if (!REPORT_REASONS.contains(reason)) {
            throw new ValidationException("Escolha um dos motivos de denúncia disponíveis.");
        }
        store.transaction(() -> {
            Data data = store.data();
            Comment comment = data.comments.get(commentId);
            if (comment == null) {
                throw new NotFoundException();
            }
            if (comment.getAuthorId().equals(reporter.getId())) {
                throw new ValidationException("Você não pode denunciar o seu próprio comentário.");
            }
            boolean alreadyReported = data.reports.values().stream()
                    .anyMatch(r -> r.getCommentId() == commentId && r.getReporterId().equals(reporter.getId()));
            if (alreadyReported) {
                throw new ValidationException("Você já denunciou este comentário.");
            }
            Report report = new Report();
            report.setId(store.nextId(Sequence.REPORT));
            report.setCommentId(commentId);
            report.setReporterId(reporter.getId());
            report.setReason(reason);
            report.setCreatedAt(Instant.now());
            data.reports.put(report.getId(), report);
        });
    }

    /** Fila de moderação: comentários com denúncias, os mais denunciados primeiro. */
    public List<ReportedComment> pendingReports() {
        Data data = store.data();
        Map<Long, List<String>> reasonsByComment = data.reports.values().stream()
                .sorted(Comparator.comparing(Report::getId))
                .collect(Collectors.groupingBy(Report::getCommentId, Collectors.mapping(Report::getReason, Collectors.toList())));
        List<ReportedComment> queue = new ArrayList<>();
        reasonsByComment.forEach((commentId, reasons) -> {
            Comment comment = data.comments.get(commentId);
            if (comment != null) {
                queue.add(new ReportedComment(comment, data.users.get(comment.getAuthorId()),
                        data.problems.get(comment.getProblemId()), reasons));
            }
        });
        queue.sort(Comparator.comparingInt(ReportedComment::getReportCount).reversed()
                .thenComparing(item -> item.getComment().getId()));
        return queue;
    }

    /**
     * Resolve as denúncias de um comentário.
     *
     * @param action  "dismiss" (rejeita), "delete" (exclui), "ban-temporary" (exclui e bane por {@code days}
     *                dias) ou "ban-permanent" (exclui e bane indeterminadamente)
     */
    public void resolve(User admin, long commentId, String action, Integer days) {
        Comment comment = store.data().comments.get(commentId);
        if (comment == null) {
            throw new NotFoundException();
        }
        switch (action) {
            case "dismiss", "delete" -> { }
            case "ban-temporary" -> users.ban(admin, comment.getAuthorId(), days == null ? Integer.valueOf(0) : days);
            case "ban-permanent" -> users.ban(admin, comment.getAuthorId(), null);
            default -> throw new ValidationException("Ação de moderação inválida.");
        }
        store.transaction(() -> {
            store.data().reports.values().removeIf(r -> r.getCommentId() == commentId);
            if (!action.equals("dismiss")) {
                store.data().comments.remove(commentId);
            }
        });
    }
}
