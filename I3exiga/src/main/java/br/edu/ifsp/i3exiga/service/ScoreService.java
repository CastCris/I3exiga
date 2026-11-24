package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.model.ReviewStatus;
import br.edu.ifsp.i3exiga.model.Submission;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.store.Data;
import br.edu.ifsp.i3exiga.store.Store;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pontuação e placares.
 *
 * <p>Regra: cada problema resolvido vale os pontos do problema, uma única vez por usuário. Se o usuário
 * abriu os artigos de solução antes de resolver o problema, o problema não pontua para ele.
 */
public final class ScoreService {

    private final Store store;

    public ScoreService(Store store) {
        this.store = store;
    }

    /** Problemas com ao menos uma submissão aceita pelo usuário. */
    public Set<Long> solvedBy(long userId) {
        return store.data().submissions.values().stream()
                .filter(s -> s.getUserId() == userId && s.getVerdict().isAccepted())
                .map(Submission::getProblemId).collect(Collectors.toSet());
    }

    public int scoreOf(long userId) {
        return scoresByUser().getOrDefault(userId, 0);
    }

    /**
     * Monta o placar. A posição é calculada entre os participantes (empates dividem a posição) antes de
     * aplicar a busca e a ordenação escolhidas.
     *
     * @param memberIds participantes do placar, ou {@code null} para o placar geral
     * @param query     trecho do prontuário ou do nome a pesquisar (pode ser vazio)
     * @param ascending {@code true} para ordenar da menor para a maior pontuação
     */
    public List<RankingRow> ranking(Set<Long> memberIds, String query, boolean ascending) {
        Map<Long, Integer> scores = scoresByUser();
        List<User> users = store.data().users.values().stream()
                .filter(u -> !u.isBanned())
                .filter(u -> memberIds == null || memberIds.contains(u.getId()))
                .sorted(Comparator.comparingInt((User u) -> scores.getOrDefault(u.getId(), 0)).reversed()
                        .thenComparing(User::getProntuario))
                .toList();

        List<RankingRow> rows = new ArrayList<>();
        int rank = 0;
        int position = 0;
        int previousScore = -1;
        for (User user : users) {
            position++;
            int score = scores.getOrDefault(user.getId(), 0);
            if (score != previousScore) {
                rank = position;
                previousScore = score;
            }
            rows.add(new RankingRow(rank, user, score));
        }

        String text = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (!text.isEmpty()) {
            rows.removeIf(row -> !matches(row.getUser(), text));
        }
        if (ascending) {
            Collections.reverse(rows);
        }
        return rows;
    }

    private static boolean matches(User user, String text) {
        return user.getProntuario().toLowerCase(Locale.ROOT).contains(text)
                || user.getDisplayName().toLowerCase(Locale.ROOT).contains(text);
    }

    private Map<Long, Integer> scoresByUser() {
        Data data = store.data();
        Map<Long, Set<Long>> solved = new HashMap<>();
        for (Submission submission : data.submissions.values()) {
            if (submission.getVerdict().isAccepted()) {
                solved.computeIfAbsent(submission.getUserId(), id -> new HashSet<>()).add(submission.getProblemId());
            }
        }
        Map<Long, Integer> scores = new HashMap<>();
        solved.forEach((userId, problemIds) -> {
            int total = 0;
            for (Long problemId : problemIds) {
                Problem problem = data.problems.get(problemId);
                if (problem != null && problem.getStatus() == ReviewStatus.APPROVED && !data.unlocks.contains(Data.unlockKey(userId, problemId))) {
                    total += problem.getPoints();
                }
            }
            scores.put(userId, total);
        });
        return scores;
    }
}
