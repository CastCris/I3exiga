# I3exiga IFSP

Ambiente virtual de programação competitiva, público e gratuito, para a comunidade dos Institutos Federais de São Paulo.
Esta aplicação implementa as funcionalidades descritas no documento **I3exiga IFSP** (Técnico em Desenvolvimento de Sistemas, turma 331)
usando **JSP + Servlets em Java puro**, executada em **Apache Tomcat** dentro do Docker, **sem nenhuma biblioteca ou serviço externo**
(sem Maven, sem JSTL, sem banco de dados externo).

## Como executar

Requisitos: apenas **Docker** com o plugin **Docker Compose**.

```bash
docker compose up --build -d
```

Acesse <http://localhost:8080>.

Na primeira execução é criado o administrador (prontuário `admin`) e são inseridos **dados de demonstração**: 7 problemas
(soma, par ou ímpar, fatorial, maior elemento, palíndromo, Fibonacci e componentes conexos) e 7 artigos em PDF (4 de conhecimento geral
aprovados, 2 de solução aprovados e 1 em avaliação para você testar a fila de avaliação). Use `I3EXIGA_SEED_DEMO=false` para começar vazio.

### Senha do administrador e recuperação de acesso

- **Com `I3EXIGA_ADMIN_PASSWORD` definida** (no `.env`): essa senha vale a cada inicialização. Se o admin já existia com outra senha, ela é
  **redefinida**; se ele foi rebaixado ou banido, o acesso é restaurado. Para recuperar o acesso basta definir a variável e rodar `docker compose up -d`.
- **Sem a variável**: na primeira execução uma senha aleatória é gerada e escrita no log
  (`docker compose logs i3exiga | grep -i "senha gerada"`). Nas execuções seguintes as senhas existentes são mantidas.
- No `.env`, evite `$` na senha (o compose o interpreta); se precisar, escreva `$$`.
- Depois de entrar, qualquer usuário pode trocar a própria senha em **Perfil**.
- Para apagar tudo e recomeçar (inclusive os dados de demonstração): `docker compose down -v`.

Para personalizar porta, administrador e fuso horário, copie `.env.example` para `.env` e edite. Para parar: `docker compose down`
(os dados ficam no volume `i3exiga-data` e sobrevivem à remoção do contêiner).

> **Troque a senha do administrador** (ou defina `I3EXIGA_ADMIN_PASSWORD` antes de subir) e coloque a aplicação atrás de HTTPS
> (proxy reverso) em produção.

### Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `I3EXIGA_ADMIN_USER` | `admin` | Prontuário do administrador inicial (criado só se não houver usuários). |
| `I3EXIGA_ADMIN_PASSWORD` | *(aleatória)* | Senha do administrador inicial. |
| `I3EXIGA_SEED_DEMO` | `true` | Insere problemas e artigos de demonstração quando a plataforma está vazia. |
| `I3EXIGA_TIMEZONE` | `America/Sao_Paulo` | Fuso horário das datas exibidas. |
| `I3EXIGA_PORT` | `8080` | Porta publicada no host (somente no compose). |
| `I3EXIGA_DATA_DIR` | `/data` | Onde ficam o banco de dados e os PDFs. |
| `I3EXIGA_SANDBOX_USER` | `judge` | Usuário do SO que executa o código enviado. `none` desativa a troca de usuário (só para desenvolvimento). |

## Funcionalidades (do documento para a aplicação)

