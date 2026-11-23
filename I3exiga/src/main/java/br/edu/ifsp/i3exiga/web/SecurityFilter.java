package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.service.Services;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Filtro único de segurança: cabeçalhos HTTP, usuário logado, páginas públicas e proteção CSRF.
 * Disponibiliza à JSP os atributos {@code currentUser} e {@code ctx} (caminho da aplicação).
 */
@WebFilter("/*")
public class SecurityFilter implements Filter {

    public static final String USER_ID = "userId";
    public static final String CSRF = "csrf";

    private static final SecureRandom RANDOM = new SecureRandom();
    /** Páginas acessíveis sem login (os artigos de conhecimento geral são públicos; o servlet decide o resto). */
    private static final Set<String> PUBLIC_PATHS = Set.of("/login", "/register", "/articles", "/articles/view", "/articles/file");

    private Services services;

    @Override
    public void init(FilterConfig config) {
        services = (Services) config.getServletContext().getAttribute(AppListener.SERVICES);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = request.getRequestURI().substring(request.getContextPath().length());

        addSecurityHeaders(response, path);
        request.setAttribute("ctx", request.getContextPath());
        if (path.startsWith("/assets/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession();
        if (session.getAttribute(CSRF) == null) {
            session.setAttribute(CSRF, newToken());
        }
        User user = services.users.find((Long) session.getAttribute(USER_ID)).filter(u -> !u.isBanned()).orElse(null);
        if (user == null) {
            session.removeAttribute(USER_ID);
        }
        request.setAttribute("currentUser", user);

        if ("POST".equals(request.getMethod()) && !validCsrf(request, session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token CSRF inválido.");
            return;
        }
        if (user == null && !PUBLIC_PATHS.contains(path)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        chain.doFilter(request, response);
    }

    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean validCsrf(HttpServletRequest request, HttpSession session) {
        String sent = request.getParameter("_csrf");
        String expected = (String) session.getAttribute(CSRF);
        return sent != null && expected != null && MessageDigest.isEqual(
                sent.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }

    private static void addSecurityHeaders(HttpServletResponse response, String path) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");
        // O visualizador de PDF do navegador não funciona bem com CSP restritiva; o resto das páginas usa.
        if (!path.equals("/articles/file")) {
            response.setHeader("Content-Security-Policy", "default-src 'self'; img-src 'self' data:; "
                    + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'");
        }
    }
}
