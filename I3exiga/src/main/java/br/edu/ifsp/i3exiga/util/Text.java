package br.edu.ifsp.i3exiga.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Funções auxiliares para tratamento de texto. */
public final class Text {

    private static final int MAX_DISPLAY_CHARS = 4000;

    private Text() {
    }

    public static String trim(String value) {
        return value == null ? "" : value.strip();
    }

    /** Converte quebras de linha do navegador (\r\n) para \n. */
    public static String normalizeNewlines(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
    }

    /** Minúsculas e sem acentos, para buscas ("Programação" encontra "programacao"). */
    public static String fold(String value) {
        String decomposed = Normalizer.normalize(trim(value), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Separa tópicos digitados em uma linha ("grafos, programação dinâmica"): minúsculas, sem repetições nem vazios. */
    public static List<String> splitTopics(String raw) {
        List<String> topics = new ArrayList<>();
        for (String part : trim(raw).split("[,;\\n]")) {
            String topic = part.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (!topic.isEmpty() && !topics.contains(topic)) {
                topics.add(topic);
            }
        }
        return topics;
    }

    /** Limita o texto exibido ao usuário às primeiras linhas (e a um tamanho máximo em caracteres). */
    public static String limitLines(String text, int maxLines) {
        String[] lines = normalizeNewlines(text).stripTrailing().split("\n", -1);
        String joined = String.join("\n", Arrays.copyOf(lines, Math.min(lines.length, maxLines)));
        return joined.length() > MAX_DISPLAY_CHARS ? joined.substring(0, MAX_DISPLAY_CHARS) : joined;
    }
}
