<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="${empty board.id ? 'Novo placar' : 'Editar placar'}">
    <h1>${empty board.id ? 'Novo placar' : 'Editar placar'}</h1>
    <form method="post" action="${ctx}/scoreboards/save" class="stack">
        <t:csrf/>
        <v:if test="${not empty board.id}"><input type="hidden" name="id" value="${board.id}"></v:if>
        <label>Nome
            <input name="name" value="${v:h(board.name)}" required minlength="3" maxlength="80">
        </label>
        <label>Descrição
            <textarea name="description" rows="3" maxlength="500">${v:h(board.description)}</textarea>
        </label>
        <label class="check">
            <input type="checkbox" name="publicAccess" ${board.publicAccess ? 'checked' : ''}>
            Aberto ao público (qualquer usuário pode ver e participar)
        </label>
        <label>Participantes (prontuários)
            <textarea name="members" rows="6">${v:h(members)}</textarea>
            <span class="hint">Um por linha. Em placares públicos, outros usuários também podem entrar sozinhos.</span>
        </label>
        <div class="actions">
            <button type="submit" class="primary">Salvar placar</button>
            <a href="${ctx}/scoreboards">Cancelar</a>
        </div>
    </form>
</t:layout>
