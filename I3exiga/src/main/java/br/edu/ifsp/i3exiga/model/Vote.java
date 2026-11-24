package br.edu.ifsp.i3exiga.model;

import java.io.Serializable;
import java.time.Instant;

/** Voto de um administrador na aprovação de um problema ou artigo. */
public class Vote implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private boolean approve;
    private String comment;
    private Instant votedAt;

    public Vote() {
    }

    public Vote(Long userId, boolean approve, String comment, Instant votedAt) {
        this.userId = userId;
        this.approve = approve;
        this.comment = comment;
        this.votedAt = votedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isApprove() {
        return approve;
    }

    public void setApprove(boolean approve) {
        this.approve = approve;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getVotedAt() {
        return votedAt;
    }

    public void setVotedAt(Instant votedAt) {
        this.votedAt = votedAt;
    }
}
