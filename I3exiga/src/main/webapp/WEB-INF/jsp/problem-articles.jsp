<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="${problem.title}">
    <t:problem-tabs problem="${problem}" active="articles"/>
    <p class="meta">Os artigos de solução ficam visíveis depois que você resolve o problema. Também é possível abri-los antes,
        mas o problema deixa de pontuar para você.</p>
    <v:if test="${currentUser.staff}">
        <div class="actions"><a class="button" href="${ctx}/articles/new?problemId=${problem.id}">Enviar artigo de solução</a></div>
    </v:if>
    <v:if test="${empty articles}"><p class="empty">Este problema ainda não tem artigos de solução.</p></v:if>
    <ul class="plain">
        <v:each items="${articles}" var="a">
            <li>
                <a href="${ctx}/articles/view?id=${a.id}">${v:h(a.title)}</a>
                <v:if test="${a.status.name() != 'APPROVED'}"><span class="badge ${a.status.cssClass}">${a.status.label}</span></v:if>
            </li>
        </v:each>
    </ul>
</t:layout>
