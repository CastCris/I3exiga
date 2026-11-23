package br.edu.ifsp.i3exiga.service;

import br.edu.ifsp.i3exiga.model.Article;
import br.edu.ifsp.i3exiga.model.ArticleType;
import br.edu.ifsp.i3exiga.model.Problem;
import br.edu.ifsp.i3exiga.model.TestCase;
import br.edu.ifsp.i3exiga.model.User;
import br.edu.ifsp.i3exiga.store.Data;
import br.edu.ifsp.i3exiga.util.SimplePdf;
import br.edu.ifsp.i3exiga.util.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * Problemas e artigos de demonstração, inseridos uma única vez quando a plataforma está vazia.
 * Tudo passa pelos serviços normais, então segue as mesmas validações do uso real.
 */
public final class DemoData {

    /** Soluções de referência (também publicadas nos artigos de solução). */
    public static final String FIBONACCI_PYTHON = """
            n = int(input())
            a, b = 0, 1
            for _ in range(n):
                a, b = b, a + b
            print(a)
            """;

    public static final String FIBONACCI_C = """
            #include <stdio.h>

            int main(void) {
                int n;
                scanf("%d", &n);
                long long a = 0, b = 1;
                for (int i = 0; i < n; i++) {
                    long long next = a + b;
                    a = b;
                    b = next;
                }
                printf("%lld\\n", a);
                return 0;
            }
            """;

    public static final String COMPONENTS_PYTHON = """
            import sys

            def main():
                data = sys.stdin.read().split()
                n, m = int(data[0]), int(data[1])
                parent = list(range(n + 1))

                def find(x):
                    while parent[x] != x:
                        parent[x] = parent[parent[x]]
                        x = parent[x]
                    return x

                components = n
                for i in range(m):
                    a = find(int(data[2 + 2 * i]))
                    b = find(int(data[3 + 2 * i]))
                    if a != b:
                        parent[a] = b
                        components -= 1
                print(components)

            main()
            """;

    private DemoData() {
    }

    /** Insere os dados de demonstração se a plataforma ainda não tem problemas nem artigos. */
    public static void seed(Services services, User admin) {
        Data data = services.store.data();
        if (data.demoSeeded || !data.problems.isEmpty() || !data.articles.isEmpty()) {
            return;
        }
        seedProblems(services, admin);
        Problem fibonacci = findByTitle(services, "Fibonacci");
        Problem components = findByTitle(services, "Componentes conexos");
        seedArticles(services, admin, fibonacci, components);
        services.store.transaction(() -> data.demoSeeded = true);
    }

