<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="Submissão ${submission.id}" autoRefresh="${submission.verdict.pending}">
    <h1>Submissão ${submission.id}</h1>
    <p class="meta">
        Problema <a href="${ctx}/problems/view?id=${problem.id}">${v:h(problem.title)}</a>
        · ${submission.language.label} · ${v:date(submission.submittedAt)}
    </p>
    <p><span class="badge ${submission.verdict.cssClass}">${submission.verdict.label}</span></p>

    <v:if test="${submission.verdict.pending}">
        <p class="meta">A correção está em andamento. Esta página atualiza sozinha.</p>
    </v:if>
    <v:if test="${submission.verdict.accepted}">
        <div class="notice ok">Parabéns! Todos os casos de teste passaram. A pontuação do problema foi creditada no placar.</div>
    </v:if>

    <v:if test="${submission.verdict.name() == 'COMPILATION_ERROR'}">
        <h2>Mensagens do compilador</h2>
        <pre>${v:h(submission.details)}</pre>
    </v:if>

    <v:if test="${submission.failedTestCase > 0}">
        <h2>Caso de teste ${submission.failedTestCase}</h2>
        <div class="grid2">
            <div><strong>Saída esperada</strong><pre>${v:h(submission.expectedOutput)}</pre></div>
            <div><strong>Saída recebida</strong><pre>${v:h(submission.receivedOutput)}</pre></div>
        </div>
        <v:if test="${not empty submission.details}">
            <p><strong>Mensagens do programa</strong></p>
            <pre>${v:h(submission.details)}</pre>
        </v:if>
        <p class="hint">São exibidas no máximo 20 linhas de cada saída.</p>
    </v:if>

    <h2>Código enviado</h2>
    <pre>${v:h(submission.sourceCode)}</pre>
</t:layout>
