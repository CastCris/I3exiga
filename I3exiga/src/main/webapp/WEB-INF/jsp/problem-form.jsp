<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="${empty problem.id ? 'Novo problema' : 'Editar problema'}">
    <h1>${empty problem.id ? 'Novo problema' : 'Editar problema'}</h1>
    <p class="meta">Problemas novos só aparecem para os usuários depois que <strong>todos os administradores</strong> votarem a favor.
        Se o problema ainda não foi aprovado, editá-lo reinicia a votação.</p>
    <form method="post" action="${ctx}/problems/save" class="stack">
        <t:csrf/>
        <v:if test="${not empty problem.id}"><input type="hidden" name="id" value="${problem.id}"></v:if>
        <label>Título
            <input name="title" value="${v:h(problem.title)}" required maxlength="120">
        </label>
        <label>Enunciado
            <textarea name="statement" rows="10" required>${v:h(problem.statement)}</textarea>
        </label>
        <label>Autores (prontuários de docentes ou administradores)
            <textarea name="authors" rows="2" required>${v:h(authors)}</textarea>
            <span class="hint">Um por linha. Você deve permanecer na lista, a menos que seja administrador.</span>
        </label>
        <label>Tópicos
            <input name="topics" value="${v:h(v:join(problem.topics))}" maxlength="200" placeholder="grafos, programação dinâmica">
            <span class="hint">Separe por vírgula (até 5).<v:if test="${not empty knownTopics}"> Já usados: ${v:h(v:join(knownTopics))}.</v:if></span>
        </label>
        <div class="row">
            <label>Exemplo de entrada
                <textarea class="code" name="exampleInput" rows="4">${v:h(problem.exampleInput)}</textarea>
            </label>
            <label>Exemplo de saída
                <textarea class="code" name="exampleOutput" rows="4">${v:h(problem.exampleOutput)}</textarea>
            </label>
        </div>
        <div class="row">
            <label>Tempo limite (ms)
                <input type="number" name="timeLimitMs" value="${problem.timeLimitMs}" min="100" max="10000" required>
            </label>
            <label>Memória limite (MB)
                <input type="number" name="memoryLimitMb" value="${problem.memoryLimitMb}" min="16" max="512" required>
            </label>
            <v:if test="${currentUser.admin}">
                <label>Pontos
                    <input type="number" name="points" value="${problem.points}" min="1" max="1000" required>
                    <span class="hint">Definidos pelos administradores conforme a dificuldade.</span>
                </label>
            </v:if>
        </div>
        <v:if test="${not currentUser.admin}">
            <p class="hint">A pontuação do problema é definida pela equipe de administradores.</p>
        </v:if>

        <h2>Casos de teste</h2>
        <p class="hint">Cada linha é um caso de teste: a linha N das entradas combina com a linha N das saídas. Para uma quebra de
            linha dentro de uma entrada ou saída, escreva <code>\n</code> (exemplo: <code>5\n3 9 2 7 1</code>); para uma barra, <code>\\</code>.
            A correção para no primeiro caso que falhar.</p>
        <div class="grid2">
            <label>Entradas (uma por linha)
                <textarea class="code tests" name="testInputs" rows="10" wrap="off" data-sync="tests" required spellcheck="false">${v:h(testInputs)}</textarea>
            </label>
            <label>Saídas esperadas (uma por linha)
                <textarea class="code tests" name="testOutputs" rows="10" wrap="off" data-sync="tests" required spellcheck="false">${v:h(testOutputs)}</textarea>
            </label>
        </div>

        <div class="actions">
            <button type="submit" class="primary">Salvar problema</button>
            <a href="${ctx}/problems">Cancelar</a>
        </div>
    </form>
</t:layout>
