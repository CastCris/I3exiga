package br.edu.ifsp.i3exiga.store;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Persistência simples em arquivo, sem dependências externas.
 *
 * <p>Leituras são livres (os mapas de {@link Data} são concorrentes). Escritas passam por
 * {@link #transaction(Runnable)}, que serializa as alterações e grava o arquivo de forma atômica
 * (arquivo temporário + rename). Os serviços validam tudo antes de alterar os dados.
 */
public final class Store {

    private final Path file;
    private final Data data;

    public Store(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.file = directory.resolve("i3exiga.db");
        this.data = Files.exists(file) ? load() : new Data();
    }

    public Data data() {
        return data;
    }

    /** Executa uma alteração nos dados e grava o resultado em disco. */
    public synchronized void transaction(Runnable change) {
        change.run();
        save();
    }

    /** Igual a {@link #transaction(Runnable)}, mas devolve um valor (por exemplo, a entidade criada). */
    public synchronized <T> T transactionWithResult(Supplier<T> change) {
        T result = change.get();
        save();
        return result;
    }

    /** Gera o próximo id da sequência. Deve ser chamado dentro de uma transação. */
    public long nextId(Sequence sequence) {
        long next = Math.max(data.sequences.getOrDefault(sequence, 0L), highestId(sequence)) + 1;
        data.sequences.put(sequence, next);
        return next;
    }

    /** Maior id existente: mantém a numeração correta em bancos criados antes das sequências por tipo. */
    private long highestId(Sequence sequence) {
        Map<Long, ?> entities = switch (sequence) {
            case USER -> data.users;
            case PROBLEM -> data.problems;
            case SUBMISSION -> data.submissions;
            case COMMENT -> data.comments;
            case REPORT -> data.reports;
            case ARTICLE -> data.articles;
            case SCOREBOARD -> data.scoreboards;
        };
        return entities.keySet().stream().mapToLong(Long::longValue).max().orElse(0);
    }

    private Data load() {
        try (var in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            return (Data) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // Não sobrescrevemos o arquivo: é melhor falhar na inicialização do que perder dados.
            throw new IllegalStateException("Não foi possível ler " + file, e);
        }
    }

    private void save() {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (var out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))) {
            out.writeObject(data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
