package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.AppConfig;
import br.edu.ifsp.i3exiga.judge.Judge;
import br.edu.ifsp.i3exiga.judge.Sandbox;
import br.edu.ifsp.i3exiga.store.Store;

/** Reúne os serviços da aplicação (ligação manual das dependências, sem framework). */
public final class Services implements AutoCloseable {

    public final Store store;
    public final UserService users;
    public final SubmissionService submissions;
    public final ArticleService articles;
    public final ProblemService problems;
    public final ScoreService scores;
    public final CommentService comments;
    public final ScoreboardService scoreboards;

    public Services(AppConfig config) {
        store = new Store(config.dataDir);
        users = new UserService(store);
        submissions = new SubmissionService(store, new Judge(new Sandbox(config)));
        articles = new ArticleService(store, submissions, config.dataDir.resolve("articles"));
        problems = new ProblemService(store, articles);
        scores = new ScoreService(store);
        comments = new CommentService(store, users);
        scoreboards = new ScoreboardService(store);
    }

    @Override
    public void close() {
        submissions.close();
    }
}
