<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="Artigos">
    <h1>Artigos de conhecimento geral</h1>
    <p class="meta">Textos abertos ao público sobre temas como grafos, programação dinâmica, operações binárias e uso da plataforma.</p>
    <v:if test="${currentUser.staff}">
        <div class="actions">
            <a class="button primary" href="${ctx}/articles/new">Enviar artigo</a>
            <a class="button" href="${ctx}/assets/modelo-artigo.tex" download>Baixar modelo LaTeX</a>
        </div>
    </v:if>
    <form method="get" action="${ctx}/articles" class="filters" role="search">
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
            <a href="${ctx}/articles">Limpar</a>
        </div>
    </form>
    <p class="meta">Exibindo ${articles.size()} de ${total} artigo(s).</p>
    <v:if test="${empty articles}"><p class="empty">${total == 0 ? 'Ainda não há artigos publicados.' : 'Nenhum artigo encontrado com esses filtros.'}</p></v:if>
    <ul class="plain">
        <v:each items="${articles}" var="a">
            <li>
                <a href="${ctx}/articles/view?id=${a.id}">${v:h(a.title)}</a>
                <v:each items="${a.topics}" var="tp"><a class="tag" href="${ctx}/articles?topic=${v:enc(tp)}">${v:h(tp)}</a></v:each>
                <div class="meta">#${a.id} ·

                    <v:each items="${a.authorIds}" var="authorId">${v:h(usersById[authorId].displayName)}&nbsp; </v:each>
                </div>
            </li>
        </v:each>
    </ul>

    <v:if test="${not empty mine}">
        <h2>Meus artigos</h2>
        <div class="table-wrap">
        <table>
            <thead><tr><th>Título</th><th>Tipo</th><th>Situação</th></tr></thead>
            <tbody>
            <v:each items="${mine}" var="a">
                <tr>
                    <td><a href="${ctx}/articles/view?id=${a.id}">${v:h(a.title)}</a></td>
                    <td>${a.type.label}</td>
                    <td><span class="badge ${a.status.cssClass}">${a.status.label}</span></td>
                </tr>
            </v:each>
            </tbody>
        </table>
        </div>
    </v:if>
</t:layout>
