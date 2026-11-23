<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="${problem.title}">
    <t:problem-tabs problem="${problem}" active="comments"/>

    <v:if test="${empty comments}"><p class="empty">Ainda não há comentários. Seja o primeiro a comentar.</p></v:if>
    <v:each items="${comments}" var="c">
        <article class="comment">
            <header>
                <a href="${ctx}/users?id=${c.authorId}">${v:h(usersById[c.authorId].displayName)}</a>
                <span class="meta">${v:date(c.createdAt)}</span>
            </header>
            <div class="text">${v:h(c.text)}</div>
            <v:if test="${c.authorId != currentUser.id}">
                <form method="post" action="${ctx}/comments/report">
                    <t:csrf/>
                    <input type="hidden" name="problemId" value="${problem.id}">
                    <input type="hidden" name="commentId" value="${c.id}">
                    <select name="reason" aria-label="Motivo da denúncia">
                        <v:each items="${reasons}" var="reason"><option>${v:h(reason)}</option></v:each>
                    </select>
                    <button type="submit">Denunciar</button>
                </form>
            </v:if>
        </article>
    </v:each>

    <h2>Novo comentário</h2>
    <form method="post" action="${ctx}/comments/add" class="stack">
        <t:csrf/>
        <input type="hidden" name="problemId" value="${problem.id}">
        <label>Comentário
            <textarea name="text" rows="5" maxlength="2000" required>${v:h(text)}</textarea>
            <span class="hint">Sem termos profanos ou ofensivos, relacionado à solução do problema e sem código com a solução.
                Comentários fora dessas regras podem ser denunciados e levar ao banimento.</span>
        </label>
        <div><button type="submit" class="primary">Comentar</button></div>
    </form>
</t:layout>
