package br.edu.ifsp.i3exiga.judge;

import br.edu.ifsp.i3exiga.AppConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Executa o código submetido em um processo isolado.
 *
 * <p>Camadas de proteção: (1) o processo roda como um usuário sem privilégios ({@code judge}) que não
 * enxerga os dados da aplicação; (2) o script {@code sandbox.sh} aplica limites de CPU, memória, arquivos
 * e processos; (3) há um tempo limite de relógio, ao fim do qual a árvore de processos é encerrada;
 * (4) o ambiente é limpo, sem variáveis do servidor.
 */
public final class Sandbox {

    private static final Logger LOG = Logger.getLogger(Sandbox.class.getName());
    private static final int MAX_CAPTURED_BYTES = 2 * 1024 * 1024;
    private static final String SYSTEM_PATH = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin";

    private final AppConfig config;

    public Sandbox(AppConfig config) {
        this.config = config;
    }

    /**
     * Cria um diretório temporário gravável pelo usuário do avaliador. As permissões são definidas
     * explicitamente porque o Tomcat roda com umask 0027, que esconderia os arquivos do usuário "judge".
     */
    public Path newWorkDir() throws IOException {
        Files.createDirectories(config.judgeDir);
        Files.setPosixFilePermissions(config.judgeDir, PosixFilePermissions.fromString("rwxr-xr-x"));
        Path dir = Files.createTempDirectory(config.judgeDir, "run-");
        Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwxrwxrwx"));
        return dir;
    }

    /** Grava um arquivo de entrada do programa (código-fonte) legível pelo usuário do avaliador. */
    public void writeReadable(Path file, String content) throws IOException {
        Files.writeString(file, content);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
    }

    /**
     * Executa um comando dentro de {@code dir}, alimentando a entrada padrão com {@code stdin}.
     *
     * @param memoryKb limite de memória virtual em KB (0 = sem limite)
     */
    public SandboxResult run(Path dir, List<String> command, String stdin, long timeoutMs, long memoryKb)
            throws IOException, InterruptedException {
        Path inFile = dir.resolve(".stdin");
        Path outFile = dir.resolve(".stdout");
        Path errFile = dir.resolve(".stderr");
        writeReadable(inFile, stdin);

        ProcessBuilder builder = new ProcessBuilder(wrap(command, timeoutMs, memoryKb))
                .directory(dir.toFile())
                .redirectInput(inFile.toFile())
                .redirectOutput(outFile.toFile())
                .redirectError(errFile.toFile());
        builder.environment().clear();
        builder.environment().put("PATH", Path.of(System.getProperty("java.home"), "bin") + ":" + SYSTEM_PATH);
        builder.environment().put("HOME", dir.toString());
        builder.environment().put("TMPDIR", dir.toString());
        builder.environment().put("LANG", "C.UTF-8");

        Process process = builder.start();
        boolean finished;
        try {
            finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            kill(process);
            throw e;
        }
        if (!finished) {
            kill(process);
            process.waitFor();
        }
        return new SandboxResult(finished ? process.exitValue() : -1, !finished, read(outFile), read(errFile));
    }

    /** Encerra processos que o programa tenha deixado em segundo plano e apaga o diretório de trabalho. */
    public void cleanup(Path dir) {
        killLeftovers();
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Não foi possível apagar " + dir, e);
        }
    }

    private List<String> wrap(List<String> command, long timeoutMs, long memoryKb) {
        List<String> full = new ArrayList<>();
        if (!config.sandboxUser.isBlank()) {
            full.addAll(List.of("/usr/bin/setpriv", "--reuid=" + config.sandboxUser,
                    "--regid=" + config.sandboxUser, "--clear-groups"));
        }
        long cpuSeconds = (timeoutMs + 999) / 1000 + 1;
        full.addAll(List.of("/bin/bash", config.sandboxScript, Long.toString(cpuSeconds), Long.toString(memoryKb)));
        full.addAll(command);
        return full;
    }

    private static void kill(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private void killLeftovers() {
        if (config.sandboxUser.isBlank()) {
            return;
        }
        try {
            Process process = new ProcessBuilder("/usr/bin/setpriv", "--reuid=" + config.sandboxUser,
                    "--regid=" + config.sandboxUser, "--clear-groups", "/bin/bash", "-c", "kill -KILL -1")
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Não foi possível encerrar processos remanescentes", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String read(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return new String(in.readNBytes(MAX_CAPTURED_BYTES), StandardCharsets.UTF_8);
        }
    }
}
