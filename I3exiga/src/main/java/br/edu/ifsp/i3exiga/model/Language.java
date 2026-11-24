package br.edu.ifsp.i3exiga.model;

/** Linguagens aceitas nas submissões (iguais para todos os problemas). */
public enum Language {
    C("C", "main.c", 0, """
            #include <stdio.h>

            int main() {
                // Sua solução aqui
                return 0;
            }
            """),
    CPP("C++", "main.cpp", 0, """
            #include <iostream>
            using namespace std;

            int main() {
                // Sua solução aqui
                return 0;
            }
            """),
    JAVA("Java", "Main.java", 1000, """
            import java.util.Scanner;

            public class Main {
                public static void main(String[] args) {
                    Scanner sc = new Scanner(System.in);
                    // Sua solução aqui
                }
            }
            """),
    PYTHON("Python", "main.py", 0, """
            # Sua solução aqui
            """);

    private final String label;
    private final String fileName;
    private final int timeBonusMs;
    private final String template;

    Language(String label, String fileName, int timeBonusMs, String template) {
        this.label = label;
        this.fileName = fileName;
        this.timeBonusMs = timeBonusMs;
        this.template = template;
    }

    public String getLabel() {
        return label;
    }

    /** Nome do arquivo-fonte no diretório de correção (em Java a classe precisa se chamar Main). */
    public String getFileName() {
        return fileName;
    }

    /** Código inicial exibido no editor ao escolher a linguagem. */
    public String getTemplate() {
        return template;
    }

    /** Tempo extra concedido à linguagem para compensar a inicialização da JVM. */
    public int getTimeBonusMs() {
        return timeBonusMs;
    }
}
