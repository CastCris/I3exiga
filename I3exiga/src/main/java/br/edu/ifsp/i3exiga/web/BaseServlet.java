package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.service.AccessDeniedException;
import br.edu.ifsp.i3exiga.service.NotFoundException;
import br.edu.ifsp.i3exiga.service.Services;
import br.edu.ifsp.i3exiga.service.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;

/**
 * Base dos servlets: acesso aos serviços, renderização das páginas, mensagens (flash),
 * leitura de parâmetros e tratamento padrão de erros.
 */
public abstract class BaseServlet extends HttpServlet {

    private static final String FLASH = "flash";
    private static final String FLASH_ERROR = "flashError";

    protected Services services;

    @Override
    public void init() {
        services = (Services) getServletContext().getAttribute(AppListener.SERVICES);
    }

    /** Converte as exceções de negócio em respostas HTTP, para os servlets não repetirem esse código. */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            super.service(request, response);
        } catch (NotFoundException e) {
            showError(request, response, HttpServletResponse.SC_NOT_FOUND, "Página ou recurso não encontrado.");
        } catch (AccessDeniedException e) {
            showError(request, response, HttpServletResponse.SC_FORBIDDEN, "Você não tem permissão para acessar este recurso.");
        } catch (ValidationException e) {
            // Erro em uma ação sem formulário próprio: mostra a mensagem e volta para a página anterior.
            flashError(request, e.getMessage());
            redirect(request, response, previousPage(request));
        }
    }

    protected void render(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        moveToRequest(session, request, FLASH);
        moveToRequest(session, request, FLASH_ERROR);
        request.getRequestDispatcher("/WEB-INF/jsp/" + view + ".jsp").forward(request, response);
    }

    protected void redirect(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    protected void flash(HttpServletRequest request, String message) {
        request.getSession().setAttribute(FLASH, message);
    }

    protected void flashError(HttpServletRequest request, String message) {
        request.getSession().setAttribute(FLASH_ERROR, message);
    }

    protected User user(HttpServletRequest request) {
        return (User) request.getAttribute("currentUser");
    }

    /** Usuário autenticado; páginas públicas que exigem login em parte da funcionalidade usam este método. */
    protected User requireUser(HttpServletRequest request) {
        User user = user(request);
        if (user == null) {
            throw new AccessDeniedException();
        }
        return user;
    }

    protected User requireStaff(HttpServletRequest request) {
        User user = requireUser(request);
        if (!user.isStaff()) {
            throw new AccessDeniedException();
        }
        return user;
    }

    protected User requireAdmin(HttpServletRequest request) {
        User user = requireUser(request);
        if (!user.isAdmin()) {
            throw new AccessDeniedException();
        }
        return user;
    }

    /** Parte do caminho depois do mapeamento do servlet (ex.: "/view"); vazio se não houver. */
    protected String path(HttpServletRequest request) {
        String info = request.getPathInfo();
        return info == null ? "" : info;
    }

    /** Lê um parâmetro numérico obrigatório; se faltar ou for inválido, a página não existe (404). */
    protected long idParam(HttpServletRequest request, String name) {
        try {
            return Long.parseLong(request.getParameter(name).strip());
        } catch (NullPointerException | NumberFormatException e) {
            throw new NotFoundException();
        }
    }

    /** Lê um parâmetro numérico opcional; devolve {@code fallback} se faltar ou for inválido. */
    protected int intParam(HttpServletRequest request, String name, int fallback) {
        try {
            return Integer.parseInt(request.getParameter(name).strip());
        } catch (NullPointerException | NumberFormatException e) {
            return fallback;
        }
    }

    /** Lê o filtro "id" das listagens: vazio = sem filtro; texto inválido mostra um aviso e também não filtra. */
    protected Long filterId(HttpServletRequest request) {
        String text = param(request, "id").strip();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            request.setAttribute("error", "O id deve ser um número inteiro.");
            return null;
        }
    }

    protected String param(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value;
    }

    private void showError(HttpServletRequest request, HttpServletResponse response, int status, String message)
            throws ServletException, IOException {
        response.setStatus(status);
        request.setAttribute("message", message);
        render(request, response, "error");
    }

    /** Página de onde veio a requisição (cabeçalho Referer), aceita somente se for do mesmo servidor. */
    private String previousPage(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        try {
            URI uri = URI.create(referer);
            if (request.getServerName().equals(uri.getHost()) && uri.getRawPath() != null
                    && uri.getRawPath().startsWith(request.getContextPath() + "/")) {
                String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
                return uri.getRawPath().substring(request.getContextPath().length()) + query;
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            // Sem Referer válido: usa a página inicial.
        }
        return "/problems";
    }

    private static void moveToRequest(HttpSession session, HttpServletRequest request, String name) {
        Object value = session.getAttribute(name);
        if (value != null) {
            request.setAttribute(name, value);
            session.removeAttribute(name);
        }
    }
}
