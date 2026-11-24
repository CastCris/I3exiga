package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.model.Vote;

/** Situação do voto de um administrador em um item: {@code vote} é nulo enquanto ele não votou. */
public class Ballot {

    private final User admin;
    private final Vote vote;

    public Ballot(User admin, Vote vote) {
        this.admin = admin;
        this.vote = vote;
    }

    public User getAdmin() {
        return admin;
    }

    public Vote getVote() {
        return vote;
    }
}
