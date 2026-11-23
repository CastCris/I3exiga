<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="Cadastro">
    <section class="narrow">
        <h1>Cadastro</h1>
        <form method="post" action="${ctx}/register" class="stack">
            <t:csrf/>
            <label>Prontuário
                <input name="prontuario" value="${v:h(prontuario)}" required autofocus maxlength="20" autocomplete="username">
                <span class="hint">De 3 a 20 letras e números.</span>
            </label>
            <label>Senha
                <input type="password" name="password" required minlength="8" maxlength="128" autocomplete="new-password">
                <span class="hint">Mínimo de 8 caracteres.</span>
            </label>
            <label>Confirme a senha
                <input type="password" name="confirmation" required minlength="8" maxlength="128" autocomplete="new-password">
            </label>
            <button type="submit" class="primary">Cadastrar</button>
        </form>
        <p>Já tem conta? <a href="${ctx}/login">Entrar</a>.</p>
    </section>
</t:layout>
