<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<t:layout title="Erro">
    <h1>Não foi possível concluir</h1>
    <p>${empty message ? 'Ocorreu um erro inesperado. Tente novamente.' : v:h(message)}</p>
    <p><a href="${ctx}/problems">Voltar para os problemas</a></p>
</t:layout>
