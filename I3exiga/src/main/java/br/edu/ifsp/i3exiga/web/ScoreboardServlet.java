package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.model.Scoreboard;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.service.AccessDeniedException;
import br.edu.ifsp.i3exiga.service.NotFoundException;
import br.edu.ifsp.i3exiga.service.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/** Placar geral e placares personalizados. */
@WebServlet("/scoreboards/*")
public class ScoreboardServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = requireUser(request);
        switch (path(request)) {
            case "", "/" -> {
                request.setAttribute("boards", services.scoreboards.visibleTo(user));
                render(request, response, "scoreboards");
            }
            case "/general" -> showRanking(request, response, "Placar geral", "/scoreboards/general", null, null);
            case "/view" -> {
                Scoreboard board = services.scoreboards.get(idParam(request, "id"));
                if (!services.scoreboards.canView(user, board)) {
                    throw new AccessDeniedException();
                }
                request.setAttribute("board", board);
                request.setAttribute("canManage", services.scoreboards.canManage(user, board));
                request.setAttribute("isMember", board.getMemberIds().contains(user.getId()));
                showRanking(request, response, board.getName(), "/scoreboards/view", board.getMemberIds(), board.getId());
            }
            case "/new" -> {
                requireStaff(request);
                Scoreboard draft = new Scoreboard();
                draft.setPublicAccess(true);
                showForm(request, response, draft, "");
            }
            case "/edit" -> {
                requireStaff(request);
                Scoreboard board = services.scoreboards.get(idParam(request, "id"));
                if (!services.scoreboards.canManage(user, board)) {
                    throw new AccessDeniedException();
                }
                String members = board.getMemberIds().stream()
                        .map(id -> services.users.find(id).map(User::getProntuario).orElse(""))
                        .filter(p -> !p.isEmpty()).collect(Collectors.joining("\n"));
                showForm(request, response, board, members);
            }
            default -> throw new NotFoundException();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = requireUser(request);
        switch (path(request)) {
            case "/save" -> save(request, response, requireStaff(request));
            case "/join" -> {
                long id = idParam(request, "id");
                services.scoreboards.join(user, id);
                redirect(request, response, "/scoreboards/view?id=" + id);
            }
            case "/leave" -> {
                services.scoreboards.leave(user, idParam(request, "id"));
                redirect(request, response, "/scoreboards");
            }
            case "/delete" -> {
                services.scoreboards.delete(user, idParam(request, "id"));
                flash(request, "Placar removido.");
                redirect(request, response, "/scoreboards");
            }
            default -> throw new NotFoundException();
        }
    }

    private void showRanking(HttpServletRequest request, HttpServletResponse response, String title, String action,
                             java.util.Set<Long> memberIds, Long boardId) throws ServletException, IOException {
        String query = param(request, "q");
        boolean ascending = "asc".equals(param(request, "order"));
        request.setAttribute("title", title);
        request.setAttribute("formAction", action);
        request.setAttribute("boardId", boardId);
        request.setAttribute("q", query);
        request.setAttribute("ascending", ascending);
        request.setAttribute("rows", services.scores.ranking(memberIds, query, ascending));
        render(request, response, "scoreboard");
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, Scoreboard board, String members)
            throws ServletException, IOException {
        request.setAttribute("board", board);
        request.setAttribute("members", members);
        render(request, response, "scoreboard-form");
    }

    private void save(HttpServletRequest request, HttpServletResponse response, User user) throws ServletException, IOException {
        Scoreboard draft = new Scoreboard();
        if (!param(request, "id").isBlank()) {
            draft.setId(idParam(request, "id"));
        }
        draft.setName(param(request, "name"));
        draft.setDescription(param(request, "description"));
        draft.setPublicAccess(request.getParameter("publicAccess") != null);
        String membersText = param(request, "members");
        try {
            List<User> members = services.users.findAllByProntuarios(membersText);
            Scoreboard saved = services.scoreboards.save(draft, members, user);
            flash(request, "Placar salvo.");
            redirect(request, response, "/scoreboards/view?id=" + saved.getId());
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            showForm(request, response, draft, membersText);
        }
    }
}
