<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="${article.title}">
    <h1>${v:h(article.title)}</h1>
    <p class="meta">
        ${article.type.label}
        <v:if test="${not empty problem}"> · problema <a href="${ctx}/problems/view?id=${problem.id}">${v:h(problem.title)}</a></v:if>
        · autores:
        <v:each items="${article.authorIds}" var="authorId">${v:h(usersById[authorId].displayName)}&nbsp; </v:each>
    </p>
    <p><v:each items="${article.topics}" var="tp"><span class="tag">${v:h(tp)}</span></v:each></p>
    <v:if test="${canManage}">
        <p><span class="badge ${article.status.cssClass}">${article.status.label}</span></p>
    </v:if>

    <v:if test="${canView}">
        <div class="actions"><a class="button primary" href="${ctx}/articles/file?id=${article.id}" target="_blank" rel="noopener">Abrir PDF</a></div>
    </v:if>
    <v:if test="${canUnlock}">
        <div class="notice warn">
            <p>Este artigo de solução é liberado depois que você resolve o problema.</p>
            <p>Se preferir abri-lo agora, os pontos de <strong>todas</strong> as suas submissões deste problema serão descontados:
                o problema não pontuará para você, mesmo que o resolva depois.</p>
            <form method="post" action="${ctx}/articles/unlock" data-confirm="Abrir o artigo e abrir mão dos pontos deste problema?">
                <t:csrf/>
                <input type="hidden" name="id" value="${article.id}">
                <button type="submit" class="danger">Abrir artigo e descontar os pontos</button>
            </form>
        </div>
    </v:if>
    <v:if test="${not canView and not canUnlock}">
        <div class="notice warn">Entre na plataforma e resolva o problema para ver este artigo.
            <a href="${ctx}/login">Entrar</a></div>
    </v:if>

    <v:if test="${canManage}">
        <div class="actions">
            <a class="button" href="${ctx}/articles/edit?id=${article.id}">Editar</a>
            <form method="post" action="${ctx}/articles/delete" class="inline" data-confirm="Remover este artigo?">
                <t:csrf/>
                <input type="hidden" name="id" value="${article.id}">
                <button type="submit" class="danger">Remover</button>
            </form>
        </div>
    </v:if>
    <v:if test="${canManage}">
        <t:vote-panel kind="article" itemId="${article.id}" status="${article.status}" ballots="${ballots}" criteria="${criteria}" canVote="${canVote}"/>
    </v:if>
</t:layout>
