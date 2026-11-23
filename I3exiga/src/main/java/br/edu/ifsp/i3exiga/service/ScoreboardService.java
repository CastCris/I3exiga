package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.Scoreboard;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.store.Sequence;
import br.edu.ifsp.i3exiga.store.Store;
import br.edu.ifsp.i3exiga.util.Text;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Placares personalizados: públicos ou restritos a usuários selecionados. */
public final class ScoreboardService {

    private static final int MAX_NAME = 80;
    private static final int MAX_DESCRIPTION = 500;

    private final Store store;

    public ScoreboardService(Store store) {
        this.store = store;
    }

    public List<Scoreboard> visibleTo(User user) {
        return store.data().scoreboards.values().stream()
                .filter(board -> canView(user, board))
                .sorted(Comparator.comparing(Scoreboard::getName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public Scoreboard get(long id) {
        Scoreboard board = store.data().scoreboards.get(id);
        if (board == null) {
            throw new NotFoundException();
        }
        return board;
    }

    public boolean canView(User user, Scoreboard board) {
        return board.isPublicAccess() || canManage(user, board) || board.getMemberIds().contains(user.getId());
    }

    public boolean canManage(User user, Scoreboard board) {
        return user.isAdmin() || board.getCreatorId().equals(user.getId());
    }

    /** Cria ou atualiza um placar (somente administradores e docentes; edição apenas pelo criador ou administrador). */
    public Scoreboard save(Scoreboard draft, List<User> members, User actor) {
        String name = Text.trim(draft.getName());
        String description = Text.trim(draft.getDescription());
        if (name.length() < 3 || name.length() > MAX_NAME) {
            throw new ValidationException("O nome deve ter de 3 a " + MAX_NAME + " caracteres.");
        }
        if (description.length() > MAX_DESCRIPTION) {
            throw new ValidationException("A descrição deve ter no máximo " + MAX_DESCRIPTION + " caracteres.");
        }
        Set<Long> memberIds = members.stream().map(User::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        return store.transactionWithResult(() -> {
            Scoreboard target;
            if (draft.getId() == null) {
                target = new Scoreboard();
                target.setId(store.nextId(Sequence.SCOREBOARD));
                target.setCreatorId(actor.getId());
            } else {
                target = get(draft.getId());
                if (!canManage(actor, target)) {
                    throw new AccessDeniedException();
                }
            }
            target.setName(name);
            target.setDescription(description);
            target.setPublicAccess(draft.isPublicAccess());
            target.setMemberIds(memberIds);
            store.data().scoreboards.put(target.getId(), target);
            return target;
        });
    }

    /** Qualquer usuário pode participar de um placar público. */
    public void join(User user, long id) {
        Scoreboard board = get(id);
        if (!board.isPublicAccess()) {
            throw new AccessDeniedException();
        }
        store.transaction(() -> {
            Set<Long> members = new LinkedHashSet<>(board.getMemberIds());
            members.add(user.getId());
            board.setMemberIds(members);
        });
    }

    public void leave(User user, long id) {
        Scoreboard board = get(id);
        store.transaction(() -> {
            Set<Long> members = new LinkedHashSet<>(board.getMemberIds());
            members.remove(user.getId());
            board.setMemberIds(members);
        });
    }

    public void delete(User actor, long id) {
        Scoreboard board = get(id);
        if (!canManage(actor, board)) {
            throw new AccessDeniedException();
        }
        store.transaction(() -> store.data().scoreboards.remove(id));
    }
}
