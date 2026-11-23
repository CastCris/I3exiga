package br.edu.ifsp.i3exiga.model;

/** Resultado da correção de uma submissão. */
public enum Verdict {
    PENDING("Aguardando correção", "pending"),
    ACCEPTED("Aceito", "ok"),
    COMPILATION_ERROR("Erro de compilação", "bad"),
    RUNTIME_ERROR("Erro Durante a Execução", "bad"),
    TIME_LIMIT_EXCEEDED("Tempo Excedido", "bad"),
    MEMORY_LIMIT_EXCEEDED("Memória Excedida", "bad"),
    PRESENTATION_ERROR("Erro de Formatação", "bad"),
    WRONG_ANSWER("Erro de Saída", "bad"),
    INTERNAL_ERROR("Erro interno do avaliador", "warn");

    private final String label;
    private final String cssClass;

    Verdict(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isAccepted() {
        return this == ACCEPTED;
    }
}
