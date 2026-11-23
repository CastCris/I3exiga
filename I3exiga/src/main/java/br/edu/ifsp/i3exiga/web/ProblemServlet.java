package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.model.Language;
import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.model.Submission;
import br.edu.ifsp.i3exiga.model.TestCase;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.service.NotFoundException;
import br.edu.ifsp.i3exiga.service.ValidationException;
import br.edu.ifsp.i3exiga.util.Text;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Lista, visualização, cadastro de problemas e envio de submissões. */
@WebServlet("/problems/*")
public class ProblemServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        switch (path(request)) {
            case "", "/" -> list(request, response);
            case "/view" -> showProblem(request, response, services.problems.get(idParam(request, "id")));
            case "/submissions" -> mySubmissions(request, response);
            case "/new" -> {
                requireStaff(request);
                showForm(request, response, new Problem());
            }
            case "/edit" -> {
                requireStaff(request);
                showForm(request, response, services.problems.get(idParam(request, "id")));
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
                requireStaff(request);
                services.problems.delete(idParam(request, "id"));
                flash(request, "Problema removido.");
                redirect(request, response, "/problems");
            }
            default -> throw new NotFoundException();
        }
    }

    private void list(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("problems", services.problems.search(param(request, "q"), filterId(request), param(request, "topic")));
        request.setAttribute("total", services.problems.all().size());
        request.setAttribute("topics", services.problems.topics());
        request.setAttribute("q", param(request, "q"));
        request.setAttribute("topic", param(request, "topic"));
        request.setAttribute("idFilter", param(request, "id"));
        request.setAttribute("solved", services.scores.solvedBy(user(request).getId()));
        render(request, response, "problems");
    }

    private void showProblem(HttpServletRequest request, HttpServletResponse response, Problem problem)
            throws ServletException, IOException {
        request.setAttribute("problem", problem);
        request.setAttribute("languages", Language.values());
        render(request, response, "problem");
    }

    private void mySubmissions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Problem problem = services.problems.get(idParam(request, "id"));
        request.setAttribute("problem", problem);
        request.setAttribute("submissions", services.submissions.byUserAndProblem(user(request).getId(), problem.getId()));
        render(request, response, "problem-submissions");
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, Problem problem)
            throws ServletException, IOException {
        if (problem.getTestCases().isEmpty()) {
            problem.getTestCases().add(new TestCase("", ""));
        }
        request.setAttribute("problem", problem);
        request.setAttribute("knownTopics", services.problems.topics());
        render(request, response, "problem-form");
    }

    private void save(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = requireStaff(request);
        Problem draft = readProblem(request);
        try {
            Problem saved = services.problems.save(draft, user);
            flash(request, "Problema salvo.");
            redirect(request, response, "/problems/view?id=" + saved.getId());
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            showForm(request, response, draft);
        }
    }

    private void submit(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = requireUser(request);
        long problemId = idParam(request, "problemId");
        try {
            Submission submission = services.submissions.submit(user, problemId, param(request, "language"), param(request, "code"));
            redirect(request, response, "/submissions?id=" + submission.getId());
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("code", param(request, "code"));
            request.setAttribute("selectedLanguage", param(request, "language"));
            showProblem(request, response, services.problems.get(problemId));
        }
    }

    /** Monta um problema a partir do formulário; a validação fica a cargo do serviço. */
    private Problem readProblem(HttpServletRequest request) {
        Problem problem = new Problem();
        String id = param(request, "id");
        if (!id.isBlank()) {
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

        String[] inputs = request.getParameterValues("testInput");
        String[] outputs = request.getParameterValues("testOutput");
        List<TestCase> tests = new ArrayList<>();
        for (int i = 0; inputs != null && outputs != null && i < Math.min(inputs.length, outputs.length); i++) {
            tests.add(new TestCase(inputs[i], outputs[i]));
        }
        problem.setTestCases(tests);
        return problem;
    }
}