| Requisito do documento | Como foi implementado |
|---|---|
| Cadastro/autenticação só com **prontuário e senha** | `/register` e `/login`. Senhas com PBKDF2-HMAC-SHA256 e sal; bloqueio temporário após 5 falhas seguidas. |
| Três categorias: **administrador, docente, discente** | Todo cadastro é discente. Administradores alteram categorias em **Usuários**. |
| Só administradores e docentes inserem, modificam e removem **problemas e artigos** | Telas de problema e de artigo exibem as ações apenas para a equipe, e os servlets/serviços validam a permissão. Artigos só podem ser alterados por seus autores ou por administradores. |
| Submissão com saída **esperada e recebida** em caso de erro, **no máximo 20 linhas** | Página da submissão mostra o primeiro caso de teste que falhou; todas as saídas e mensagens são cortadas em 20 linhas. |
| Tipos de erro: Execução, Tempo, Memória, Formatação, Saída, Compilação | Veredictos: *Erro Durante a Execução*, *Tempo Excedido*, *Memória Excedida*, *Erro de Formatação*, *Erro de Saída*, *Erro de compilação* (e *Aceito*). Há ainda *Aguardando correção* e *Erro interno do avaliador*. |
| Linguagens **C, C++, Java e Python** para todos os problemas | Compilação/execução reais no contêiner (`gcc`, `g++`, `javac`/`java`, `python3`). |
| Pontos do problema ao resolver; definidos por **administradores** | Cada problema vale seus pontos uma única vez por usuário. Só administradores editam os pontos (problemas criados por docentes começam com 10). |
| **Comentários** em aba específica de cada problema, com regras de conteúdo | Aba *Comentários*. Há uma barreira automática (lista de termos em `src/main/resources/blocked-terms.txt` e detecção de código) e a denúncia pelos usuários. |
| **Denúncia** analisada por equipe de administradores; exclusão ou banimento temporário/indeterminado | Botão *Denunciar* em cada comentário; fila em **Denúncias** com: rejeitar, excluir, excluir e banir por N dias, excluir e banir indeterminadamente. Contas banidas não entram nem continuam logadas. |
| **Artigos**: tipos geral e solução, PDF no **modelo LaTeX** do sistema, avaliados por administradores | Envio em `/articles/new` (PDF até 10 MB), modelo em `/assets/modelo-artigo.tex`. Avaliação em **Avaliações** com checklist dos critérios (gramática, semântica, coesão, modelo LaTeX, termos ofensivos, códigos nas linguagens suportadas, e critérios específicos por tipo). Aprovar exige todos os critérios; rejeitar exige parecer. |
| Artigos de **conhecimento geral** são públicos | Listagem e PDF acessíveis sem login (depois de aprovados). |
| Artigos de **solução** só após resolver o problema, ou **descontando os pontos** | O PDF só abre para quem resolveu o problema (ou é da equipe). Quem aceita abrir antes passa a não pontuar naquele problema. |
| **Filtrar problemas e artigos** por tópico, nome e/ou id | Os problemas e os artigos de conhecimento geral têm até 5 tópicos. As listagens têm filtro por *id*, *nome* (ignora maiúsculas e acentos) e *tópico*; os critérios se combinam e os tópicos exibidos são links de filtro. |
| **Placar geral** (maior → menor, ordem inversa, busca) | `/scoreboards/general`. Empates dividem a posição. |
| Cada linha do placar leva à **página do usuário** (nome, matrícula, pontuação, curso, gráfico de submissões) | `/users?id=…` com gráfico SVG gerado no servidor (últimos 30 dias, aceitas × rejeitadas). |
| **Placares específicos**: usuários selecionados ou abertos ao público | Docentes/administradores criam placares restritos (lista de prontuários) ou públicos (qualquer usuário pode participar/sair). |

### Decisões de interpretação

O documento deixa alguns pontos em aberto. As escolhas feitas, fáceis de mudar no código, foram:

- **Nome completo e curso**: o cadastro pede só prontuário e senha, como no documento. Nome e curso são preenchidos pelo usuário em *Perfil* (opcionais).
- **Descontar pontos**: abrir um artigo de solução sem ter resolvido o problema faz o problema não pontuar para aquele usuário, mesmo que ele o resolva depois.
- **Placar personalizado** ranqueia apenas os participantes escolhidos, usando a pontuação global de cada um.
- **Ids por tipo**: problemas, artigos, usuários etc. têm numeração própria (problema 1, 2, 3...), o que torna o filtro por id útil.
- **Java**: a classe principal deve se chamar `Main`. Em todas as linguagens, leia da entrada padrão e escreva na saída padrão.
- **Comparação de saída**: *Aceito* ignora espaços no fim das linhas e linhas vazias no fim; se só diferir em espaços/quebras de linha é *Erro de Formatação*; qualquer outra diferença é *Erro de Saída*.

## Arquitetura (simples e sem camadas desnecessárias)

```
Navegador ──► SecurityFilter ──► Servlets ──► Services ──► Store (arquivo)
                 (login, CSRF,        │            │
                  cabeçalhos)         │            └──► Judge ──► Sandbox ──► gcc / g++ / javac / python3
                                      └──► JSP (/WEB-INF/jsp) + tags (/WEB-INF/tags)
```

