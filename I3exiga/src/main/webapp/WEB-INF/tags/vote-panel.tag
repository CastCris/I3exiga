<%@ tag description="Votação unânime dos administradores: quem votou a favor/contra e formulário de voto" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="kind" required="true" %>
<%@ attribute name="itemId" required="true" type="java.lang.Long" %>
<%@ attribute name="status" required="true" type="br.edu.ifsp.i3exiga.model.ReviewStatus" %>
<%@ attribute name="ballots" required="true" type="java.util.List" %>
<%@ attribute name="criteria" required="false" type="java.util.List" %>
<%@ attribute name="canVote" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<section class="panel votes">
    <h2>Votação dos administradores <span class="badge ${status.cssClass}">${status.label}</span></h2>
    <p class="meta">A aprovação é unânime: todos os administradores precisam votar a favor, e um voto contra rejeita.
        O voto pode ser alterado até a aprovação, e um novo envio dos autores reinicia a votação.</p>
    <div class="table-wrap">
    <table>
        <thead><tr><th>Administrador</th><th>Voto</th><th>Comentário</th><th>Data</th></tr></thead>
        <tbody>
        <v:each items="${ballots}" var="b">
            <tr>
                <td><a href="${ctx}/users?id=${b.admin.id}">${v:h(b.admin.displayName)}</a></td>
                <td>
                    <v:if test="${empty b.vote}"><span class="badge pending">Aguardando</span></v:if>
                    <v:if test="${not empty b.vote and b.vote.approve}"><span class="badge ok">A favor</span></v:if>
                    <v:if test="${not empty b.vote and not b.vote.approve}"><span class="badge bad">Contra</span></v:if>
                </td>
                <td class="text">${v:h(b.vote.comment)}</td>
                <td>${v:date(b.vote.votedAt)}</td>
            </tr>
        </v:each>
        </tbody>
    </table>
    </div>

    <v:if test="${canVote}">
        <form method="post" action="${ctx}/admin/reviews/vote" class="stack">
            <t:csrf/>
            <input type="hidden" name="kind" value="${kind}">
            <input type="hidden" name="id" value="${itemId}">
            <v:if test="${not empty criteria}">
                <fieldset>
                    <legend>Critérios (confirme todos para votar a favor)</legend>
                    <v:each items="${criteria}" var="criterion">
                        <label class="check"><input type="checkbox" name="criteria" value="${v:h(criterion)}"> ${v:h(criterion)}</label>
                    </v:each>
                </fieldset>
            </v:if>
            <label>Comentário <span class="hint">(obrigatório ao votar contra)</span>
                <textarea name="comment" rows="3" maxlength="1000"></textarea>
            </label>
            <div class="actions">
                <button type="submit" name="decision" value="approve" class="primary">Votar a favor</button>
                <button type="submit" name="decision" value="reject" class="danger">Votar contra</button>
            </div>
        </form>
    </v:if>
</section>
