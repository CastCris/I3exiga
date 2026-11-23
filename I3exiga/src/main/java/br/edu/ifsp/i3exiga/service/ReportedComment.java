package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.Comment;
import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.model.User;
import java.util.List;

/** Comentário com denúncias pendentes, exibido na fila de moderação dos administradores. */
public class ReportedComment {

    private final Comment comment;
    private final User author;
    private final Problem problem;
    private final List<String> reasons;

    public ReportedComment(Comment comment, User author, Problem problem, List<String> reasons) {
        this.comment = comment;
        this.author = author;
        this.problem = problem;
        this.reasons = reasons;
    }

    public Comment getComment() {
        return comment;
    }

    public User getAuthor() {
        return author;
    }

    public Problem getProblem() {
        return problem;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public int getReportCount() {
        return reasons.size();
    }
}
