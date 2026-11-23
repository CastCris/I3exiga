package br.edu.ifsp.i3exiga.model;

/** Linguagens aceitas nas submissões (iguais para todos os problemas). */
public enum Language {
    C("C", "main.c", 0),
    CPP("C++", "main.cpp", 0),
    JAVA("Java", "Main.java", 1000),
    PYTHON("Python", "main.py", 0);

    private final String label;
    private final String fileName;
    private final int timeBonusMs;

    Language(String label, String fileName, int timeBonusMs) {
        this.label = label;
        this.fileName = fileName;
        this.timeBonusMs = timeBonusMs;
    }

    public String getLabel() {
        return label;
    }

    /** Nome do arquivo-fonte no diretório de correção (em Java a classe precisa se chamar Main). */
    public String getFileName() {
        return fileName;
    }

    /** Tempo extra concedido à linguagem para compensar a inicialização da JVM. */
    public int getTimeBonusMs() {
        return timeBonusMs;
    }
}
