<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="Usuários">
    <h1>Usuários</h1>
    <div class="table-wrap">
    <table>
        <thead><tr><th>Prontuário</th><th>Nome</th><th>Categoria</th><th>Situação</th></tr></thead>
        <tbody>
        <v:each items="${users}" var="u">
            <tr>
                <td><a href="${ctx}/users?id=${u.id}">${v:h(u.prontuario)}</a></td>
                <td>${v:h(u.fullName)}</td>
                <td>
                    <form method="post" action="${ctx}/admin/users/role" class="row">
                        <t:csrf/><input type="hidden" name="userId" value="${u.id}">
                        <select name="role" aria-label="Categoria de ${v:h(u.prontuario)}">
                            <v:each items="${roles}" var="r"><option value="${r}" ${u.role == r ? 'selected' : ''}>${r.label}</option></v:each>
                        </select>
                        <button type="submit">Alterar</button>
                    </form>
                </td>
                <td>
                    <v:if test="${u.banned}">
                        <span class="badge bad">${u.permanentlyBanned ? 'Banido indeterminadamente' : 'Banido até '}${u.permanentlyBanned ? '' : v:date(u.bannedUntil)}</span>
                        <form method="post" action="${ctx}/admin/users/ban" class="inline">
                            <t:csrf/><input type="hidden" name="userId" value="${u.id}"><input type="hidden" name="mode" value="unban">
                            <button type="submit">Remover banimento</button>
                        </form>
                    </v:if>
                    <v:if test="${not u.banned}">
                        <form method="post" action="${ctx}/admin/users/ban" class="row" data-confirm="Banir este usuário?">
                            <t:csrf/><input type="hidden" name="userId" value="${u.id}">
                            <select name="mode" aria-label="Tipo de banimento">
                                <option value="temporary">Temporário</option>
                                <option value="permanent">Indeterminado</option>
                            </select>
                            <input type="number" name="days" value="7" min="1" max="365" aria-label="Dias de banimento">
                            <button type="submit" class="danger">Banir</button>
                        </form>
                    </v:if>
                </td>
            </tr>
        </v:each>
        </tbody>
    </table>
    </div>
    <p class="hint">Você não pode alterar a sua própria categoria nem banir a si mesmo. Em banimento temporário, informe os dias.</p>
</t:layout>
