<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="Placares">
    <h1>Placares</h1>
    <p><a href="${ctx}/scoreboards/general">Ver o placar geral</a></p>
    <v:if test="${currentUser.staff}">
        <div class="actions"><a class="button primary" href="${ctx}/scoreboards/new">Novo placar</a></div>
    </v:if>
    <v:if test="${empty boards}"><p class="empty">Não há placares personalizados disponíveis para você.</p></v:if>
    <ul class="plain">
        <v:each items="${boards}" var="b">
            <li>
                <a href="${ctx}/scoreboards/view?id=${b.id}">${v:h(b.name)}</a>
                <span class="badge ${b.publicAccess ? 'ok' : 'pending'}">${b.publicAccess ? 'Público' : 'Restrito'}</span>
                <div class="meta">${v:h(b.description)}</div>
            </li>
        </v:each>
    </ul>
</t:layout>
