package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.model.Role;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.service.NotFoundException;
import br.edu.ifsp.i3exiga.service.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Área da equipe de administradores: usuários, moderação de denúncias e avaliação de artigos. */
@WebServlet("/admin/*")
public class AdminServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        requireAdmin(request);
        switch (path(request)) {
            case "/users" -> {
                request.setAttribute("users", services.users.all());
                request.setAttribute("roles", Role.values());
                render(request, response, "admin-users");
            }
            case "/reports" -> {
                request.setAttribute("reports", services.comments.pendingReports());
                render(request, response, "admin-reports");
            }
            case "/articles" -> {
                request.setAttribute("articles", services.articles.pendingReview());
                request.setAttribute("criteria", services.articles.criteria());
                request.setAttribute("usersById", services.users.mapById());
                render(request, response, "admin-articles");
            }
            default -> throw new NotFoundException();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User admin = requireAdmin(request);
        String next;
        switch (path(request)) {
            case "/users/role" -> {
                services.users.changeRole(admin, idParam(request, "userId"), parseRole(param(request, "role")));
                flash(request, "Categoria alterada.");
                next = "/admin/users";
            }
            case "/users/ban" -> {
                long userId = idParam(request, "userId");
                switch (param(request, "mode")) {
                    case "permanent" -> services.users.ban(admin, userId, null);
                    case "temporary" -> services.users.ban(admin, userId, intParam(request, "days", 0));
                    case "unban" -> services.users.unban(userId);
                    default -> throw new ValidationException("Escolha uma ação.");
                }
                flash(request, "Situação do usuário atualizada.");
                next = "/admin/users";
            }
            case "/reports/resolve" -> {
                services.comments.resolve(admin, idParam(request, "commentId"), param(request, "action"),
                        intParam(request, "days", 0));
                flash(request, "Denúncia analisada.");
                next = "/admin/reports";
            }
            case "/articles/review" -> {
                String[] confirmed = request.getParameterValues("criteria");
                services.articles.review(admin, idParam(request, "articleId"), "approve".equals(param(request, "decision")),
                        confirmed == null ? 0 : confirmed.length, param(request, "feedback"));
                flash(request, "Avaliação registrada.");
                next = "/admin/articles";
            }
            default -> throw new NotFoundException();
        }
        redirect(request, response, next);
    }

    private static Role parseRole(String value) {
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Categoria inválida.");
        }
    }
}