    private static void seedProblems(Services services, User admin) {
        List<Problem> problems = List.of(
            problem("Soma de dois números", "matemática, iniciante", 10, """
                    Leia dois números inteiros A e B e escreva a soma deles.

                    Entrada
                    Uma linha com dois inteiros A e B (-10^9 <= A, B <= 10^9), separados por espaço.

                    Saída
                    Um inteiro: o valor de A + B.""", "3 4\n", "7\n",
                    "1 2\n", "3\n", "10 20\n", "30\n", "-5 8\n", "3\n", "1000000000 1000000000\n", "2000000000\n"),
            problem("Par ou ímpar", "matemática, iniciante", 10, """
                    Dado um número inteiro N, diga se ele é par ou ímpar.

                    Entrada
                    Um inteiro N (0 <= N <= 10^9).

                    Saída
                    Escreva PAR se N for par, ou IMPAR se N for ímpar (sem acento).""", "7\n", "IMPAR\n",
                    "4\n", "PAR\n", "7\n", "IMPAR\n", "0\n", "PAR\n", "1000000001\n", "IMPAR\n"),
            problem("Fatorial", "matemática, recursão", 20, """
                    O fatorial de N, escrito N!, é o produto dos inteiros de 1 até N, e 0! vale 1.
                    Calcule N!.

                    Entrada
                    Um inteiro N (0 <= N <= 12).

                    Saída
                    O valor de N!.""", "5\n", "120\n",
                    "0\n", "1\n", "5\n", "120\n", "10\n", "3628800\n", "12\n", "479001600\n"),
            problem("Maior elemento", "vetores, iniciante", 20, """
                    Dada uma lista de números inteiros, encontre o maior deles.

                    Entrada
                    A primeira linha contém N (1 <= N <= 1000). A segunda linha contém N inteiros
                    separados por espaço, todos com valor absoluto de até 10^9.

                    Saída
                    O maior valor da lista.""", "5\n3 9 2 7 1\n", "9\n",
                    "5\n3 9 2 7 1\n", "9\n", "1\n-4\n", "-4\n", "4\n-1 -2 -3 -4\n", "-1\n", "6\n10 10 10 10 10 11\n", "11\n"),
            problem("Palíndromo", "strings", 20, """
                    Uma palavra é um palíndromo quando se lê igual da esquerda para a direita e da direita para a esquerda.

                    Entrada
                    Uma palavra com até 100 letras minúsculas, sem acentos.

                    Saída
                    Escreva SIM se a palavra for um palíndromo, ou NAO caso contrário.""", "arara\n", "SIM\n",
                    "arara\n", "SIM\n", "banana\n", "NAO\n", "a\n", "SIM\n", "abccba\n", "SIM\n", "ab\n", "NAO\n"),
            problem("Fibonacci", "programação dinâmica, matemática", 30, """
                    A sequência de Fibonacci começa com F(0) = 0 e F(1) = 1, e cada termo seguinte é a soma
                    dos dois anteriores: 0, 1, 1, 2, 3, 5, 8, ...

                    Entrada
                    Um inteiro N (0 <= N <= 45).

                    Saída
                    O valor de F(N).""", "10\n", "55\n",
                    "0\n", "0\n", "1\n", "1\n", "10\n", "55\n", "30\n", "832040\n", "45\n", "1134903170\n"),
            problem("Componentes conexos", "grafos", 50, """
                    Um grafo não direcionado tem N vértices (numerados de 1 a N) e M arestas. Um componente conexo é um
                    grupo de vértices em que é possível chegar de qualquer um a qualquer outro seguindo arestas.
                    Conte quantos componentes conexos o grafo possui.

                    Entrada
                    A primeira linha contém N e M (1 <= N <= 1000, 0 <= M <= 5000). Cada uma das M linhas seguintes
                    contém dois inteiros U e V, indicando uma aresta entre os vértices U e V.

                    Saída
                    O número de componentes conexos.""", "5 3\n1 2\n2 3\n4 5\n", "2\n",
                    "5 3\n1 2\n2 3\n4 5\n", "2\n", "4 0\n", "4\n", "3 3\n1 2\n2 3\n1 3\n", "1\n",
                    "6 4\n1 2\n3 4\n5 6\n2 3\n", "2\n", "1 0\n", "1\n"));
        problems.forEach(problem -> services.problems.save(problem, admin));
    }

