package br.edu.ifsp.i3exiga.web.view;

import br.edu.ifsp.i3exiga.util.Dates;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

/** Funções usadas nas páginas JSP via EL, por exemplo {@code ${v:h(problem.title)}}. */
public final class ViewFunctions {

    private ViewFunctions() {
    }

    /** Escapa HTML. Todo texto vindo de usuários deve passar por aqui antes de ser exibido. */
    public static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    public static String date(Instant instant) {
        return Dates.format(instant);
    }

    /** Junta os elementos com vírgula (ex.: tópicos no campo do formulário). */
    public static String join(Collection<?> items) {
        return items == null ? "" : items.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }

    /** Codifica um valor para uso em URL (ex.: link de filtro por tópico). */
    public static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    public static boolean contains(Collection<?> collection, Object item) {
        return collection != null && collection.contains(item);
    }
}
