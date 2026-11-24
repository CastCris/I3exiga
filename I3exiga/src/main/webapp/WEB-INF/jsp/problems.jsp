<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="Problemas">
    <h1>Problemas</h1>
    <v:if test="${currentUser.staff}">
        <div class="actions"><a class="button primary" href="${ctx}/problems/new">Novo problema</a></div>
    </v:if>
    <form method="get" action="${ctx}/problems" class="filters" role="search">
        <label class="narrow-field">Id
            <input type="number" name="id" min="1" value="${v:h(idFilter)}" placeholder="Ex.: 3">
        </label>
        <label>Nome
            <input type="search" name="q" value="${v:h(q)}" placeholder="Parte do título">
        </label>
        <label>Tópico
            <select name="topic">
                <option value="">Todos</option>
                <v:each items="${topics}" var="tp"><option value="${v:h(tp)}" ${topic == tp ? 'selected' : ''}>${v:h(tp)}</option></v:each>
            </select>
        </label>
        <div class="actions">
            <button type="submit" class="primary">Filtrar</button>
            <a href="${ctx}/problems">Limpar</a>
        </div>
    </form>
    <p class="meta">Exibindo ${problems.size()} de ${total} problema(s).</p>
    <v:if test="${empty problems}">
        <p class="empty">${total == 0 ? 'Ainda não há problemas cadastrados.' : 'Nenhum problema encontrado com esses filtros.'}</p>
    </v:if>
    <v:if test="${not empty problems}">
        <div class="table-wrap">
        <table>
            <thead><tr><th class="num">#</th><th>Título</th><th>Tópicos</th><th>Autores</th><th class="num">Pontos</th><th>Situação</th></tr></thead>
            <tbody>
            <v:each items="${problems}" var="p">
                <tr>
                    <td class="num">${p.id}</td>
                    <td><a href="${ctx}/problems/view?id=${p.id}">${v:h(p.title)}</a></td>
                    <td><v:each items="${p.topics}" var="tp"><a class="tag" href="${ctx}/problems?topic=${v:enc(tp)}">${v:h(tp)}</a></v:each></td>
                    <td><v:each items="${p.authorIds}" var="aid"><a href="${ctx}/users?id=${aid}">${v:h(usersById[aid].displayName)}</a>&nbsp; </v:each></td>
                    <td class="num">${p.points}</td>
                    <td>
                        <v:if test="${v:contains(solved, p.id)}"><span class="badge ok">Resolvido</span></v:if>
                        <v:if test="${p.status.name() != 'APPROVED'}"><span class="badge ${p.status.cssClass}">${p.status.label}</span></v:if>
                    </td>
                </tr>
            </v:each>
            </tbody>
        </table>
        </div>
    </v:if>
</t:layout>
