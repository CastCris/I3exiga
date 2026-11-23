<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="Perfil">
    <section class="narrow">
        <h1>Meu perfil</h1>
        <p class="meta">Prontuário <strong>${v:h(currentUser.prontuario)}</strong> · ${currentUser.role.label}</p>
        <form method="post" action="${ctx}/profile" class="stack">
            <t:csrf/>
            <label>Nome completo
                <input name="fullName" value="${v:h(currentUser.fullName)}" maxlength="120" autocomplete="name">
            </label>
            <label>Curso
                <input name="course" value="${v:h(currentUser.course)}" maxlength="120">
            </label>
            <p class="hint">Nome e curso são opcionais e aparecem na sua página e nos placares.</p>
            <button type="submit" class="primary">Salvar perfil</button>
        </form>
    </section>

    <section class="narrow">
        <h2>Alterar senha</h2>
        <form method="post" action="${ctx}/profile/password" class="stack">
            <t:csrf/>
            <label>Senha atual
                <input type="password" name="current" required autocomplete="current-password">
            </label>
            <label>Nova senha
                <input type="password" name="password" required minlength="8" maxlength="128" autocomplete="new-password">
            </label>
            <label>Confirme a nova senha
                <input type="password" name="confirmation" required minlength="8" maxlength="128" autocomplete="new-password">
            </label>
            <button type="submit">Alterar senha</button>
        </form>
    </section>
</t:layout>
