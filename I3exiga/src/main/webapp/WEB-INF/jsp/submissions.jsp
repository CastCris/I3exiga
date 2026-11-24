<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="Submissões">
    <h1>Submissões</h1>
    <p class="meta">Todas as submissões da plataforma. O código só é visível para quem enviou e para a equipe.</p>

    <form method="get" action="${ctx}/submissions/all" class="filters" role="search">
        <label>Problema
            <select name="problem">
                <option value="">Todos</option>
                <v:each items="${problems}" var="p">
                    <option value="${p.id}" ${filters.problem == p.id ? 'selected' : ''}>${p.id} · ${v:h(p.title)}</option>
                </v:each>
            </select>
        </label>
        <label>Usuário
            <input type="search" name="user" value="${v:h(filters.user)}" placeholder="Nome ou prontuário">
        </label>
        <label class="narrow-field">De
            <input type="date" name="from" value="${v:h(filters.from)}">
        </label>
        <label class="narrow-field">Até
            <input type="date" name="to" value="${v:h(filters.to)}">
        </label>
        <label>Resultado
            <select name="result">
                <option value="">Todos</option>
                <v:each items="${verdicts}" var="vd">
                    <option value="${vd}" ${filters.result == vd.name() ? 'selected' : ''}>${vd.label}</option>
                </v:each>
            </select>
        </label>
        <label>Linguagem
            <select name="language">
                <option value="">Todas</option>
                <v:each items="${languages}" var="lg">
                    <option value="${lg}" ${filters.language == lg.name() ? 'selected' : ''}>${lg.label}</option>
                </v:each>
            </select>
        </label>
        <div class="actions">
            <button type="submit" class="primary">Filtrar</button>
            <a href="${ctx}/submissions/all">Limpar</a>
        </div>
    </form>

    <p class="meta">${found} submissão(ões) encontrada(s).</p>
    <v:if test="${empty submissions}"><p class="empty">Nenhuma submissão encontrada com esses filtros.</p></v:if>
    <v:if test="${not empty submissions}">
        <div class="table-wrap">
        <table>
            <thead><tr><th class="num">#</th><th>Data</th><th>Usuário</th><th>Problema</th><th>Linguagem</th><th>Resultado</th><th></th></tr></thead>
            <tbody>
            <v:each items="${submissions}" var="s">
                <tr>
                    <td class="num">${s.id}</td>
                    <td>${v:date(s.submittedAt)}</td>
                    <td><a href="${ctx}/users?id=${s.userId}">${v:h(usersById[s.userId].displayName)}</a></td>
                    <td><a href="${ctx}/problems/view?id=${s.problemId}">${s.problemId} · ${v:h(problemsById[s.problemId].title)}</a></td>
                    <td>${s.language.label}</td>
                    <td><span class="badge ${s.verdict.cssClass}">${s.verdict.label}</span></td>
                    <td><v:if test="${currentUser.staff or s.userId == currentUser.id}"><a href="${ctx}/submissions?id=${s.id}">Ver código</a></v:if></td>
                </tr>
            </v:each>
            </tbody>
        </table>
        </div>
        <div class="pager">
            <v:if test="${page > 1}"><a class="button" href="${ctx}/submissions/all?${v:h(queryString)}${empty queryString ? '' : '&amp;'}page=${page - 1}">Anterior</a></v:if>
            <span class="meta">Página ${page} de ${pages}</span>
            <v:if test="${page < pages}"><a class="button" href="${ctx}/submissions/all?${v:h(queryString)}${empty queryString ? '' : '&amp;'}page=${page + 1}">Próxima</a></v:if>
        </div>
    </v:if>
</t:layout>
