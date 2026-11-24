<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="Votações">
    <h1>Votações dos administradores</h1>
    <p class="meta">Problemas e artigos novos só são publicados com o voto a favor de <strong>todos</strong> os administradores.
        Aqui ficam os itens ainda não aprovados; quem votou a favor e contra aparece em cada item.</p>
    <v:if test="${empty items}"><p class="empty">Nenhum problema ou artigo aguardando votação.</p></v:if>
    <v:each items="${items}" var="item">
        <article>
            <h2><a href="${ctx}${item.path}">${v:h(item.title)}</a></h2>
            <p class="meta">${v:h(item.description)} · autores:
                <v:each items="${item.authors}" var="author">${v:h(author.displayName)}&nbsp; </v:each>
            </p>
            <t:vote-panel kind="${item.kind}" itemId="${item.id}" status="${item.status}" ballots="${item.ballots}"
                          criteria="${item.criteria}" canVote="true"/>
        </article>
    </v:each>
</t:layout>
