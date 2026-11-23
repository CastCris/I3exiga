<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="${empty article.id ? 'Novo artigo' : 'Editar artigo'}">
    <h1>${empty article.id ? 'Novo artigo' : 'Editar artigo'}</h1>
    <p class="meta">O PDF deve seguir o <a href="${ctx}/assets/modelo-artigo.tex" download>modelo LaTeX do sistema</a>.
        Depois de enviado, o artigo é avaliado por uma equipe de administradores.</p>
    <form method="post" action="${ctx}/articles/save" enctype="multipart/form-data" class="stack">
        <t:csrf/>
        <v:if test="${not empty article.id}"><input type="hidden" name="id" value="${article.id}"></v:if>
        <label>Título
            <input name="title" value="${v:h(article.title)}" required maxlength="150">
        </label>
        <div class="row">
            <label>Tipo
                <select name="type">
                    <v:each items="${types}" var="type">
                        <option value="${type}" ${article.type == type ? 'selected' : ''}>${type.label}</option>
                    </v:each>
                </select>
            </label>
            <label>Problema (somente para artigo de solução)
                <select name="problemId">
                    <option value="">—</option>
                    <v:each items="${problems}" var="p">
                        <option value="${p.id}" ${article.problemId == p.id ? 'selected' : ''}>${p.id} · ${v:h(p.title)}</option>
                    </v:each>
                </select>
            </label>
        </div>
        <label>Tópicos
            <input name="topics" value="${v:h(v:join(article.topics))}" maxlength="200" placeholder="grafos, bits">
            <span class="hint">Separe por vírgula (até 5).<v:if test="${not empty knownTopics}"> Já usados: ${v:h(v:join(knownTopics))}.</v:if></span>
        </label>
        <label>Autores (prontuários de docentes ou administradores)
            <textarea name="authors" rows="3" required>${v:h(authors)}</textarea>
            <span class="hint">Um por linha. Você deve permanecer na lista, a menos que seja administrador.</span>
        </label>
        <label>Arquivo PDF
            <input type="file" name="pdf" accept="application/pdf,.pdf" ${empty article.id ? 'required' : ''}>
            <span class="hint">Até 10 MB.${empty article.id ? '' : ' Deixe em branco para manter o PDF atual.'}</span>
        </label>
        <div class="actions">
            <button type="submit" class="primary">Enviar para avaliação</button>
            <a href="${ctx}/articles">Cancelar</a>
        </div>
    </form>
</t:layout>
