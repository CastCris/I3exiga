package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.Role;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.store.Sequence;
import br.edu.ifsp.i3exiga.store.Store;
import br.edu.ifsp.i3exiga.util.Dates;
import br.edu.ifsp.i3exiga.util.Text;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Cadastro, autenticação e administração de usuários. */
public final class UserService {

    private static final Pattern PRONTUARIO = Pattern.compile("[A-Z0-9]{3,20}");
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 128;
    private static final int MAX_NAME = 120;
    private static final int MAX_FAILED_LOGINS = 5;
    private static final int MAX_TRACKED_LOGINS = 10_000;
    private static final Duration LOGIN_LOCK = Duration.ofMinutes(5);
    /** Usado para gastar o mesmo tempo quando o prontuário não existe (evita descobrir quais existem). */
    private static final String DUMMY_HASH = Passwords.hash("senha-ficticia");

    private static final class LoginAttempts {
        private int failures;
        private Instant lockedUntil;
    }

    private final Store store;
    private final Map<String, LoginAttempts> attempts = new ConcurrentHashMap<>();

    public UserService(Store store) {
        this.store = store;
    }

    /** Cadastro: somente prontuário e senha, como descrito nos requisitos. Todo novo usuário é discente. */
    public User register(String prontuario, String password, String confirmation) {
        String id = normalize(prontuario);
        if (!PRONTUARIO.matcher(id).matches()) {
            throw new ValidationException("Prontuário inválido: use de 3 a 20 letras e números.");
        }
        validateNewPassword(password, confirmation);
        String hash = Passwords.hash(password);
        return store.transactionWithResult(() -> {
            if (findByProntuario(id).isPresent()) {
                throw new ValidationException("Este prontuário já está cadastrado.");
            }
            return createUser(id, hash, Role.STUDENT);
        });
    }

    /** Cria o administrador inicial quando ainda não há nenhum usuário. */
    public boolean seedAdmin(String prontuario, String password) {
        String id = normalize(prontuario);
        String hash = Passwords.hash(password);
        return store.transactionWithResult(() -> {
            if (!store.data().users.isEmpty()) {
                return false;
            }
            createUser(id, hash, Role.ADMIN);
            return true;
        });
    }

    /** O que {@link #ensureAdmin} fez com a conta do administrador. */
    public enum AdminSetup { CREATED, RESET, UNCHANGED }

    /**
     * Garante que o administrador exista com a senha informada (usada quando I3EXIGA_ADMIN_PASSWORD está definida).
     * Também recupera o acesso: restaura a categoria de administrador e remove banimentos.
     */
    public AdminSetup ensureAdmin(String prontuario, String password) {
        String id = normalize(prontuario);
        Optional<User> existing = findByProntuario(id);
        if (existing.isEmpty()) {
            seedAdmin(id, password);
            return AdminSetup.CREATED;
        }
        User admin = existing.get();
        if (admin.isAdmin() && !admin.isBanned() && Passwords.verify(password, admin.getPasswordHash())) {
            return AdminSetup.UNCHANGED;
        }
        String hash = Passwords.hash(password);
        store.transaction(() -> {
            admin.setPasswordHash(hash);
            admin.setRole(Role.ADMIN);
            admin.setPermanentlyBanned(false);
            admin.setBannedUntil(null);
        });
        attempts.remove(id);
        return AdminSetup.RESET;
    }

    public void changePassword(User user, String current, String newPassword, String confirmation) {
        if (current == null || !Passwords.verify(current, user.getPasswordHash())) {
            throw new ValidationException("A senha atual está incorreta.");
        }
        validateNewPassword(newPassword, confirmation);
        String hash = Passwords.hash(newPassword);
        store.transaction(() -> user.setPasswordHash(hash));
    }

    public User authenticate(String prontuario, String password) {
        String id = normalize(prontuario);
        checkNotLocked(id);
        Optional<User> found = findByProntuario(id);
        String stored = found.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean passwordMatches = Passwords.verify(password == null ? "" : password, stored);
        if (found.isEmpty() || !passwordMatches) {
            registerFailure(id);
            throw new ValidationException("Prontuário ou senha inválidos.");
        }
        User user = found.get();
        if (user.isBanned()) {
            throw new ValidationException(user.isPermanentlyBanned()
                    ? "Sua conta foi banida da plataforma."
                    : "Sua conta está banida até " + Dates.format(user.getBannedUntil()) + ".");
        }
        attempts.remove(id);
        return user;
    }

