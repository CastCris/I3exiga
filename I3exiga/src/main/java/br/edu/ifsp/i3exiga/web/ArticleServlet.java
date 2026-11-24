package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.model.Article;
import br.edu.ifsp.i3exiga.model.ArticleType;
import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.model.ReviewStatus;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.service.AccessDeniedException;
import br.edu.ifsp.i3exiga.service.NotFoundException;
import br.edu.ifsp.i3exiga.service.ValidationException;
import br.edu.ifsp.i3exiga.util.Text;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Artigos: listagem pública, artigos por problema, envio, edição, avaliação (em /admin) e download do PDF. */
@WebServlet("/articles/*")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 11 * 1024 * 1024)
public class ArticleServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        switch (path(request)) {
            case "", "/" -> list(request, response);
            case "/view" -> view(request, response);
            case "/file" -> sendPdf(request, response);
            case "/new" -> {
                User user = requireStaff(request);
                Article draft = new Article();
                String problemId = param(request, "problemId");
                draft.setType(problemId.isBlank() ? ArticleType.GENERAL : ArticleType.SOLUTION);
                draft.setProblemId(parseLong(problemId));
                showForm(request, response, draft, user.getProntuario());
            }
            case "/edit" -> {
                User user = requireStaff(request);
                Article article = services.articles.get(idParam(request, "id"));
                if (!services.articles.canManage(user, article)) {
                    throw new AccessDeniedException();
                }
                showForm(request, response, article, prontuarios(article.getAuthorIds()));
            }
            default -> throw new NotFoundException();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        switch (path(request)) {
            case "/save" -> save(request, response);
            case "/unlock" -> {
                long id = idParam(request, "id");
                services.articles.unlock(requireUser(request), id);
                redirect(request, response, "/articles/view?id=" + id);
            }
            case "/delete" -> {
                services.articles.delete(requireUser(request), idParam(request, "id"));
                flash(request, "Artigo removido.");
                redirect(request, response, "/articles");
            }
            default -> throw new NotFoundException();
        }
    }

    /** Sem problemId: artigos de conhecimento geral (público). Com problemId: artigos de solução do problema. */
    private void list(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("usersById", services.users.mapById());
        if (!param(request, "problemId").isBlank()) {
            User user = requireUser(request);
            long problemId = idParam(request, "problemId");
            request.setAttribute("problem", services.problems.getVisible(user, problemId));
            request.setAttribute("articles", services.articles.forProblem(user, problemId));
            render(request, response, "problem-articles");
            return;
        }
        User user = user(request);
        request.setAttribute("articles", services.articles.searchGeneral(param(request, "q"), filterId(request), param(request, "topic")));
        request.setAttribute("total", services.articles.approvedGeneral().size());
        request.setAttribute("topics", services.articles.generalTopics());
        request.setAttribute("q", param(request, "q"));
        request.setAttribute("topic", param(request, "topic"));
        request.setAttribute("idFilter", param(request, "id"));
        if (user != null && user.isStaff()) {
            request.setAttribute("mine", services.articles.byAuthor(user.getId()));
        }
        render(request, response, "articles");
    }

    private void view(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User viewer = user(request);
        Article article = services.articles.get(idParam(request, "id"));
        boolean canView = services.articles.canView(viewer, article);
        boolean isApprovedSolution = article.getType() == ArticleType.SOLUTION && article.getStatus() == ReviewStatus.APPROVED;
        // Quem ainda não pode ver um artigo de solução aprovado pode aceitar abri-lo em troca dos pontos do problema.
        boolean canUnlock = viewer != null && isApprovedSolution && !canView;
        if (!canView && !isApprovedSolution) {
            throw new NotFoundException();
        }
        request.setAttribute("article", article);
        request.setAttribute("canView", canView);
        request.setAttribute("canUnlock", canUnlock);
        request.setAttribute("canManage", viewer != null && services.articles.canManage(viewer, article));
        request.setAttribute("usersById", services.users.mapById());
        if (article.getProblemId() != null) {
            Problem problem = services.problems.get(article.getProblemId());
            if (services.problems.canView(viewer, problem)) {
                request.setAttribute("problem", problem);
            }
        }
        if (viewer != null && services.articles.canManage(viewer, article)) {
            request.setAttribute("ballots", services.reviews.ballots(article));
            request.setAttribute("criteria", services.articles.criteria().get(article.getType()));
            request.setAttribute("canVote", viewer.isAdmin() && article.getStatus() != ReviewStatus.APPROVED);
        }
        render(request, response, "article");
    }

    private void sendPdf(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Article article = services.articles.get(idParam(request, "id"));
        if (!services.articles.canView(user(request), article)) {
            throw new AccessDeniedException();
        }
        Path file = services.articles.pdfPath(article.getId());
        if (!Files.exists(file)) {
            throw new NotFoundException();
        }
        response.setContentType("application/pdf");
        response.setContentLengthLong(Files.size(file));
        response.setHeader("Content-Disposition", "inline; filename=\"artigo-" + article.getId() + ".pdf\"");
        Files.copy(file, response.getOutputStream());
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, Article article, String authors)
            throws ServletException, IOException {
        request.setAttribute("article", article);
        request.setAttribute("authors", authors);
        request.setAttribute("types", ArticleType.values());
        request.setAttribute("knownTopics", Stream.concat(services.problems.topics().stream(), services.articles.generalTopics().stream())
                .distinct().sorted().toList());
        request.setAttribute("problems", services.problems.visibleTo(requireUser(request)));
        render(request, response, "article-form");
    }

    private void save(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = requireStaff(request);
        Article draft = new Article();
        if (!param(request, "id").isBlank()) {
            draft.setId(idParam(request, "id"));
        }
        draft.setTitle(param(request, "title"));
        draft.setType(parseType(param(request, "type")));
        draft.setProblemId(parseLong(param(request, "problemId")));
        draft.setTopics(Text.splitTopics(param(request, "topics")));
        String authorsText = param(request, "authors");
        try {
            List<User> authors = services.users.findAllByProntuarios(authorsText);
            Article saved = services.articles.save(draft, authors, user, readPdf(request));
            flash(request, "Artigo enviado para avaliação da equipe de administradores.");
            redirect(request, response, "/articles/view?id=" + saved.getId());
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            showForm(request, response, draft, authorsText);
        }
    }

    private byte[] readPdf(HttpServletRequest request) throws IOException, ServletException {
        try {
            Part part = request.getPart("pdf");
            return part == null ? null : part.getInputStream().readAllBytes();
        } catch (IllegalStateException e) {
            throw new ValidationException("O PDF deve ter no máximo 10 MB.");
        }
    }

    private String prontuarios(List<Long> userIds) {
        return userIds.stream().map(id -> services.users.find(id).map(User::getProntuario).orElse(""))
                .filter(p -> !p.isEmpty()).collect(Collectors.joining("\n"));
    }

    private static ArticleType parseType(String value) {
        try {
            return ArticleType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Long parseLong(String value) {
        try {
            return Long.valueOf(value.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
