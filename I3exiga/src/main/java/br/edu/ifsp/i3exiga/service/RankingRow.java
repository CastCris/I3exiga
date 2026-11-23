package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.User;

/** Linha de um placar: posição, usuário e pontuação. */
public class RankingRow {

    private final int rank;
    private final User user;
    private final int score;

    public RankingRow(int rank, User user, int score) {
        this.rank = rank;
        this.user = user;
        this.score = score;
    }

    public int getRank() {
        return rank;
    }

    public User getUser() {
        return user;
    }

    public int getScore() {
        return score;
    }
}
