package br.edu.ifsp.i3exiga.judge;

import br.edu.ifsp.i3exiga.model.Language;
import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.model.Submission;
import br.edu.ifsp.i3exiga.model.TestCase;
import br.edu.ifsp.i3exiga.model.Verdict;
import br.edu.ifsp.i3exiga.util.Text;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/** Compila e executa a submissão contra os casos de teste do problema, parando no primeiro erro. */
public final class Judge {

    private static final int COMPILE_TIMEOUT_MS = 30_000;
    private static final int DISPLAY_LINES = 20;
    /** Código de saída de um processo encerrado por SIGXCPU (limite de CPU atingido). */
    private static final int EXIT_SIGXCPU = 128 + 24;
    private static final long VIRTUAL_MEMORY_SLACK_MB = 128;
    private static final Pattern MEMORY_ERROR = Pattern.compile(
            "OutOfMemoryError|MemoryError|bad_alloc|Cannot allocate memory|out of memory", Pattern.CASE_INSENSITIVE);
    private static final Path JAVA_BIN = Path.of(System.getProperty("java.home"), "bin");

    private final Sandbox sandbox;

    public Judge(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    public JudgeResult judge(Problem problem, Submission submission) throws IOException, InterruptedException {
        Language language = submission.getLanguage();
        Path dir = sandbox.newWorkDir();
        try {
            sandbox.writeReadable(dir.resolve(language.getFileName()), submission.getSourceCode());

            SandboxResult build = sandbox.run(dir, compileCommand(language), "", COMPILE_TIMEOUT_MS, 0);
            if (build.timedOut() || build.exitCode() != 0) {
                String message = build.timedOut() ? "Tempo limite de compilação excedido."
                        : build.stderr().isBlank() ? build.stdout() : build.stderr();
                return JudgeResult.compilationError(Text.limitLines(message, DISPLAY_LINES));
            }

            int number = 0;
            for (TestCase test : problem.getTestCases()) {
                number++;
                JudgeResult failure = runTestCase(dir, problem, language, test, number);
                if (failure != null) {
                    return failure;
                }
            }
            return JudgeResult.accepted();
        } finally {
            sandbox.cleanup(dir);
        }
    }

    /** Devolve o resultado do caso de teste se ele falhou, ou {@code null} se passou. */
    private JudgeResult runTestCase(Path dir, Problem problem, Language language, TestCase test, int number)
            throws IOException, InterruptedException {
        long timeLimitMs = problem.getTimeLimitMs() + language.getTimeBonusMs();
        SandboxResult run = sandbox.run(dir, runCommand(language, problem.getMemoryLimitMb()), test.getInput(),
                timeLimitMs, virtualMemoryLimitKb(language, problem));

        if (run.timedOut() || run.exitCode() == EXIT_SIGXCPU) {
            return failure(Verdict.TIME_LIMIT_EXCEEDED, number, test, run);
        }
        if (run.exitCode() != 0) {
            boolean outOfMemory = MEMORY_ERROR.matcher(run.stderr()).find();
            return failure(outOfMemory ? Verdict.MEMORY_LIMIT_EXCEEDED : Verdict.RUNTIME_ERROR, number, test, run);
        }
        Verdict verdict = OutputComparator.compare(test.getExpectedOutput(), run.stdout());
        return verdict == Verdict.ACCEPTED ? null : failure(verdict, number, test, run);
    }

    /** O usuário nunca vê mais de 20 linhas de saída (esperada, recebida ou mensagens de erro). */
    private static JudgeResult failure(Verdict verdict, int number, TestCase test, SandboxResult run) {
        return new JudgeResult(verdict, number,
                Text.limitLines(test.getExpectedOutput(), DISPLAY_LINES),
                Text.limitLines(run.stdout(), DISPLAY_LINES),
                Text.limitLines(run.stderr(), DISPLAY_LINES));
    }

    private static List<String> compileCommand(Language language) {
        String file = language.getFileName();
        return switch (language) {
            case C -> List.of("gcc", "-O2", "-o", "main", file, "-lm");
            case CPP -> List.of("g++", "-O2", "-std=c++17", "-o", "main", file);
            case JAVA -> List.of(JAVA_BIN.resolve("javac").toString(), "-encoding", "UTF-8", file);
            case PYTHON -> List.of("python3", "-m", "py_compile", file); // detecta erros de sintaxe
        };
    }

    private static List<String> runCommand(Language language, int memoryLimitMb) {
        return switch (language) {
            case C, CPP -> List.of("./main");
            case JAVA -> List.of(JAVA_BIN.resolve("java").toString(), "-Xmx" + memoryLimitMb + "m", "-Xss16m",
                    "-XX:+UseSerialGC", "-XX:-UsePerfData", "-Djava.io.tmpdir=.", "-cp", ".", "Main");
            case PYTHON -> List.of("python3", "-B", language.getFileName());
        };
    }

    /** A JVM reserva muito espaço virtual; por isso ela é limitada com -Xmx e não com ulimit -v. */
    private static long virtualMemoryLimitKb(Language language, Problem problem) {
        return language == Language.JAVA ? 0 : (problem.getMemoryLimitMb() + VIRTUAL_MEMORY_SLACK_MB) * 1024;
    }
}
