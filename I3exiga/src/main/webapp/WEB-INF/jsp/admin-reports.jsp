<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="Denúncias">
    <h1>Denúncias de comentários</h1>
    <v:if test="${empty reports}"><p class="empty">Nenhuma denúncia pendente.</p></v:if>
    <v:each items="${reports}" var="r">
        <article class="comment">
            <header>
                <span>
                    <a href="${ctx}/users?id=${r.author.id}">${v:h(r.author.displayName)}</a> em
                    <a href="${ctx}/comments?problemId=${r.problem.id}">${v:h(r.problem.title)}</a>
                </span>
                <span class="badge bad">${r.reportCount} denúncia(s)</span>
            </header>
            <div class="text">${v:h(r.comment.text)}</div>
            <p class="meta">Motivos:
                <v:each items="${r.reasons}" var="reason">${v:h(reason)}; </v:each>
            </p>
            <form method="post" action="${ctx}/admin/reports/resolve" data-confirm="Confirmar a decisão?">
                <t:csrf/><input type="hidden" name="commentId" value="${r.comment.id}">
                <button type="submit" name="action" value="dismiss">Rejeitar denúncia</button>
                <button type="submit" name="action" value="delete" class="danger">Excluir comentário</button>
                <button type="submit" name="action" value="ban-temporary" class="danger">Excluir e banir por</button>
                <input type="number" name="days" value="7" min="1" max="365" aria-label="Dias de banimento">
                <span class="meta">dias</span>
                <button type="submit" name="action" value="ban-permanent" class="danger">Excluir e banir indeterminadamente</button>
            </form>
        </article>
    </v:each>
</t:layout>
