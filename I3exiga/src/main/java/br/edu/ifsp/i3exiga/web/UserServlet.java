package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.service.SubmissionChart;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Página de um usuário: nome, matrícula, pontuação, curso e gráfico de submissões. */
@WebServlet("/users")
public class UserServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        requireUser(request);
        User profile = services.users.get(idParam(request, "id"));
        request.setAttribute("profile", profile);
        request.setAttribute("score", services.scores.scoreOf(profile.getId()));
        request.setAttribute("solvedCount", services.scores.solvedBy(profile.getId()).size());
        request.setAttribute("chart", SubmissionChart.render(services.submissions.byUser(profile.getId())));
        render(request, response, "user");
    }
}
