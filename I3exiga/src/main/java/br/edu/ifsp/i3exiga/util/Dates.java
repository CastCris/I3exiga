package br.edu.ifsp.i3exiga.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Formatação de datas no fuso horário configurado (padrão: America/Sao_Paulo). */
public final class Dates {

    public static final ZoneId ZONE = ZoneId.of(System.getenv().getOrDefault("I3EXIGA_TIMEZONE", "America/Sao_Paulo"));

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Dates() {
    }

    public static String format(Instant instant) {
        return instant == null ? "" : FORMAT.format(instant.atZone(ZONE));
    }
}
