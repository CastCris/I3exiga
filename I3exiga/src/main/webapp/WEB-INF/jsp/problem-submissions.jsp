<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="${problem.title}">
    <t:problem-tabs problem="${problem}" active="submissions"/>
    <v:if test="${empty submissions}">
        <p class="empty">Você ainda não enviou soluções para este problema.</p>
    </v:if>
    <v:if test="${not empty submissions}">
        <div class="table-wrap">
        <table>
            <thead><tr><th class="num">#</th><th>Enviada em</th><th>Linguagem</th><th>Resultado</th></tr></thead>
            <tbody>
            <v:each items="${submissions}" var="s">
                <tr>
                    <td class="num"><a href="${ctx}/submissions?id=${s.id}">${s.id}</a></td>
                    <td>${v:date(s.submittedAt)}</td>
                    <td>${s.language.label}</td>
                    <td><span class="badge ${s.verdict.cssClass}">${s.verdict.label}</span></td>
                </tr>
            </v:each>
            </tbody>
        </table>
        </div>
    </v:if>
</t:layout>
