package br.edu.ifsp.i3exiga.model;

/** Categorias de usuário da plataforma. */
public enum Role {
    ADMIN("Administrador"),
    TEACHER("Docente"),
    STUDENT("Discente");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
