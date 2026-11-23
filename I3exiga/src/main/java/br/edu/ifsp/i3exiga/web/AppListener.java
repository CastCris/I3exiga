package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.AppConfig;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.service.DemoData;
import br.edu.ifsp.i3exiga.service.Services;
import br.edu.ifsp.i3exiga.service.UserService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

/** Inicializa os serviços, cria o administrador inicial e retoma a fila de correção. */
@WebListener
public class AppListener implements ServletContextListener {

    public static final String SERVICES = "services";

    private static final Logger LOG = Logger.getLogger(AppListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        AppConfig config = new AppConfig();
        Services services = new Services(config);
        seedAdmin(services, config);
        services.submissions.requeuePending();
        event.getServletContext().setAttribute(SERVICES, services);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Object services = event.getServletContext().getAttribute(SERVICES);
        if (services instanceof Services s) {
            s.close();
        }
    }

    private static void seedAdmin(Services services, AppConfig config) {
        String login = config.adminProntuario.strip().toUpperCase();
        if (!config.adminPassword.isBlank()) {
            // A senha definida no ambiente manda: cria o administrador ou redefine a senha dele se tiver mudado.
            UserService.AdminSetup result = services.users.ensureAdmin(login, config.adminPassword);
            LOG.info("Administrador " + login + ": " + switch (result) {
                case CREATED -> "criado com a senha de I3EXIGA_ADMIN_PASSWORD.";
                case RESET -> "senha redefinida para a de I3EXIGA_ADMIN_PASSWORD (categoria e acesso restaurados).";
                case UNCHANGED -> "senha de I3EXIGA_ADMIN_PASSWORD já em uso.";
            });
        } else {
            String password = randomPassword();
            if (services.users.seedAdmin(login, password)) {
                LOG.warning("Administrador inicial criado. Prontuário: " + login + " | Senha gerada (troque-a em Perfil): " + password);
            } else {
                LOG.info("I3EXIGA_ADMIN_PASSWORD não definida: senhas existentes mantidas. Para redefinir a senha do administrador "
                        + login + ", defina a variável e reinicie.");
            }
        }
        if (config.seedDemo) {
            services.users.findByProntuario(login).filter(User::isAdmin).ifPresent(admin -> DemoData.seed(services, admin));
        }
    }

    private static String randomPassword() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
