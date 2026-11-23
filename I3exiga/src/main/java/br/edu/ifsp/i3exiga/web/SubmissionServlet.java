package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.model.Submission;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.service.AccessDeniedException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Resultado de uma submissão (visível ao autor e à equipe). */
@WebServlet("/submissions")
public class SubmissionServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = requireUser(request);
        Submission submission = services.submissions.get(idParam(request, "id"));
        if (!submission.getUserId().equals(user.getId()) && !user.isStaff()) {
            throw new AccessDeniedException();
        }
        request.setAttribute("submission", submission);
        request.setAttribute("problem", services.problems.get(submission.getProblemId()));
        render(request, response, "submission");
    }
}