    private static void seedArticles(Services services, User admin, Problem fibonacci, Problem components) {
        publish(services, admin, "Introdução a grafos", ArticleType.GENERAL, null, "grafos", new SimplePdf()
                .title("Introdução a grafos")
                .paragraph("Um grafo é um conjunto de vértices ligados por arestas. Ele modela situações do dia a dia: cidades e estradas, "
                        + "pessoas e amizades, páginas e links. Quando as arestas têm sentido o grafo é direcionado; caso contrário, "
                        + "não direcionado.")
                .heading("Como representar")
                .paragraph("A lista de adjacência guarda, para cada vértice, a lista de vizinhos. Ocupa espaço proporcional ao número de "
                        + "vértices mais arestas e é a escolha usual. A matriz de adjacência usa uma tabela N x N e permite saber em "
                        + "tempo constante se existe uma aresta, mas gasta muito espaço em grafos grandes.")
                .code("# lista de adjacência em Python\ngrafo = {1: [2, 3], 2: [1], 3: [1, 4], 4: [3]}")
                .heading("Percorrendo um grafo")
                .paragraph("A busca em profundidade (DFS) segue um caminho até não haver mais vizinhos novos e então volta. "
                        + "A busca em largura (BFS) visita primeiro todos os vizinhos, depois os vizinhos dos vizinhos, e por isso "
                        + "encontra o menor número de arestas entre dois vértices em grafos sem pesos.")
                .code("def dfs(grafo, v, visitados):\n    visitados.add(v)\n    print(v)\n"
                        + "    for w in grafo[v]:\n        if w not in visitados:\n            dfs(grafo, w, visitados)")
                .paragraph("Marque sempre os vértices já visitados: sem isso o percurso pode girar para sempre em grafos com ciclos.")
                .build(), true);

        publish(services, admin, "Programação dinâmica: a ideia por trás da tabela", ArticleType.GENERAL, null,
                "programação dinâmica", new SimplePdf()
                .title("Programação dinâmica: a ideia por trás da tabela")
                .paragraph("Programação dinâmica resolve um problema grande combinando respostas de subproblemas menores, guardando cada "
                        + "resposta para não calculá-la duas vezes. Ela funciona quando o problema tem subestrutura ótima (a melhor "
                        + "resposta é montada a partir das melhores respostas menores) e subproblemas que se repetem.")
                .heading("Memoização e tabulação")
                .paragraph("Na memoização escrevemos a recursão natural e guardamos os resultados já calculados. Na tabulação "
                        + "preenchemos uma tabela da menor entrada para a maior, sem recursão. As duas têm a mesma complexidade; "
                        + "a tabulação costuma ser mais rápida e evita estouro de pilha.")
                .heading("Exemplo: troco mínimo")
                .paragraph("Dadas moedas de 1, 4 e 5 centavos, qual o menor número de moedas para formar 8 centavos? "
                        + "Seja melhor[v] o menor número de moedas para o valor v. Então melhor[0] = 0 e, para v > 0, "
                        + "melhor[v] é 1 mais o menor melhor[v - moeda] entre as moedas que cabem em v.")
                .code("moedas = [1, 4, 5]\nmelhor = [0] + [float('inf')] * 8\nfor v in range(1, 9):\n"
                        + "    for m in moedas:\n        if m <= v:\n            melhor[v] = min(melhor[v], melhor[v - m] + 1)\nprint(melhor[8])  # 2 (4 + 4)")
                .paragraph("Repare que o algoritmo guloso (sempre a maior moeda) daria 5 + 1 + 1 + 1 = 4 moedas: a tabela encontra a "
                        + "resposta correta.")
                .build(), true);

        publish(services, admin, "Operações binárias (bit a bit)", ArticleType.GENERAL, null, "bits, matemática", new SimplePdf()
                .title("Operações binárias (bit a bit)")
                .paragraph("Computadores guardam inteiros em binário. Os operadores bit a bit atuam em cada posição separadamente: "
                        + "AND (&) vale 1 se os dois bits valem 1; OR (|) vale 1 se algum vale 1; XOR (^) vale 1 se forem diferentes; "
                        + "NOT (~) inverte todos os bits.")
                .heading("Deslocamentos")
                .paragraph("Deslocar à esquerda (<<) k posições multiplica por 2 elevado a k; deslocar à direita (>>) divide, descartando "
                        + "o resto. Por isso 1 << k é o número com apenas o bit k ligado.")
                .heading("Truques úteis")
                .code("n & (1 << k)      # o bit k de n está ligado?\nn | (1 << k)      # liga o bit k\n"
                        + "n & ~(1 << k)     # desliga o bit k\nn ^ (1 << k)      # inverte o bit k\n"
                        + "n & (n - 1) == 0 # n é potência de 2 (para n > 0)")
                .paragraph("Subconjuntos de um conjunto com poucos elementos podem ser representados por máscaras de bits: "
                        + "cada bit indica se o elemento está no subconjunto.")
                .build(), true);

        publish(services, admin, "Como usar o I3exiga", ArticleType.GENERAL, null, "plataforma, iniciante", new SimplePdf()
                .title("Como usar o I3exiga")
                .heading("1. Entrar")
                .paragraph("Cadastre-se apenas com prontuário e senha. Depois, em Perfil, você pode informar nome completo e curso.")
                .heading("2. Resolver um problema")
                .paragraph("Abra um problema, escolha a linguagem (C, C++, Java ou Python) e cole o código. Leia os dados da entrada "
                        + "padrão e escreva o resultado na saída padrão. Em Java, a classe principal deve se chamar Main.")
                .heading("3. Entender o resultado")
                .paragraph("Aceito: todos os casos passaram. Erro de compilação: o compilador reclamou do código. Erro Durante a "
                        + "Execução: o programa terminou com falha. Tempo Excedido e Memória Excedida: o programa passou do limite "
                        + "do problema. Erro de Formatação: a resposta está certa, mas com espaços ou quebras de linha diferentes. "
                        + "Erro de Saída: a resposta está errada. Em caso de erro você vê a saída esperada e a recebida (até 20 linhas).")
                .heading("4. Pontos e artigos de solução")
                .paragraph("Cada problema resolvido vale seus pontos uma única vez. Os artigos de solução abrem depois que você resolve o "
                        + "problema; se abri-los antes, o problema deixa de pontuar para você.")
                .heading("5. Comentários")
                .paragraph("Cada problema tem uma aba de comentários. Não use termos ofensivos, mantenha o assunto na solução do problema "
                        + "e não publique código com a resposta. Comentários fora das regras podem ser denunciados.")
                .build(), true);

        publish(services, admin, "Solução: Fibonacci com programação dinâmica", ArticleType.SOLUTION, fibonacci.getId(),
                "programação dinâmica", new SimplePdf()
                .title("Solução: Fibonacci com programação dinâmica")
                .heading("Ideia")
                .paragraph("Cada termo depende só dos dois anteriores. Em vez de recalcular F(N - 1) e F(N - 2) recursivamente (o que "
                        + "repete um número exponencial de chamadas), guardamos apenas os dois últimos valores e avançamos N vezes: "
                        + "tempo O(N) e memória O(1).")
                .paragraph("Para N até 45 o resultado cabe em 32 bits sem sinal, mas usamos inteiro de 64 bits em C por segurança.")
                .heading("Solução em Python")
                .code(FIBONACCI_PYTHON.stripTrailing())
                .heading("Solução em C")
                .code(FIBONACCI_C.stripTrailing())
                .build(), true);

        publish(services, admin, "Solução: componentes conexos com union-find", ArticleType.SOLUTION, components.getId(), "grafos",
                new SimplePdf()
                .title("Solução: componentes conexos com union-find")
                .heading("Ideia")
                .paragraph("Começamos com N componentes, um por vértice. Para cada aresta, descobrimos o representante (a raiz) de cada "
                        + "ponta. Se as raízes são diferentes, as duas pontas estavam em componentes distintos: unimos os dois e "
                        + "diminuímos a contagem em um. Ao final, a contagem é o número de componentes.")
                .paragraph("A estrutura union-find com compressão de caminho faz cada operação em tempo quase constante, então o total "
                        + "é aproximadamente O(N + M).")
                .heading("Solução em Python")
                .code(COMPONENTS_PYTHON.stripTrailing())
                .build(), true);

        publish(services, admin, "Busca em largura (BFS) passo a passo", ArticleType.GENERAL, null, "grafos", new SimplePdf()
                .title("Busca em largura (BFS) passo a passo")
                .paragraph("Rascunho enviado para demonstrar a fila de avaliação dos administradores. "
                        + "A BFS usa uma fila: tire um vértice, visite os vizinhos ainda não vistos e coloque-os no fim da fila.")
                .build(), false);
    }

