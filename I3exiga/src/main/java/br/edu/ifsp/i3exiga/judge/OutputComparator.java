package br.edu.ifsp.i3exiga.judge;

import br.edu.ifsp.i3exiga.model.Verdict;
import br.edu.ifsp.i3exiga.util.Text;
import java.util.ArrayList;
import java.util.List;

/** Compara a saída esperada com a recebida, como nos juízes online tradicionais. */
final class OutputComparator {

    private OutputComparator() {
    }

    /**
     * <ul>
     *   <li>ACCEPTED: iguais, ignorando espaços no fim das linhas e linhas vazias no fim da saída;</li>
     *   <li>PRESENTATION_ERROR (Erro de Formatação): iguais quando se ignoram todos os espaços e quebras de linha;</li>
     *   <li>WRONG_ANSWER (Erro de Saída): qualquer outra diferença.</li>
     * </ul>
     */
    static Verdict compare(String expected, String received) {
        if (canonical(expected).equals(canonical(received))) {
            return Verdict.ACCEPTED;
        }
        if (withoutWhitespace(expected).equals(withoutWhitespace(received))) {
            return Verdict.PRESENTATION_ERROR;
        }
        return Verdict.WRONG_ANSWER;
    }

    private static String canonical(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : Text.normalizeNewlines(text).split("\n", -1)) {
            lines.add(line.stripTrailing());
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return String.join("\n", lines);
    }

    private static String withoutWhitespace(String text) {
        return text.replaceAll("\\s+", "");
    }
}
