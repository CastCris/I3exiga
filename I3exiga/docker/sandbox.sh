#!/bin/bash
# Executa um comando com limites de recursos. Usado pelo avaliador (classe Sandbox).
#
# Uso: sandbox.sh <segundos_de_cpu> <memoria_virtual_kb | 0> <comando> [argumentos...]
#
# O avaliador chama este script já com o usuário sem privilégios ("judge"),
# através do setpriv. Os limites abaixo protegem o servidor de programas defeituosos
# ou maliciosos (laços infinitos, alocação excessiva, saída gigante, fork bomb).

cpu_seconds=$1
memory_kb=$2
shift 2

ulimit -t "$cpu_seconds"            # tempo de CPU
if [ "$memory_kb" -gt 0 ]; then
    ulimit -v "$memory_kb"          # memória virtual (a JVM é limitada com -Xmx)
fi
ulimit -f 2048                      # arquivos (inclui a saída padrão) de até 2 MB
ulimit -u 256                       # processos/threads
ulimit -n 64                        # descritores de arquivo
ulimit -c 0                         # sem core dump

exec "$@"
