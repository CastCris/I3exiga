<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="Entrar">
    <section class="narrow">
        <h1>Entrar</h1>
        <form method="post" action="${ctx}/login" class="stack">
            <t:csrf/>
            <label>Prontuário
                <input name="prontuario" value="${v:h(prontuario)}" required autofocus autocomplete="username">
            </label>
            <label>Senha
                <input type="password" name="password" required autocomplete="current-password">
            </label>
            <button type="submit" class="primary">Entrar</button>
        </form>
        <p>Ainda não tem conta? <a href="${ctx}/register">Cadastre-se</a>.</p>
    </section>
</t:layout>
