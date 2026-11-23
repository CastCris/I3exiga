package br.edu.ifsp.i3exiga.model;

import java.io.Serializable;
import java.time.Instant;

/** Comentário feito na aba de um problema. */
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long problemId;
    private Long authorId;
    private String text;
    private Instant createdAt;

    public Comment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
