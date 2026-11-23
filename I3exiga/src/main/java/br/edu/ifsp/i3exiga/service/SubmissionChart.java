package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.Submission;
import br.edu.ifsp.i3exiga.util.Dates;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

/** Gráfico (SVG gerado no servidor) das submissões por dia nos últimos 30 dias. */
public final class SubmissionChart {

    private static final int DAYS = 30;
    private static final int WIDTH = 600;
    private static final int HEIGHT = 190;
    private static final int LEFT = 34;
    private static final int BASELINE = 150;
    private static final int PLOT_HEIGHT = 120;
    private static final int SLOT = 18;
    private static final int BAR = 14;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");

    private SubmissionChart() {
    }

    /** Barras empilhadas: aceitas (embaixo) e rejeitadas (em cima). Submissões em correção são ignoradas. */
    public static String render(Collection<Submission> submissions) {
        LocalDate today = LocalDate.now(Dates.ZONE);
        int[] accepted = new int[DAYS];
        int[] rejected = new int[DAYS];
        for (Submission submission : submissions) {
            if (submission.getVerdict().isPending()) {
                continue;
            }
            long age = ChronoUnit.DAYS.between(LocalDate.ofInstant(submission.getSubmittedAt(), Dates.ZONE), today);
            if (age < 0 || age >= DAYS) {
                continue;
            }
            int index = DAYS - 1 - (int) age;
            if (submission.getVerdict().isAccepted()) {
                accepted[index]++;
            } else {
                rejected[index]++;
            }
        }
        int max = 1;
        for (int i = 0; i < DAYS; i++) {
            max = Math.max(max, accepted[i] + rejected[i]);
        }

        StringBuilder svg = new StringBuilder();
        svg.append("<svg class=\"chart\" viewBox=\"0 0 ").append(WIDTH).append(' ').append(HEIGHT)
                .append("\" role=\"img\" aria-label=\"Submissões por dia nos últimos 30 dias\">");
        svg.append("<line class=\"axis\" x1=\"").append(LEFT).append("\" y1=\"").append(BASELINE)
                .append("\" x2=\"").append(WIDTH - 6).append("\" y2=\"").append(BASELINE).append("\"/>");
        for (int i = 0; i < DAYS; i++) {
            int okHeight = (int) Math.round((double) accepted[i] / max * PLOT_HEIGHT);
            int badHeight = (int) Math.round((double) rejected[i] / max * PLOT_HEIGHT);
            int x = LEFT + 4 + i * SLOT;
            svg.append("<g><title>").append(today.minusDays(DAYS - 1 - i).format(DAY)).append(": ")
                    .append(accepted[i]).append(" aceitas, ").append(rejected[i]).append(" rejeitadas</title>");
            if (badHeight > 0) {
                svg.append(bar("bar-bad", x, BASELINE - okHeight - badHeight, badHeight));
            }
            if (okHeight > 0) {
                svg.append(bar("bar-ok", x, BASELINE - okHeight, okHeight));
            }
            svg.append("</g>");
        }
        svg.append("<text class=\"label\" x=\"").append(LEFT - 6).append("\" y=\"").append(BASELINE - PLOT_HEIGHT + 4)
                .append("\" text-anchor=\"end\">").append(max).append("</text>");
        svg.append("<text class=\"label\" x=\"").append(LEFT - 6).append("\" y=\"").append(BASELINE + 4)
                .append("\" text-anchor=\"end\">0</text>");
        svg.append("<text class=\"label\" x=\"").append(LEFT + 4).append("\" y=\"").append(BASELINE + 20).append("\">")
                .append(today.minusDays(DAYS - 1).format(DAY)).append("</text>");
        svg.append("<text class=\"label\" x=\"").append(LEFT + 4 + (DAYS - 1) * SLOT + BAR).append("\" y=\"")
                .append(BASELINE + 20).append("\" text-anchor=\"end\">").append(today.format(DAY)).append("</text>");
        return svg.append("</svg>").toString();
    }

    private static String bar(String cssClass, int x, int y, int height) {
        return "<rect class=\"" + cssClass + "\" x=\"" + x + "\" y=\"" + y + "\" width=\"" + BAR
                + "\" height=\"" + height + "\"/>";
    }
}
