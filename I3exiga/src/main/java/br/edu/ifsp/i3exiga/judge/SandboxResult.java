package br.edu.ifsp.i3exiga.judge;

/** Resultado da execução de um programa no {@link Sandbox}. */
public record SandboxResult(int exitCode, boolean timedOut, String stdout, String stderr) {
}
