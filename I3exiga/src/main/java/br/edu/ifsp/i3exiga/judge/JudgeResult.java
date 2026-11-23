package br.edu.ifsp.i3exiga.judge;

import br.edu.ifsp.i3exiga.model.Verdict;

/**
 * Resultado da correção de uma submissão.
 *
 * @param failedTestCase número (a partir de 1) do primeiro caso de teste que falhou; 0 se nenhum
 */
public record JudgeResult(Verdict verdict, int failedTestCase, String expectedOutput, String receivedOutput,
                          String details) {

    public static JudgeResult accepted() {
        return new JudgeResult(Verdict.ACCEPTED, 0, null, null, null);
    }

    public static JudgeResult compilationError(String compilerMessage) {
        return new JudgeResult(Verdict.COMPILATION_ERROR, 0, null, null, compilerMessage);
    }

    public static JudgeResult internalError() {
        return new JudgeResult(Verdict.INTERNAL_ERROR, 0, null, null, null);
    }
}
