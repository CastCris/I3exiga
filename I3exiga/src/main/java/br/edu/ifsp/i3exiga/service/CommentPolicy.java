package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.util.Text;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regras automáticas dos comentários: sem termos ofensivos e sem código com a solução.
 * É uma primeira barreira; o que passar por ela ainda pode ser denunciado pelos usuários.
 */
final class CommentPolicy {

    private static final int MAX_LENGTH = 2000;
    private static final int MAX_CODE_LINES = 3;

    private static final Pattern CODE = Pattern.compile("```|#include|\\bint\\s+main\\s*\\(|System\\.out|\\bprintf\\s*\\("
            + "|\\bscanf\\s*\\(|\\bcout\\s*<<|\\bpublic\\s+static\\s+void|\\bdef\\s+\\w+\\s*\\(.*\\)\\s*:"
            + "|\\bfor\\s*\\(.*;.*;.*\\)|\\bwhile\\s*\\(.*\\)\\s*\\{");
    private static final Pattern STATEMENT_LINE = Pattern.compile("(?m)^.*[;{}]\\s*$");

    private final List<Pattern> blockedTerms = loadBlockedTerms();

    /** Valida o comentário e devolve o texto normalizado. */
    String check(String text) {
        String comment = Text.normalizeNewlines(text).strip();
        if (comment.isEmpty()) {
            throw new ValidationException("Escreva o comentário antes de enviar.");
        }
        if (comment.length() > MAX_LENGTH) {
            throw new ValidationException("O comentário deve ter no máximo " + MAX_LENGTH + " caracteres.");
        }
        for (Pattern term : blockedTerms) {
            if (term.matcher(comment).find()) {
                throw new ValidationException("O comentário contém termos ofensivos ou profanos.");
            }
        }
        if (looksLikeCode(comment)) {
            throw new ValidationException("Comentários não podem conter código com a solução do problema.");
        }
        return comment;
    }

    private static boolean looksLikeCode(String comment) {
        if (CODE.matcher(comment).find()) {
            return true;
        }
        int statements = 0;
        Matcher lines = STATEMENT_LINE.matcher(comment);
        while (lines.find()) {
            statements++;
        }
        return statements >= MAX_CODE_LINES;
    }

    private static List<Pattern> loadBlockedTerms() {
        List<Pattern> patterns = new ArrayList<>();
        try (InputStream in = CommentPolicy.class.getResourceAsStream("/blocked-terms.txt")) {
            if (in == null) {
                return patterns;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String term = line.strip();
                if (!term.isEmpty() && !term.startsWith("#")) {
                    patterns.add(Pattern.compile("(?<![\\p{L}])" + Pattern.quote(term) + "(?![\\p{L}])",
                            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return patterns;
    }
}
