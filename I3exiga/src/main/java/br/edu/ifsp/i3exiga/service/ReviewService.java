package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.ReviewStatus;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.model.Votable;
import br.edu.ifsp.i3exiga.model.Vote;
import br.edu.ifsp.i3exiga.store.Store;
import br.edu.ifsp.i3exiga.util.Text;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Votação unânime dos administradores para publicar problemas e artigos.
 *
 * <p>Regra: o item só é aprovado quando <b>todos</b> os administradores ativos votam a favor; um voto contra o
 * deixa rejeitado. Os votos (e quem votou) ficam registrados. O voto pode ser alterado enquanto o item não
 * estiver aprovado, e um novo envio dos autores reinicia a votação.
 */
public final class ReviewService {

    private static final int MAX_COMMENT = 1000;

    private final Store store;

    public ReviewService(Store store) {
        this.store = store;
    }

    /** Administradores ativos (não banidos): são eles que precisam aprovar. */
    public Set<Long> voterIds() {
        return activeAdmins().map(User::getId).collect(Collectors.toSet());
    }

    /** Uma linha por administrador ativo, com o voto dele ou nulo se ainda não votou. */
    public List<Ballot> ballots(Votable item) {
        Map<Long, Vote> votes = item.getVotes().stream()
                .collect(Collectors.toMap(Vote::getUserId, Function.identity(), (first, second) -> second));
        return activeAdmins().sorted(Comparator.comparing(User::getProntuario))
                .map(admin -> new Ballot(admin, votes.get(admin.getId()))).toList();
    }

    /** Registra (ou altera) o voto do administrador e recalcula a situação do item. */
    public void cast(User admin, Votable item, boolean approve, String comment) {
        if (!admin.isAdmin()) {
            throw new AccessDeniedException();
        }
        String text = Text.trim(comment);
        if (item.getStatus() == ReviewStatus.APPROVED) {
            throw new ValidationException("Este item já foi aprovado: a votação está encerrada.");
        }
        if (!approve && text.isEmpty()) {
            throw new ValidationException("Explique o motivo do voto contra.");
        }
        if (text.length() > MAX_COMMENT) {
            throw new ValidationException("O comentário do voto deve ter no máximo " + MAX_COMMENT + " caracteres.");
        }
        store.transaction(() -> {
            List<Vote> votes = new ArrayList<>(item.getVotes());
            votes.removeIf(vote -> vote.getUserId().equals(admin.getId()));
            votes.add(new Vote(admin.getId(), approve, text.isEmpty() ? null : text, Instant.now()));
            item.setVotes(votes);
            item.setStatus(evaluate(votes, voterIds()));
        });
    }

    /** Recalcula os itens ainda não aprovados; usado quando muda quem são os administradores ativos. */
    public void reevaluate() {
        Set<Long> voters = voterIds();
        store.transaction(() -> Stream.concat(store.data().problems.values().stream(), store.data().articles.values().stream())
                .map(Votable.class::cast)
                .filter(item -> item.getStatus() != ReviewStatus.APPROVED)
                .forEach(item -> item.setStatus(evaluate(item.getVotes(), voters))));
    }

    /** Unanimidade: qualquer voto contra rejeita; aprovado só se todos os administradores aprovaram. */
    static ReviewStatus evaluate(List<Vote> votes, Set<Long> voters) {
        boolean anyAgainst = votes.stream().anyMatch(vote -> !vote.isApprove() && voters.contains(vote.getUserId()));
        if (anyAgainst) {
            return ReviewStatus.REJECTED;
        }
        Set<Long> approvals = votes.stream().filter(Vote::isApprove).map(Vote::getUserId).collect(Collectors.toSet());
        return !voters.isEmpty() && approvals.containsAll(voters) ? ReviewStatus.APPROVED : ReviewStatus.PENDING;
    }

    private Stream<User> activeAdmins() {
        return store.data().users.values().stream().filter(user -> user.isAdmin() && !user.isBanned());
    }
}
