<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="${title}">
    <h1>${v:h(title)}</h1>
    <v:if test="${not empty board.description}"><p class="meta">${v:h(board.description)}</p></v:if>

    <v:if test="${not empty board}">
        <div class="actions">
            <v:if test="${board.publicAccess and not isMember}">
                <form method="post" action="${ctx}/scoreboards/join" class="inline">
                    <t:csrf/><input type="hidden" name="id" value="${board.id}">
                    <button type="submit" class="primary">Participar deste placar</button>
                </form>
            </v:if>
            <v:if test="${isMember}">
                <form method="post" action="${ctx}/scoreboards/leave" class="inline" data-confirm="Sair deste placar?">
                    <t:csrf/><input type="hidden" name="id" value="${board.id}">
                    <button type="submit">Sair do placar</button>
                </form>
            </v:if>
            <v:if test="${canManage}">
                <a class="button" href="${ctx}/scoreboards/edit?id=${board.id}">Editar</a>
                <form method="post" action="${ctx}/scoreboards/delete" class="inline" data-confirm="Remover este placar?">
                    <t:csrf/><input type="hidden" name="id" value="${board.id}">
                    <button type="submit" class="danger">Remover</button>
                </form>
            </v:if>
        </div>
    </v:if>

    <form method="get" action="${ctx}${formAction}" class="row">
        <v:if test="${not empty boardId}"><input type="hidden" name="id" value="${boardId}"></v:if>
        <label>Buscar usuário
            <input type="search" name="q" value="${v:h(q)}" placeholder="Nome ou prontuário">
        </label>
        <label>Ordem
            <select name="order">
                <option value="desc" ${ascending ? '' : 'selected'}>Maior pontuação primeiro</option>
                <option value="asc" ${ascending ? 'selected' : ''}>Menor pontuação primeiro</option>
            </select>
        </label>
        <div><button type="submit">Aplicar</button></div>
    </form>

    <v:if test="${empty rows}"><p class="empty">Nenhum usuário encontrado.</p></v:if>
    <v:if test="${not empty rows}">
        <div class="table-wrap">
        <table>
            <thead><tr><th class="num">Posição</th><th>Usuário</th><th>Prontuário</th><th class="num">Pontos</th></tr></thead>
            <tbody>
            <v:each items="${rows}" var="row">
                <tr>
                    <td class="num">${row.rank}</td>
                    <td><a href="${ctx}/users?id=${row.user.id}">${v:h(row.user.displayName)}</a></td>
                    <td>${v:h(row.user.prontuario)}</td>
                    <td class="num">${row.score}</td>
                </tr>
            </v:each>
            </tbody>
        </table>
        </div>
    </v:if>
</t:layout>