    public Optional<User> find(Long id) {
        return id == null ? Optional.empty() : Optional.ofNullable(store.data().users.get(id));
    }

    public User get(long id) {
        return find(id).orElseThrow(NotFoundException::new);
    }

    public Optional<User> findByProntuario(String prontuario) {
        String id = normalize(prontuario);
        return store.data().users.values().stream().filter(u -> u.getProntuario().equals(id)).findFirst();
    }

    public List<User> all() {
        return store.data().users.values().stream().sorted(Comparator.comparing(User::getProntuario)).toList();
    }

    /** Todos os usuários indexados pelo id (usado pelas páginas para exibir nomes de autores). */
    public Map<Long, User> mapById() {
        return new LinkedHashMap<>(store.data().users);
    }

    /** Converte uma lista de prontuários (separados por espaço, vírgula ou quebra de linha) em usuários. */
    public List<User> findAllByProntuarios(String raw) {
        Set<String> ids = new LinkedHashSet<>();
        for (String part : Text.trim(raw).split("[\\s,;]+")) {
            if (!part.isBlank()) {
                ids.add(normalize(part));
            }
        }
        List<String> unknown = ids.stream().filter(id -> findByProntuario(id).isEmpty()).toList();
        if (!unknown.isEmpty()) {
            throw new ValidationException("Prontuários não encontrados: " + String.join(", ", unknown) + ".");
        }
        return ids.stream().map(id -> findByProntuario(id).orElseThrow()).toList();
    }

    public void updateProfile(User user, String fullName, String course) {
        String name = Text.trim(fullName);
        String courseName = Text.trim(course);
        if (name.length() > MAX_NAME || courseName.length() > MAX_NAME) {
            throw new ValidationException("Nome e curso devem ter no máximo " + MAX_NAME + " caracteres.");
        }
        store.transaction(() -> {
            user.setFullName(name);
            user.setCourse(courseName);
        });
    }

    public void changeRole(User actor, long userId, Role role) {
        if (actor.getId() == userId) {
            throw new ValidationException("Você não pode alterar a sua própria categoria.");
        }
        User target = get(userId);
        store.transaction(() -> target.setRole(role));
    }

    /** Bane por {@code days} dias, ou indeterminadamente quando {@code days} é nulo. */
    public void ban(User actor, long userId, Integer days) {
        if (actor.getId() == userId) {
            throw new ValidationException("Você não pode banir a si mesmo.");
        }
        if (days != null && (days < 1 || days > 365)) {
            throw new ValidationException("O banimento temporário deve durar de 1 a 365 dias.");
        }
        User target = get(userId);
        store.transaction(() -> {
            target.setPermanentlyBanned(days == null);
            target.setBannedUntil(days == null ? null : Instant.now().plus(Duration.ofDays(days)));
        });
    }

    public void unban(long userId) {
        User target = get(userId);
        store.transaction(() -> {
            target.setPermanentlyBanned(false);
            target.setBannedUntil(null);
        });
    }

    private User createUser(String prontuario, String passwordHash, Role role) {
        User user = new User();
        user.setId(store.nextId(Sequence.USER));
        user.setProntuario(prontuario);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setCreatedAt(Instant.now());
        store.data().users.put(user.getId(), user);
        return user;
    }

    private static void validateNewPassword(String password, String confirmation) {
        if (password == null || password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) {
            throw new ValidationException("A senha deve ter entre " + MIN_PASSWORD + " e " + MAX_PASSWORD + " caracteres.");
        }
        if (!password.equals(confirmation)) {
            throw new ValidationException("A confirmação da senha não confere.");
        }
    }

    private static String normalize(String prontuario) {
        return prontuario == null ? "" : prontuario.strip().toUpperCase(Locale.ROOT);
    }

    private void checkNotLocked(String id) {
        LoginAttempts entry = attempts.get(id);
        if (entry == null) {
            return;
        }
        synchronized (entry) {
            if (entry.lockedUntil != null && entry.lockedUntil.isAfter(Instant.now())) {
                throw new ValidationException("Muitas tentativas de login. Aguarde alguns minutos e tente novamente.");
            }
        }
    }

    private void registerFailure(String id) {
        if (attempts.size() > MAX_TRACKED_LOGINS) {
            attempts.clear();
        }
        LoginAttempts entry = attempts.computeIfAbsent(id, key -> new LoginAttempts());
        synchronized (entry) {
            if (++entry.failures >= MAX_FAILED_LOGINS) {
                entry.failures = 0;
                entry.lockedUntil = Instant.now().plus(LOGIN_LOCK);
            }
        }
    }
}
