// Comportamentos de interface, sem dependências e sem scripts inline (a CSP só permite 'self').
(function () {
    'use strict';

    // ---------- Confirmação em formulários com data-confirm="mensagem" ----------
    document.addEventListener('submit', function (event) {
        var message = event.target.dataset.confirm;
        if (message && !window.confirm(message)) {
            event.preventDefault();
        }
    });

    // ---------- Realce de sintaxe (C, C++, Java e Python) ----------
    var NUMBER = /\b(?:0[xX][0-9a-fA-F]+|\d+\.?\d*(?:[eE][+-]?\d+)?)[uUlLfF]*/;
    var C_COMMENT = /\/\*[\s\S]*?(?:\*\/|(?![\s\S]))|\/\/[^\n]*/;
    var C_STRING = /"(?:\\.|[^"\\\n])*"?|'(?:\\.|[^'\\\n])*'?/;
    var PREPROCESSOR = /^[ \t]*#[ \t]*[A-Za-z_]+[^\n]*/;
    var PY_COMMENT = /#[^\n]*/;
    var PY_STRING = /[rRbBuUfF]{0,2}(?:"""[\s\S]*?(?:"""|(?![\s\S]))|'''[\s\S]*?(?:'''|(?![\s\S]))|"(?:\\.|[^"\\\n])*"?|'(?:\\.|[^'\\\n])*'?)/;
    var IDENTIFIER = /[A-Za-z_$][\w$]*/;

    var C_KEYWORDS = 'auto break case const continue default do else enum extern for goto if inline register restrict return ' +
        'sizeof static struct switch typedef union volatile while';
    var C_TYPES = 'int char short long float double void unsigned signed size_t FILE';
    var CPP_KEYWORDS = C_KEYWORDS + ' class namespace template typename using public private protected virtual new delete try catch ' +
        'throw this operator nullptr true false constexpr';
    var CPP_TYPES = C_TYPES + ' bool string vector map set pair queue stack deque cin cout cerr endl';
    var JAVA_KEYWORDS = 'abstract assert break case catch class continue default do else enum extends final finally for if ' +
        'implements import instanceof interface new package private protected public return static super switch synchronized ' +
        'this throw throws try volatile while true false null var';
    var JAVA_TYPES = 'int long short byte char float double boolean void String Integer Long Double Scanner System List ' +
        'ArrayList Map HashMap Math Object BufferedReader InputStreamReader';
    var PY_KEYWORDS = 'and as assert break class continue def del elif else except finally for from global if import in is ' +
        'lambda nonlocal not or pass raise return try while with yield True False None';
    var PY_TYPES = 'print input len range int str float list dict set tuple map sum min max abs sorted enumerate zip open';

    function wordMap(keywords, types) {
        var words = Object.create(null);
        keywords.split(' ').forEach(function (w) { words[w] = 'kw'; });
        types.split(' ').forEach(function (w) { words[w] = 'type'; });
        return words;
    }

    function language(patterns, keywords, types) {
        var classes = patterns.map(function (p) { return p[0]; });
        var source = patterns.map(function (p) { return '(' + p[1].source + ')'; }).join('|');
        return { classes: classes, regex: new RegExp(source, 'gm'), words: wordMap(keywords, types) };
    }

    var cLike = [['comment', C_COMMENT], ['string', C_STRING], ['number', NUMBER], ['ident', IDENTIFIER]];
    var withPreprocessor = [['comment', C_COMMENT], ['preproc', PREPROCESSOR], ['string', C_STRING], ['number', NUMBER], ['ident', IDENTIFIER]];
    var LANGUAGES = {
        C: language(withPreprocessor, C_KEYWORDS, C_TYPES),
        CPP: language(withPreprocessor, CPP_KEYWORDS, CPP_TYPES),
        JAVA: language(cLike, JAVA_KEYWORDS, JAVA_TYPES),
        PYTHON: language([['comment', PY_COMMENT], ['string', PY_STRING], ['number', NUMBER], ['ident', IDENTIFIER]], PY_KEYWORDS, PY_TYPES)
    };

    function escapeHtml(text) {
        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    /** Devolve o código como HTML seguro, com os trechos envolvidos em <span class="tok-...">. */
    function highlight(code, languageName) {
        var lang = LANGUAGES[languageName];
        if (!lang) {
            return escapeHtml(code);
        }
        var regex = lang.regex;
        regex.lastIndex = 0;
        var html = '';
        var last = 0;
        var match;
        while ((match = regex.exec(code)) !== null) {
            if (match[0] === '') {
                regex.lastIndex++;
                continue;
            }
            html += escapeHtml(code.slice(last, match.index));
            var kind = null;
            for (var i = 0; i < lang.classes.length; i++) {
                if (match[i + 1] !== undefined) {
                    kind = lang.classes[i];
                    break;
                }
            }
            if (kind === 'ident') {
                kind = lang.words[match[0]] || (code.charAt(regex.lastIndex) === '(' ? 'fn' : null);
            }
            html += kind ? '<span class="tok-' + kind + '">' + escapeHtml(match[0]) + '</span>' : escapeHtml(match[0]);
            last = regex.lastIndex;
        }
        return html + escapeHtml(code.slice(last));
    }

    // Código exibido (página da submissão).
    document.querySelectorAll('pre.code-view').forEach(function (pre) {
        var code = pre.querySelector('code');
        code.innerHTML = highlight(code.textContent, pre.dataset.language);
    });

    // ---------- Editor de código com realce, código base por linguagem e Tab/Enter ----------
    var editor = document.querySelector('.code-editor');
    var select = document.getElementById('language');
    if (editor && select) {
        var textarea = editor.querySelector('textarea');
        var layer = editor.querySelector('pre');
        var templates = Array.prototype.map.call(select.options, function (option) { return option.dataset.template; });

        var render = function () {
            layer.firstElementChild.innerHTML = highlight(textarea.value, select.value) + '\n';
            layer.scrollTop = textarea.scrollTop;
            layer.scrollLeft = textarea.scrollLeft;
        };

        // Troca o código base, mas nunca apaga o que o usuário escreveu: só substitui se estiver vazio
        // ou se ainda for o código base de alguma linguagem.
        var applyTemplate = function () {
            var current = textarea.value;
            if (current.trim() === '' || templates.indexOf(current) !== -1) {
                textarea.value = select.selectedOptions[0].dataset.template;
            }
        };

        var insert = function (text) {
            textarea.focus();
            if (!document.execCommand('insertText', false, text)) {
                textarea.setRangeText(text, textarea.selectionStart, textarea.selectionEnd, 'end');
                textarea.dispatchEvent(new Event('input'));
            }
        };

        select.addEventListener('change', function () { applyTemplate(); render(); });
        textarea.addEventListener('input', render);
        textarea.addEventListener('scroll', function () {
            layer.scrollTop = textarea.scrollTop;
            layer.scrollLeft = textarea.scrollLeft;
        });
        textarea.addEventListener('keydown', function (event) {
            if (event.key === 'Tab' && !event.shiftKey) {
                event.preventDefault();
                insert('    ');
            } else if (event.key === 'Enter' && textarea.selectionStart === textarea.selectionEnd) {
                // Mantém a indentação da linha atual e aumenta após "{" ou ":".
                var before = textarea.value.slice(0, textarea.selectionStart);
                var line = before.slice(before.lastIndexOf('\n') + 1);
                var indent = line.match(/^[ \t]*/)[0] + (/[{:]\s*$/.test(line) ? '    ' : '');
                event.preventDefault();
                insert('\n' + indent);
            }
        });

        applyTemplate();
        render();
    }

    // ---------- Caixas de casos de teste: rolagem sincronizada para as linhas ficarem alinhadas ----------
    var synced = document.querySelectorAll('textarea[data-sync]');
    synced.forEach(function (box) {
        box.addEventListener('scroll', function () {
            synced.forEach(function (other) {
                if (other !== box && other.dataset.sync === box.dataset.sync) {
                    other.scrollTop = box.scrollTop;
                }
            });
        });
    });
}());
