package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.service.CommentService;
import br.edu.ifsp.i3exiga.service.NotFoundException;
import br.edu.ifsp.i3exiga.service.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Aba de comentários de cada problema, com denúncia de comentários. */
@WebServlet("/comments/*")
public class CommentServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!path(request).isEmpty() && !path(request).equals("/")) {
            throw new NotFoundException();
        }
        showComments(request, response, services.problems.get(idParam(request, "problemId")));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long problemId = idParam(request, "problemId");
        switch (path(request)) {
            case "/add" -> {
                try {
                    services.comments.add(requireUser(request), problemId, param(request, "text"));
                    flash(request, "Comentário publicado.");
                } catch (ValidationException e) {
                    request.setAttribute("error", e.getMessage());
                    request.setAttribute("text", param(request, "text"));
                    showComments(request, response, services.problems.get(problemId));
                    return;
                }
            }
            case "/report" -> {
                services.comments.report(requireUser(request), idParam(request, "commentId"), param(request, "reason"));
                flash(request, "Denúncia enviada. Os administradores vão analisá-la.");
            }
            default -> throw new NotFoundException();
        }
        redirect(request, response, "/comments?problemId=" + problemId);
    }

    private void showComments(HttpServletRequest request, HttpServletResponse response, Problem problem)
            throws ServletException, IOException {
        request.setAttribute("problem", problem);
        request.setAttribute("comments", services.comments.list(problem.getId()));
        request.setAttribute("usersById", services.users.mapById());
        request.setAttribute("reasons", CommentService.REPORT_REASONS);
        render(request, response, "problem-comments");
    }
}
