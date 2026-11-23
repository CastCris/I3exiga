<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="${profile.displayName}">
    <h1>${v:h(profile.displayName)}</h1>
    <dl class="stats">
        <div><dt class="meta">Pontuação</dt><dd><strong>${score}</strong></dd></div>
        <div><dt class="meta">Problemas resolvidos</dt><dd><strong>${solvedCount}</strong></dd></div>
    </dl>
    <table>
        <tbody>
            <tr><th scope="row">Nome completo</th><td>${empty profile.fullName ? 'Não informado' : v:h(profile.fullName)}</td></tr>
            <tr><th scope="row">Matrícula (prontuário)</th><td>${v:h(profile.prontuario)}</td></tr>
            <tr><th scope="row">Curso</th><td>${empty profile.course ? 'Não informado' : v:h(profile.course)}</td></tr>
            <tr><th scope="row">Categoria</th><td>${profile.role.label}</td></tr>
        </tbody>
    </table>
    <h2>Submissões nos últimos 30 dias</h2>
    ${chart}
    <p class="legend"><span><i class="ok"></i>Aceitas</span><span><i class="bad"></i>Rejeitadas</span></p>
</t:layout>
