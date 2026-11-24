package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.ReviewStatus;
import br.edu.ifsp.i3exiga.model.User;
import java.util.List;

/** Item (problema ou artigo) exibido na página de votação dos administradores. */
public class ReviewItem {

    private final String kind;
    private final Long id;
    private final String title;
    private final String description;
    private final String path;
    private final ReviewStatus status;
    private final List<User> authors;
    private final List<Ballot> ballots;
    private final List<String> criteria;

    public ReviewItem(String kind, Long id, String title, String description, String path, ReviewStatus status,
                      List<User> authors, List<Ballot> ballots, List<String> criteria) {
        this.kind = kind;
        this.id = id;
        this.title = title;
        this.description = description;
        this.path = path;
        this.status = status;
        this.authors = authors;
        this.ballots = ballots;
        this.criteria = criteria;
    }

    /** "problem" ou "article". */
    public String getKind() {
        return kind;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    /** Caminho da página do item, relativo à aplicação. */
    public String getPath() {
        return path;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public List<User> getAuthors() {
        return authors;
    }

    public List<Ballot> getBallots() {
        return ballots;
    }

    /** Critérios que o administrador confirma ao votar a favor (vazio para problemas). */
    public List<String> getCriteria() {
        return criteria;
    }
}
