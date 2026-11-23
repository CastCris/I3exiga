<%@ tag description="Título e abas de um problema" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="problem" required="true" type="br.edu.ifsp.i3exiga.model.Problem" %>
<%@ attribute name="active" required="true" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<h1>${v:h(problem.title)}</h1>
<nav class="tabs" aria-label="Seções do problema">
    <a class="${active == 'statement' ? 'active' : ''}" href="${ctx}/problems/view?id=${problem.id}">Enunciado</a>
    <a class="${active == 'submissions' ? 'active' : ''}" href="${ctx}/problems/submissions?id=${problem.id}">Minhas submissões</a>
    <a class="${active == 'comments' ? 'active' : ''}" href="${ctx}/comments?problemId=${problem.id}">Comentários</a>
    <a class="${active == 'articles' ? 'active' : ''}" href="${ctx}/articles?problemId=${problem.id}">Artigos</a>
</nav>
