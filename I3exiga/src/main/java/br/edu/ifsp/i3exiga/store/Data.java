package br.edu.ifsp.i3exiga.store;

import br.edu.ifsp.i3exiga.model.Article;
import br.edu.ifsp.i3exiga.model.Comment;
import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.model.Report;
import br.edu.ifsp.i3exiga.model.Scoreboard;
import br.edu.ifsp.i3exiga.model.Submission;
import br.edu.ifsp.i3exiga.model.User;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Todos os dados da aplicação. É gravado em disco inteiro a cada alteração (veja {@link Store}). */
public class Data implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Último id usado em cada sequência (veja {@link Store#nextId(Sequence)}). */
    public final Map<Sequence, Long> sequences = new ConcurrentHashMap<>();
    /** Indica que os dados de demonstração já foram inseridos (eles são inseridos no máximo uma vez). */
    public boolean demoSeeded;
    public final Map<Long, User> users = new ConcurrentHashMap<>();
    public final Map<Long, Problem> problems = new ConcurrentHashMap<>();
    public final Map<Long, Submission> submissions = new ConcurrentHashMap<>();
    public final Map<Long, Comment> comments = new ConcurrentHashMap<>();
    public final Map<Long, Report> reports = new ConcurrentHashMap<>();
    public final Map<Long, Article> articles = new ConcurrentHashMap<>();
    public final Map<Long, Scoreboard> scoreboards = new ConcurrentHashMap<>();

    /**
     * Pares usuário/problema em que o usuário abriu os artigos de solução sem ter resolvido o problema.
     * Nesses casos o problema não pontua para ele.
     */
    public final Set<String> unlocks = ConcurrentHashMap.newKeySet();

    public static String unlockKey(Long userId, Long problemId) {
        return userId + ":" + problemId;
    }
}
