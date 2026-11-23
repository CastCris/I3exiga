<%@ tag description="Estrutura comum das páginas" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="autoRefresh" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="v" uri="i3exiga/view" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <v:if test="${autoRefresh}"><meta http-equiv="refresh" content="2"></v:if>
    <title>${v:h(title)} · I3exiga IFSP</title>
    <link rel="stylesheet" href="${ctx}/assets/style.css">
    <script src="${ctx}/assets/app.js" defer></script>
</head>
<body>
<header class="topbar">
    <div class="topbar-inner">
        <a class="brand" href="${ctx}/problems">I3exiga <span>IFSP</span></a>
        <nav aria-label="Principal">
            <v:if test="${not empty currentUser}">
                <a href="${ctx}/problems">Problemas</a>
                <a href="${ctx}/scoreboards/general">Placar</a>
                <a href="${ctx}/scoreboards">Placares</a>
            </v:if>
            <a href="${ctx}/articles">Artigos</a>
            <v:if test="${currentUser.admin}">
                <a href="${ctx}/admin/articles">Avaliações</a>
                <a href="${ctx}/admin/reports">Denúncias</a>
                <a href="${ctx}/admin/users">Usuários</a>
            </v:if>
        </nav>
        <div class="session">
            <v:if test="${not empty currentUser}">
                <a href="${ctx}/users?id=${currentUser.id}">${v:h(currentUser.displayName)}</a>
                <a href="${ctx}/profile">Perfil</a>
                <form method="post" action="${ctx}/logout" class="inline">
                    <t:csrf/>
                    <button type="submit" class="link">Sair</button>
                </form>
            </v:if>
            <v:if test="${empty currentUser}">
                <a href="${ctx}/login">Entrar</a>
                <a href="${ctx}/register">Cadastrar</a>
            </v:if>
        </div>
    </div>
</header>
<main>
    <v:if test="${not empty flash}"><div class="notice ok" role="status">${v:h(flash)}</div></v:if>
    <v:if test="${not empty flashError}"><div class="notice err" role="alert">${v:h(flashError)}</div></v:if>
    <v:if test="${not empty error}"><div class="notice err" role="alert">${v:h(error)}</div></v:if>
    <jsp:doBody/>
</main>
<footer>I3exiga IFSP · programação competitiva aberta à comunidade dos Institutos Federais de São Paulo</footer>
</body>
</html>