    private static void publish(Services services, User admin, String title, ArticleType type, Long problemId,
                                String topics, byte[] pdf, boolean approve) {
        Article draft = new Article();
        draft.setTitle(title);
        draft.setType(type);
        draft.setProblemId(problemId);
        draft.setTopics(Text.splitTopics(topics));
        Article saved = services.articles.save(draft, List.of(admin), admin, pdf);
        if (approve) {
            int criteria = services.articles.criteria().get(type).size();
            services.articles.review(admin, saved.getId(), true, criteria, "");
        }
    }

    private static Problem findByTitle(Services services, String title) {
        return services.problems.all().stream().filter(p -> p.getTitle().equals(title)).findFirst().orElseThrow();
    }

    private static Problem problem(String title, String topics, int points, String statement, String exampleInput,
                                   String exampleOutput, String... tests) {
        Problem problem = new Problem();
        problem.setTitle(title);
        problem.setTopics(Text.splitTopics(topics));
        problem.setPoints(points);
        problem.setStatement(statement);
        problem.setExampleInput(exampleInput);
        problem.setExampleOutput(exampleOutput);
        List<TestCase> cases = new ArrayList<>();
        for (int i = 0; i < tests.length; i += 2) {
            cases.add(new TestCase(tests[i], tests[i + 1]));
        }
        problem.setTestCases(cases);
        return problem;
    }
}