```
Dockerfile, docker-compose.yml, docker/sandbox.sh
src/main/java/br/edu/ifsp/i3exiga/
  model/    entidades (beans simples) e enums (Role, Verdict, Language, ...)
  store/    Store: persistência em arquivo com gravação atômica
  service/  regras de negócio: Users, Problems, Submissions, Scores, Comments, Articles, Scoreboards
  judge/    Judge (compila e roda), Sandbox (isolamento), OutputComparator
  web/      SecurityFilter, BaseServlet e um servlet por área
  web/view/ tags <v:if>, <v:each> e funções EL (substituem a JSTL)
src/main/webapp/
  WEB-INF/jsp/   páginas      WEB-INF/tags/   layout e partes reutilizáveis
  assets/        CSS, JS e modelo LaTeX
```

- **Persistência**: sem banco externo, todos os dados ficam em um arquivo (`/data/i3exiga.db`, serialização Java) gravado de forma atômica a cada
  alteração; os PDFs ficam em `/data/articles`. É adequado para uso de escola/campus. Para escalar, troque apenas a classe `Store`/os serviços por JDBC.
- **Correção assíncrona**: as submissões entram em uma fila e são corrigidas uma por vez; a página da submissão atualiza sozinha. Submissões
  pendentes são retomadas se o servidor reiniciar. Cada usuário pode ter até 3 submissões pendentes.
- **Segurança web**: token CSRF em todo POST, sessão com `HttpOnly` e `SameSite=Lax`, troca do id da sessão no login, escape de HTML em todo texto de
  usuário, `Content-Security-Policy` restritiva (sem scripts ou estilos inline), validação de PDF e de tamanhos.

## Segurança do avaliador (leia antes de publicar)

O código dos usuários é executado **dentro do contêiner**, com estas proteções: usuário sem privilégios (`judge`) sem acesso a `/data`, `/usr/local/tomcat`
ou às variáveis de ambiente do servidor; limites de CPU, memória, tamanho de arquivo, processos e descritores (`docker/sandbox.sh`); tempo limite de relógio
com encerramento da árvore de processos; diretório temporário apagado a cada execução; contêiner com capacidades mínimas, `no-new-privileges`,
limite de memória e de processos.

Limitações conhecidas: o contêiner **tem acesso à rede** (um programa poderia tentar conexões de saída) e o isolamento é por processo/usuário, não por
máquina virtual. Detecção de *Memória Excedida* é heurística (mensagens de falta de memória). Para uso aberto na internet, rode a aplicação em uma
máquina/rede isolada, restrinja o tráfego de saída do contêiner no firewall do host ou use um executor dedicado (gVisor, Firecracker, etc.).

## Backup e restauração

```bash
docker volume ls | grep i3exiga-data                      # descubra o nome real do volume
docker run --rm -v <volume>:/data -v "$PWD":/backup ubuntu tar czf /backup/i3exiga-backup.tgz -C /data .
docker run --rm -v <volume>:/data -v "$PWD":/backup ubuntu tar xzf /backup/i3exiga-backup.tgz -C /data   # restaurar (com a aplicação parada)
```

## Desenvolvimento sem Docker

Precisa de JDK 17+, Tomcat 10.1, `gcc`, `g++` e `python3`. Compile com
`javac -cp "$TOMCAT/lib/servlet-api.jar:$TOMCAT/lib/jsp-api.jar:$TOMCAT/lib/el-api.jar" -d src/main/webapp/WEB-INF/classes $(find src/main/java -name '*.java')`,
copie `src/main/resources/*` para `WEB-INF/classes` e a pasta `src/main/webapp` para `webapps/ROOT`. Defina
`I3EXIGA_SANDBOX_USER=none`, `I3EXIGA_SANDBOX_SCRIPT=$PWD/docker/sandbox.sh`, `I3EXIGA_DATA_DIR` e `I3EXIGA_JUDGE_DIR`.

## Próximos passos sugeridos

Competições com janela de tempo e conjunto de problemas próprios; JDBC/PostgreSQL no lugar do arquivo; integração de login com o SUAP/prontuário institucional;
executor de código isolado em contêiner/VM separado; testes automatizados dos servlets.
