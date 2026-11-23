<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="${problem.title}">
    <t:problem-tabs problem="${problem}" active="statement"/>
    <p class="meta">${problem.points} pontos · tempo limite ${problem.timeLimitMs} ms · memória ${problem.memoryLimitMb} MB</p>
    <p><v:each items="${problem.topics}" var="tp"><a class="tag" href="${ctx}/problems?topic=${v:enc(tp)}">${v:h(tp)}</a></v:each></p>
    <div class="panel text">${v:h(problem.statement)}</div>

    <v:if test="${not empty problem.exampleInput or not empty problem.exampleOutput}">
        <div class="grid2">
            <div><strong>Exemplo de entrada</strong><pre>${v:h(problem.exampleInput)}</pre></div>
            <div><strong>Exemplo de saída</strong><pre>${v:h(problem.exampleOutput)}</pre></div>
        </div>
    </v:if>

    <h2>Enviar solução</h2>
    <form method="post" action="${ctx}/problems/submit" class="stack">
        <t:csrf/>
        <input type="hidden" name="problemId" value="${problem.id}">
        <label>Linguagem
            <select name="language">
                <v:each items="${languages}" var="l">
                    <option value="${l}" ${selectedLanguage == l.name() ? 'selected' : ''}>${l.label}</option>
                </v:each>
            </select>
            <span class="hint">Em Java, a classe principal deve se chamar Main. Leia da entrada padrão e escreva na saída padrão.</span>
        </label>
        <label>Código
            <textarea class="code" name="code" rows="16" required spellcheck="false">${v:h(code)}</textarea>
        </label>
        <div><button type="submit" class="primary">Enviar</button></div>
    </form>

    <v:if test="${currentUser.staff}">
        <h2>Gerenciar</h2>
        <div class="actions">
            <a class="button" href="${ctx}/problems/edit?id=${problem.id}">Editar problema</a>
            <a class="button" href="${ctx}/articles/new?problemId=${problem.id}">Novo artigo de solução</a>
            <form method="post" action="${ctx}/problems/delete" class="inline"
                  data-confirm="Remover o problema e todas as suas submissões, comentários e artigos de solução?">
                <t:csrf/>
                <input type="hidden" name="id" value="${problem.id}">
                <button type="submit" class="danger">Remover problema</button>
            </form>
        </div>
    </v:if>
</t:layout>
