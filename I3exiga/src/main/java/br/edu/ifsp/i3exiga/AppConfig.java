package br.edu.ifsp.i3exiga;

import java.nio.file.Path;

/** Configuração da aplicação, lida de variáveis de ambiente (veja o README). */
public final class AppConfig {

    public final Path dataDir = Path.of(env("I3EXIGA_DATA_DIR", "/data"));
    public final Path judgeDir = Path.of(env("I3EXIGA_JUDGE_DIR", "/tmp/i3exiga-judge"));

    /** Usuário do sistema operacional que executa o código submetido. "none" = mesmo usuário do Tomcat (apenas desenvolvimento). */
    public final String sandboxUser = sandboxUser(env("I3EXIGA_SANDBOX_USER", "judge"));
    public final String sandboxScript = env("I3EXIGA_SANDBOX_SCRIPT", "/opt/i3exiga/sandbox.sh");

    public final String adminProntuario = env("I3EXIGA_ADMIN_USER", "admin");
    /** Insere problemas e artigos de demonstração na primeira execução (desative com I3EXIGA_SEED_DEMO=false). */
    public final boolean seedDemo = !env("I3EXIGA_SEED_DEMO", "true").equalsIgnoreCase("false");
    /** Se vazia, uma senha aleatória é gerada na primeira execução e escrita no log. */
    public final String adminPassword = env("I3EXIGA_ADMIN_PASSWORD", "");

    private static String sandboxUser(String value) {
        return value.equals("none") ? "" : value;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
