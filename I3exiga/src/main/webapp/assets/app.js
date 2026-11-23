// Comportamentos mínimos de interface. Sem dependências e sem scripts inline (a CSP só permite 'self').
(function () {
    // Pede confirmação em formulários com data-confirm="mensagem".
    document.addEventListener('submit', function (event) {
        var message = event.target.dataset.confirm;
        if (message && !window.confirm(message)) {
            event.preventDefault();
        }
    });

    // Casos de teste do formulário de problema: adicionar e remover linhas.
    var list = document.getElementById('tests');
    var add = document.getElementById('add-test');
    if (list && add) {
        add.addEventListener('click', function () {
            var rows = list.querySelectorAll('.test-row');
            var copy = rows[rows.length - 1].cloneNode(true);
            copy.querySelectorAll('textarea').forEach(function (field) { field.value = ''; });
            list.appendChild(copy);
        });
        list.addEventListener('click', function (event) {
            if (!event.target.classList.contains('remove-test')) {
                return;
            }
            var rows = list.querySelectorAll('.test-row');
            if (rows.length > 1) {
                event.target.closest('.test-row').remove();
            }
        });
    }
}());
