package br.edu.ifsp.i3exiga.model;

import java.util.List;

/** Item que só é publicado após a votação unânime dos administradores (problema ou artigo). */
public interface Votable {

    ReviewStatus getStatus();

    void setStatus(ReviewStatus status);

    List<Vote> getVotes();

    void setVotes(List<Vote> votes);
}
