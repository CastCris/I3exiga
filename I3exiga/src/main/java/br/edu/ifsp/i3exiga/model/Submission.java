package br.edu.ifsp.i3exiga.model;

import java.io.Serializable;
import java.time.Instant;

/** Submissão de código para um problema e o resultado da sua correção. */
public class Submission implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long problemId;
    private Language language;
    private String sourceCode;
    private Verdict verdict;
    private Instant submittedAt;
    private int failedTestCase;
    private String expectedOutput;
    private String receivedOutput;
    private String details;

    public Submission() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public int getFailedTestCase() {
        return failedTestCase;
    }

    public void setFailedTestCase(int failedTestCase) {
        this.failedTestCase = failedTestCase;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public String getReceivedOutput() {
        return receivedOutput;
    }

    public void setReceivedOutput(String receivedOutput) {
        this.receivedOutput = receivedOutput;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
