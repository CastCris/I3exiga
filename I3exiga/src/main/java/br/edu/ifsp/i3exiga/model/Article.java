package br.edu.ifsp.i3exiga.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Artigo em PDF de conhecimento geral ou de solução de um problema. */
public class Article implements Serializable, Votable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private ArticleType type;
    private Long problemId;
    private List<Long> authorIds = new ArrayList<>();
    private ReviewStatus status;
    private String feedback;
    private Long reviewerId;
    private Instant submittedAt;
    private List<String> topics = new ArrayList<>();
    private List<Vote> votes = new ArrayList<>();

    public Article() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArticleType getType() {
        return type;
    }

    public void setType(ArticleType type) {
        this.type = type;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public List<Long> getAuthorIds() {
        return authorIds;
    }

    public void setAuthorIds(List<Long> authorIds) {
        this.authorIds = authorIds;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public List<Vote> getVotes() {
        return votes;
    }

    public void setVotes(List<Vote> votes) {
        this.votes = votes;
    }

    /** Dados gravados em versões anteriores não têm alguns campos; aqui eles são completados. */
    private Object readResolve() {
        if (topics == null) {
            topics = new ArrayList<>();
        }
        if (votes == null) {
            votes = new ArrayList<>();
            if (reviewerId != null && status != ReviewStatus.PENDING) {
                votes.add(new Vote(reviewerId, status == ReviewStatus.APPROVED, feedback, submittedAt));
            }
        }
        return this;
    }
}
