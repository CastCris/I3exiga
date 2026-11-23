package br.edu.ifsp.i3exiga.model;

import java.io.Serializable;
import java.time.Instant;

/** Usuário da plataforma, identificado pelo prontuário. */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String prontuario;
    private String passwordHash;
    private Role role;
    private String fullName;
    private String course;
    private Instant bannedUntil;
    private boolean permanentlyBanned;
    private Instant createdAt;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProntuario() {
        return prontuario;
    }

    public void setProntuario(String prontuario) {
        this.prontuario = prontuario;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public Instant getBannedUntil() {
        return bannedUntil;
    }

    public void setBannedUntil(Instant bannedUntil) {
        this.bannedUntil = bannedUntil;
    }

    public boolean isPermanentlyBanned() {
        return permanentlyBanned;
    }

    public void setPermanentlyBanned(boolean permanentlyBanned) {
        this.permanentlyBanned = permanentlyBanned;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /** Indica se a conta está banida neste momento (temporária ou indeterminadamente). */
    public boolean isBanned() {
        return permanentlyBanned || (bannedUntil != null && bannedUntil.isAfter(Instant.now()));
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** Administradores e docentes podem gerenciar problemas, artigos e placares. */
    public boolean isStaff() {
        return role == Role.ADMIN || role == Role.TEACHER;
    }

    /** Nome completo quando informado; caso contrário, o prontuário. */
    public String getDisplayName() {
        return fullName == null || fullName.isBlank() ? prontuario : fullName;
    }
}
