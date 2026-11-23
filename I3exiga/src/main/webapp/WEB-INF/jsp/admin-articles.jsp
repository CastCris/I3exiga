<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="Avaliação de artigos">
    <h1>Artigos em avaliação</h1>
    <v:if test="${empty articles}"><p class="empty">Nenhum artigo aguardando avaliação.</p></v:if>
    <v:each items="${articles}" var="a">
        <section class="panel">
            <h2>${v:h(a.title)}</h2>
            <p class="meta">
                ${a.type.label} · enviado em ${v:date(a.submittedAt)} · autores:
                <v:each items="${a.authorIds}" var="authorId">${v:h(usersById[authorId].displayName)}&nbsp; </v:each>
                <v:if test="${not empty a.problemId}">· <a href="${ctx}/problems/view?id=${a.problemId}">problema ${a.problemId}</a></v:if>
            </p>
            <p><a class="button" href="${ctx}/articles/file?id=${a.id}" target="_blank" rel="noopener">Abrir PDF</a></p>
            <form method="post" action="${ctx}/admin/articles/review" class="stack">
                <t:csrf/><input type="hidden" name="articleId" value="${a.id}">
                <fieldset>
                    <legend>Critérios (confirme todos para aprovar)</legend>
                    <v:each items="${criteria[a.type]}" var="criterion">
                        <label class="check"><input type="checkbox" name="criteria" value="${v:h(criterion)}"> ${v:h(criterion)}</label>
                    </v:each>
                </fieldset>
                <label>Parecer para os autores (obrigatório ao rejeitar)
                    <textarea name="feedback" rows="3" maxlength="2000"></textarea>
                </label>
                <div class="actions">
                    <button type="submit" name="decision" value="approve" class="primary">Aprovar</button>
                    <button type="submit" name="decision" value="reject" class="danger">Rejeitar</button>
                </div>
            </form>
        </section>
    </v:each>
</t:layout>
