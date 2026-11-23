package br.edu.ifsp.i3exiga.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gerador mínimo de PDF (somente texto, fontes padrão do PDF), sem bibliotecas externas.
 * Usado para criar os artigos de demonstração.
 */
public final class SimplePdf {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 56;
    private static final int HELVETICA = 1;
    private static final int HELVETICA_BOLD = 2;
    private static final int COURIER = 3;
    private static final Charset WIN_ANSI = Charset.forName("windows-1252");

    private record Line(String text, int font, double size, double gapBefore) {
    }

    private final List<Line> lines = new ArrayList<>();

    public SimplePdf title(String text) {
        return wrap(text, HELVETICA_BOLD, 18, 60, 0);
    }

    public SimplePdf heading(String text) {
        return wrap(text, HELVETICA_BOLD, 13, 80, 12);
    }

    public SimplePdf paragraph(String text) {
        return wrap(text, HELVETICA, 11, 88, 6);
    }

    /** Bloco de código em fonte monoespaçada, uma linha do texto por linha do PDF. */
    public SimplePdf code(String block) {
        double gap = 6;
        for (String line : block.replace("\t", "    ").split("\n", -1)) {
            lines.add(new Line(line.length() > 100 ? line.substring(0, 100) : line, COURIER, 9.5, gap));
            gap = 0;
        }
        return this;
    }

    public byte[] build() {
        List<byte[]> pages = paginate();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n");

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            kids.append(6 + 2 * i).append(" 0 R ");
        }
        object(out, offsets, "<< /Type /Catalog /Pages 2 0 R >>");
        object(out, offsets, "<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>");
        for (String font : List.of("Helvetica", "Helvetica-Bold", "Courier")) {
            object(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /" + font + " /Encoding /WinAnsiEncoding >>");
        }
        for (int i = 0; i < pages.size(); i++) {
            object(out, offsets, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R >> >> /Contents " + (7 + 2 * i) + " 0 R >>");
            offsets.add(out.size());
            write(out, offsets.size() + " 0 obj\n<< /Length " + pages.get(i).length + " >>\nstream\n");
            out.writeBytes(pages.get(i));
            write(out, "\nendstream\nendobj\n");
        }

        int xref = out.size();
        write(out, "xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
        for (int offset : offsets) {
            write(out, String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        write(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private SimplePdf wrap(String text, int font, double size, int width, double gap) {
        StringBuilder current = new StringBuilder();
        double nextGap = gap;
        for (String word : text.strip().split("\\s+")) {
            if (current.length() > 0 && current.length() + 1 + word.length() > width) {
                lines.add(new Line(current.toString(), font, size, nextGap));
                nextGap = 0;
                current.setLength(0);
            }
            current.append(current.length() > 0 ? " " : "").append(word);
        }
        lines.add(new Line(current.toString(), font, size, nextGap));
        return this;
    }

    private List<byte[]> paginate() {
        List<byte[]> pages = new ArrayList<>();
        ByteArrayOutputStream page = new ByteArrayOutputStream();
        double y = PAGE_HEIGHT - MARGIN;
        for (Line line : lines) {
            double step = line.gapBefore() + line.size() * 1.4;
            if (y - step < MARGIN) {
                pages.add(page.toByteArray());
                page = new ByteArrayOutputStream();
                y = PAGE_HEIGHT - MARGIN;
                step = line.size() * 1.4;
            }
            y -= step;
            if (!line.text().isEmpty()) {
                write(page, String.format(Locale.ROOT, "BT /F%d %.1f Tf 1 0 0 1 %d %.1f Tm (", line.font(), line.size(), MARGIN, y));
                page.writeBytes(escape(line.text().getBytes(WIN_ANSI)));
                write(page, ") Tj ET\n");
            }
        }
        pages.add(page.toByteArray());
        return pages;
    }

    private static byte[] escape(byte[] text) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte b : text) {
            if (b == '\\' || b == '(' || b == ')') {
                out.write('\\');
            }
            out.write(b);
        }
        return out.toByteArray();
    }

    private static void object(ByteArrayOutputStream out, List<Integer> offsets, String body) {
        offsets.add(out.size());
        write(out, offsets.size() + " 0 obj\n" + body + "\nendobj\n");
    }

    private static void write(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.ISO_8859_1));
    }
}
