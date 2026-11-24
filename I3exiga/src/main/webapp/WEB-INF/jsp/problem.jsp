<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="${problem.title}">
    <t:problem-tabs problem="${problem}" active="statement"/>
    <p class="meta">${problem.points} pontos · tempo limite ${problem.timeLimitMs} ms · memória ${problem.memoryLimitMb} MB</p>
    <p class="meta">Autores:
        <v:each items="${problem.authorIds}" var="aid"><a href="${ctx}/users?id=${aid}">${v:h(usersById[aid].displayName)}</a>&nbsp; </v:each>
    </p>
    <p><v:each items="${problem.topics}" var="tp"><a class="tag" href="${ctx}/problems?topic=${v:enc(tp)}">${v:h(tp)}</a></v:each></p>
    <v:if test="${problem.status.name() != 'APPROVED'}">
        <div class="notice warn"><span class="badge ${problem.status.cssClass}">${problem.status.label}</span>
            Este problema ainda não está visível para os demais usuários: ele precisa da aprovação de todos os administradores.</div>
    </v:if>
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
            <select id="language" name="language">
                <v:each items="${languages}" var="l">
                    <option value="${l}" data-template="${v:h(l.template)}" ${selectedLanguage == l.name() ? 'selected' : ''}>${l.label}</option>
                </v:each>
            </select>
            <span class="hint">Em Java, a classe principal deve se chamar Main. Leia da entrada padrão e escreva na saída padrão.</span>
        </label>
        <label for="code">Código</label>
        <div class="code-editor">
            <pre aria-hidden="true"><code></code></pre>
            <textarea id="code" name="code" rows="18" required spellcheck="false" wrap="off" autocapitalize="off" autocomplete="off">${v:h(code)}</textarea>
        </div>
        <div><button type="submit" class="primary">Enviar</button></div>
    </form>

    <v:if test="${canManage}">
        <h2>Gerenciar</h2>
        <t:vote-panel kind="problem" itemId="${problem.id}" status="${problem.status}" ballots="${ballots}" canVote="${canVote}"/>
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
