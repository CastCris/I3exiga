package br.edu.ifsp.i3exiga.model;

import java.io.Serializable;
import java.time.Instant;

/** Denúncia de um comentário, a ser analisada pelos administradores. */
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long commentId;
    private Long reporterId;
    private String reason;
    private Instant createdAt;

    public Report() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
