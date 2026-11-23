package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.service.NotFoundException;
import br.edu.ifsp.i3exiga.service.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/** Cadastro, login, logout e perfil do próprio usuário. */
@WebServlet({"/login", "/register", "/logout", "/profile", "/profile/password"})
public class AuthServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        switch (request.getServletPath()) {
            case "/login" -> {
                if (user(request) != null) {
                    redirect(request, response, "/problems");
                    return;
                }
                render(request, response, "login");
            }
            case "/register" -> render(request, response, "register");
            case "/profile" -> render(request, response, "profile");
            default -> throw new NotFoundException();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        switch (request.getServletPath()) {
            case "/login" -> login(request, response);
            case "/register" -> register(request, response);
            case "/logout" -> {
                request.getSession().invalidate();
                redirect(request, response, "/login");
            }
            case "/profile" -> {
                services.users.updateProfile(requireUser(request), param(request, "fullName"), param(request, "course"));
                flash(request, "Perfil atualizado.");
                redirect(request, response, "/profile");
            }
            case "/profile/password" -> {
                services.users.changePassword(requireUser(request), param(request, "current"),
                        param(request, "password"), param(request, "confirmation"));
                flash(request, "Senha alterada.");
                redirect(request, response, "/profile");
            }
            default -> throw new NotFoundException();
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String prontuario = param(request, "prontuario");
        try {
            User user = services.users.authenticate(prontuario, param(request, "password"));
            request.changeSessionId(); // evita fixação de sessão
            HttpSession session = request.getSession();
            session.setAttribute(SecurityFilter.USER_ID, user.getId());
            session.setAttribute(SecurityFilter.CSRF, SecurityFilter.newToken());
            redirect(request, response, "/problems");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("prontuario", prontuario);
            render(request, response, "login");
        }
    }

    private void register(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String prontuario = param(request, "prontuario");
        try {
            services.users.register(prontuario, param(request, "password"), param(request, "confirmation"));
            flash(request, "Cadastro realizado. Entre com o seu prontuário e a sua senha.");
            redirect(request, response, "/login");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("prontuario", prontuario);
            render(request, response, "register");
        }
    }
}
