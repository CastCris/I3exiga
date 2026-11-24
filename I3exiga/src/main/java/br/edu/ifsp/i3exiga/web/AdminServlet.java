package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.model.Article;
import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.model.Role;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.service.NotFoundException;
import br.edu.ifsp.i3exiga.service.ReviewItem;
import br.edu.ifsp.i3exiga.service.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            case "/reviews" -> {
                request.setAttribute("items", reviewItems());
                render(request, response, "admin-reviews");
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
                services.reviews.reevaluate(); // muda quem precisa votar
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
                services.reviews.reevaluate();
                flash(request, "Situação do usuário atualizada.");
                next = "/admin/users";
            }
            case "/reports/resolve" -> {
                services.comments.resolve(admin, idParam(request, "commentId"), param(request, "action"),
                        intParam(request, "days", 0));
                flash(request, "Denúncia analisada.");
                next = "/admin/reports";
            }
            case "/reviews/vote" -> {
                vote(request, admin);
                flash(request, "Voto registrado.");
                redirect(request, response, previousPage(request, "/admin/reviews"));
                return;
            }
            default -> throw new NotFoundException();
        }
        redirect(request, response, next);
    }

    private void vote(HttpServletRequest request, User admin) {
        long id = idParam(request, "id");
        boolean approve = "approve".equals(param(request, "decision"));
        String comment = param(request, "comment");
        switch (param(request, "kind")) {
            case "problem" -> services.problems.vote(admin, id, approve, comment);
            case "article" -> {
                String[] confirmed = request.getParameterValues("criteria");
                services.articles.vote(admin, id, approve, confirmed == null ? 0 : confirmed.length, comment);
            }
            default -> throw new NotFoundException();
        }
    }

    /** Problemas e artigos que ainda não foram aprovados, com a situação da votação de cada administrador. */
    private List<ReviewItem> reviewItems() {
        List<ReviewItem> items = new ArrayList<>();
        for (Problem problem : services.problems.awaitingReview()) {
            items.add(new ReviewItem("problem", problem.getId(), problem.getTitle(), "Problema · " + problem.getPoints() + " pontos",
                    "/problems/view?id=" + problem.getId(), problem.getStatus(), authors(problem.getAuthorIds()),
                    services.reviews.ballots(problem), List.of()));
        }
        for (Article article : services.articles.awaitingReview()) {
            items.add(new ReviewItem("article", article.getId(), article.getTitle(), "Artigo · " + article.getType().getLabel(),
                    "/articles/view?id=" + article.getId(), article.getStatus(), authors(article.getAuthorIds()),
                    services.reviews.ballots(article), services.articles.criteria().get(article.getType())));
        }
        return items;
    }

    private List<User> authors(List<Long> ids) {
        return ids.stream().map(services.users::find).flatMap(Optional::stream).toList();
    }

    private static Role parseRole(String value) {
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Categoria inválida.");
        }
    }
}
