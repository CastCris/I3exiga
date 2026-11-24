package br.edu.ifsp.i3exiga.web;

import br.edu.ifsp.i3exiga.model.Language;
import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.model.Submission;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.model.Verdict;
import br.edu.ifsp.i3exiga.service.AccessDeniedException;
import br.edu.ifsp.i3exiga.service.NotFoundException;
import br.edu.ifsp.i3exiga.util.Dates;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resultado de uma submissão (código visível ao autor e à equipe) e lista de todas as submissões da
 * plataforma, com filtros. A lista mostra apenas dados gerais: o código de terceiros não é exibido.
 */
@WebServlet("/submissions/*")
public class SubmissionServlet extends BaseServlet {

    private static final int PAGE_SIZE = 50;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = requireUser(request);
        switch (path(request)) {
            case "", "/" -> view(request, response, user);
            case "/all" -> list(request, response, user);
            default -> throw new NotFoundException();
        }
    }

    private void view(HttpServletRequest request, HttpServletResponse response, User user) throws ServletException, IOException {
        Submission submission = services.submissions.get(idParam(request, "id"));
        if (!submission.getUserId().equals(user.getId()) && !user.isStaff()) {
            throw new AccessDeniedException();
        }
        request.setAttribute("submission", submission);
        request.setAttribute("problem", services.problems.get(submission.getProblemId()));
        render(request, response, "submission");
    }

    private void list(HttpServletRequest request, HttpServletResponse response, User user) throws ServletException, IOException {
        List<Problem> visibleProblems = services.problems.visibleTo(user);
        Set<Long> visibleIds = visibleProblems.stream().map(Problem::getId).collect(Collectors.toSet());

        String problemText = param(request, "problem").strip();
        Long problemId = problemText.isEmpty() ? null : parseLong(problemText);
        LocalDate fromDate = parseDate(request, "from");
        LocalDate toDate = parseDate(request, "to");
        Verdict verdict = parseEnum(Verdict.class, param(request, "result"));
        Language language = parseEnum(Language.class, param(request, "language"));
        Instant from = fromDate == null ? null : fromDate.atStartOfDay(Dates.ZONE).toInstant();
        Instant to = toDate == null ? null : toDate.plusDays(1).atStartOfDay(Dates.ZONE).toInstant();

        List<Submission> found = services.submissions.search(visibleIds, problemId, param(request, "user"), from, to, verdict, language);
        int pages = Math.max(1, (found.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(1, intParam(request, "page", 1)), pages);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("problem", problemText);
        filters.put("user", param(request, "user").strip());
        filters.put("from", param(request, "from").strip());
        filters.put("to", param(request, "to").strip());
        filters.put("result", param(request, "result"));
        filters.put("language", param(request, "language"));

        request.setAttribute("submissions", found.subList((page - 1) * PAGE_SIZE, Math.min(found.size(), page * PAGE_SIZE)));
        request.setAttribute("found", found.size());
        request.setAttribute("page", page);
        request.setAttribute("pages", pages);
        request.setAttribute("filters", filters);
        request.setAttribute("queryString", queryString(filters));
        request.setAttribute("problems", visibleProblems);
        request.setAttribute("problemsById", visibleProblems.stream().collect(Collectors.toMap(Problem::getId, Function.identity())));
        request.setAttribute("usersById", services.users.mapById());
        request.setAttribute("verdicts", Verdict.values());
        request.setAttribute("languages", Language.values());
        render(request, response, "submissions");
    }

    private LocalDate parseDate(HttpServletRequest request, String name) {
        String text = param(request, name).strip();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            request.setAttribute("error", "Use datas no formato AAAA-MM-DD.");
            return null;
        }
    }

    private static Long parseLong(String text) {
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            return -1L; // nenhum problema tem esse id: a lista fica vazia
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String text) {
        try {
            return Enum.valueOf(type, text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Filtros no formato de URL (sem a página), para os links de paginação. */
    private static String queryString(Map<String, String> filters) {
        return filters.entrySet().stream().filter(e -> !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
