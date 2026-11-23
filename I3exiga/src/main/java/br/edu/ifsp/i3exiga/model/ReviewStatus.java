package br.edu.ifsp.i3exiga.model;

/** Situação de um artigo na avaliação feita pela equipe de administradores. */
public enum ReviewStatus {
    PENDING("Em avaliação", "pending"),
    APPROVED("Aprovado", "ok"),
    REJECTED("Rejeitado", "bad");

    private final String label;
    private final String cssClass;

    ReviewStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }
}
