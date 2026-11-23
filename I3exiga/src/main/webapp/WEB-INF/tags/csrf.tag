<%@ tag description="Campo oculto com o token CSRF (obrigatório em todo formulário POST)" pageEncoding="UTF-8" body-content="empty" %>
<input type="hidden" name="_csrf" value="${sessionScope.csrf}">
