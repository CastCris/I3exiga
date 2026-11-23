<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="${empty problem.id ? 'Novo problema' : 'Editar problema'}">
    <h1>${empty problem.id ? 'Novo problema' : 'Editar problema'}</h1>
    <form method="post" action="${ctx}/problems/save" class="stack">
        <t:csrf/>
        <v:if test="${not empty problem.id}"><input type="hidden" name="id" value="${problem.id}"></v:if>
        <label>Título
            <input name="title" value="${v:h(problem.title)}" required maxlength="120">
        </label>
        <label>Enunciado
            <textarea name="statement" rows="10" required>${v:h(problem.statement)}</textarea>
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
        <p class="hint">A correção para no primeiro caso que falhar. A saída esperada é obrigatória.</p>
        <div id="tests">
            <v:each items="${problem.testCases}" var="test">
                <fieldset class="test-row">
                    <legend>Caso de teste</legend>
                    <div class="grid2">
                        <label>Entrada
                            <textarea class="code" name="testInput" rows="4">${v:h(test.input)}</textarea>
                        </label>
                        <label>Saída esperada
                            <textarea class="code" name="testOutput" rows="4">${v:h(test.expectedOutput)}</textarea>
                        </label>
                    </div>
                    <button type="button" class="link remove-test">Remover este caso</button>
                </fieldset>
            </v:each>
        </div>
        <div><button type="button" id="add-test">Adicionar caso de teste</button></div>

        <div class="actions">
            <button type="submit" class="primary">Salvar problema</button>
            <a href="${ctx}/problems">Cancelar</a>
        </div>
    </form>
</t:layout>
