package br.edu.ifsp.i3exiga.model;

/** Tipos de artigo: conhecimento geral (público) ou solução de um problema (restrito). */
public enum ArticleType {
    GENERAL("Conhecimento geral"),
    SOLUTION("Solução de problema");

    private final String label;

    ArticleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
