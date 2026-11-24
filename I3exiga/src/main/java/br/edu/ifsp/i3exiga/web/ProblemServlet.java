package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.model.Language;
import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.model.ReviewStatus;
import br.edu.ifsp.i3exiga.model.Submission;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.service.AccessDeniedException;
import br.edu.ifsp.i3exiga.service.NotFoundException;
import br.edu.ifsp.i3exiga.service.ProblemService;
import br.edu.ifsp.i3exiga.service.ValidationException;
import br.edu.ifsp.i3exiga.util.Text;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/** Lista, visualização, cadastro de problemas e envio de submissões. */
@WebServlet("/problems/*")
public class ProblemServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = requireUser(request);
        switch (path(request)) {
            case "", "/" -> list(request, response, user);
            case "/view" -> showProblem(request, response, user, services.problems.getVisible(user, idParam(request, "id")));
            case "/submissions" -> mySubmissions(request, response, user);
            case "/new" -> {
                requireStaff(request);
                showForm(request, response, new Problem(), user.getProntuario(), "", "");
            }
            case "/edit" -> {
                requireStaff(request);
                Problem problem = services.problems.get(idParam(request, "id"));
                if (!services.problems.canManage(user, problem)) {
                    throw new AccessDeniedException();
                }
                showForm(request, response, problem, prontuarios(problem.getAuthorIds()),
                        ProblemService.formatInputs(problem.getTestCases()),
                        ProblemService.formatOutputs(problem.getTestCases()));
            }
            default -> throw new NotFoundException();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        switch (path(request)) {
            case "/save" -> save(request, response);
            case "/submit" -> submit(request, response);
            case "/delete" -> {
                services.problems.delete(requireStaff(request), idParam(request, "id"));
                flash(request, "Problema removido.");
                redirect(request, response, "/problems");
            }
            default -> throw new NotFoundException();
        }
    }

    private void list(HttpServletRequest request, HttpServletResponse response, User user) throws ServletException, IOException {
        request.setAttribute("problems", services.problems.search(user, param(request, "q"), filterId(request), param(request, "topic")));
        request.setAttribute("total", services.problems.visibleTo(user).size());
        request.setAttribute("topics", services.problems.topics());
        request.setAttribute("q", param(request, "q"));
        request.setAttribute("topic", param(request, "topic"));
        request.setAttribute("idFilter", param(request, "id"));
        request.setAttribute("usersById", services.users.mapById());
        request.setAttribute("solved", services.scores.solvedBy(user.getId()));
        render(request, response, "problems");
    }

    private void showProblem(HttpServletRequest request, HttpServletResponse response, User user, Problem problem)
            throws ServletException, IOException {
        request.setAttribute("problem", problem);
        request.setAttribute("languages", Language.values());
        request.setAttribute("usersById", services.users.mapById());
        boolean canManage = services.problems.canManage(user, problem);
        request.setAttribute("canManage", canManage);
        if (canManage) {
            request.setAttribute("ballots", services.reviews.ballots(problem));
            request.setAttribute("canVote", user.isAdmin() && problem.getStatus() != ReviewStatus.APPROVED);
        }
        render(request, response, "problem");
    }

    private void mySubmissions(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        Problem problem = services.problems.getVisible(user, idParam(request, "id"));
        request.setAttribute("problem", problem);
        request.setAttribute("submissions", services.submissions.byUserAndProblem(user.getId(), problem.getId()));
        render(request, response, "problem-submissions");
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, Problem problem, String authors,
                          String testInputs, String testOutputs) throws ServletException, IOException {
        request.setAttribute("problem", problem);
        request.setAttribute("authors", authors);
        request.setAttribute("testInputs", testInputs);
        request.setAttribute("testOutputs", testOutputs);
        request.setAttribute("knownTopics", services.problems.topics());
        render(request, response, "problem-form");
    }

    private void save(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = requireStaff(request);
        Problem draft = readProblem(request);
        String authorsText = param(request, "authors");
        String inputs = param(request, "testInputs");
        String outputs = param(request, "testOutputs");
        try {
            draft.setTestCases(ProblemService.parseTestCases(inputs, outputs));
            List<User> authors = services.users.findAllByProntuarios(authorsText);
            Problem saved = services.problems.save(draft, authors, user);
            flash(request, saved.getStatus() == ReviewStatus.APPROVED ? "Problema salvo."
                    : "Problema salvo e enviado para a votação dos administradores.");
            redirect(request, response, "/problems/view?id=" + saved.getId());
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            showForm(request, response, draft, authorsText, inputs, outputs);
        }
    }

    private void submit(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = requireUser(request);
        long problemId = idParam(request, "problemId");
        Problem problem = services.problems.getVisible(user, problemId);
        try {
            Submission submission = services.submissions.submit(user, problemId, param(request, "language"), param(request, "code"));
            redirect(request, response, "/submissions?id=" + submission.getId());
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("code", param(request, "code"));
            request.setAttribute("selectedLanguage", param(request, "language"));
            showProblem(request, response, user, problem);
        }
    }

    /** Monta um problema a partir do formulário (sem os casos de teste); a validação fica a cargo do serviço. */
    private Problem readProblem(HttpServletRequest request) {
        Problem problem = new Problem();
        if (!param(request, "id").isBlank()) {
            problem.setId(idParam(request, "id"));
        }
        problem.setTitle(param(request, "title"));
        problem.setStatement(param(request, "statement"));
        problem.setExampleInput(param(request, "exampleInput"));
        problem.setExampleOutput(param(request, "exampleOutput"));
        problem.setTimeLimitMs(intParam(request, "timeLimitMs", -1));
        problem.setMemoryLimitMb(intParam(request, "memoryLimitMb", -1));
        problem.setPoints(intParam(request, "points", -1));
        problem.setTopics(Text.splitTopics(param(request, "topics")));
        return problem;
    }

    private String prontuarios(List<Long> userIds) {
        return userIds.stream().map(id -> services.users.find(id).map(User::getProntuario).orElse(""))
                .filter(p -> !p.isEmpty()).collect(Collectors.joining("\n"));
    }
}
