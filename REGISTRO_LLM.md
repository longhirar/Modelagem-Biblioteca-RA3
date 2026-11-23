# Registro de uso de LLM

Registro das interações com ferramentas de IA generativa ao longo do desenvolvimento do Trabalho 3 (Sistema de Biblioteca com padrões GoF — PUCPR, Modelagem de Sistemas Computacionais, 2026/2).

## Ferramentas utilizadas

| Ferramenta | Modelo / Provedor | Papel principal no projeto |
|---|---|---|
| **Claude Code** (extensão VS Code / CLI) | Claude Opus 5.5 (Anthropic) | Apoio na estruturação inicial do projeto Maven, rascunho dos 10 padrões GoF, camada de persistência, testes JUnit (`R01`–`R10`), CLI e documentação. |
| **Cursor CLI** | Grok 4.6 High (SpaceXAI) | Consulta teórica sobre aplicação dos padrões, revisão de aderência entre os diagramas UML e o código implementado, e checagem final de regras de negócio. |

> Todo o código gerado foi revisado, testado e ajustado pela equipe antes de ser consolidado no repositório.

---

## Histórico de solicitações e decisões da equipe

| # | Data | Ferramenta | Pedido feito ao modelo (resumo) | O que o modelo produziu | Decisão da equipe | O que foi ajustado, corrigido ou descartado e por quê |
|---|---|---|---|---|---|---|
| 1 | 01/11/2026 | Claude Code | Criar estrutura inicial do projeto Java 17 com Maven Wrapper, arquivo de propriedades e script SQL de tabelas. | `pom.xml`, `mvnw`, `mvnw.cmd`, `resources/biblioteca.properties` e `resources/schema.sql`. | **Aceito com ajustes** | Simplificamos a estrutura de pastas para `src/`, `test/` e `resources/` no `pom.xml` (sem hierarquia profunda de pacotes) e configuramos o H2 em modo MySQL apenas no escopo de teste. |
| 2 | 03/11/2026 | Claude Code | Gerar entidades base do domínio (`Pessoa`, `Estudante`, `Atendente`, `Transacao`, `Reserva`, `Emprestimo`, `Devolucao`, `Multa`), regras e relógio simulado. | Classes em `src/dominio/`, `src/dominio/pessoa/`, `src/dominio/movimentacao/` e `src/config/Relogio.java`. | **Corrigido** | Unificamos os identificadores de livro e estudante como `String` (o Trabalho 02 misturava `int`, `long` e `String`) e padronizamos todos os valores monetários em `BigDecimal` com 2 casas decimais. |
| 3 | 04/11/2026 | Claude Code | Implementar o padrão **State** (R01) para o ciclo de vida do livro a partir do diagrama de estados. | `Livro`, `EstadoLivro` e os 7 estados (`Livre`, `Reservado`, `Emprestado`, `Renovado`, `Devolvido`, `Reparando`, `Incinerado`). | **Corrigido** | Corrigimos a guarda de renovação para `numRenovacoes < 2`: o diagrama do T02 trazia `== 0` e `<= 2`, o que causava inconsistência no limite máximo de 2 renovações. |
| 4 | 06/11/2026 | Claude Code | Estruturar camada de acesso (`acesso/`) e o padrão **Abstract Factory** (R08) para persistência em memória e JDBC/MySQL. | `FabricaPersistencia`, repositórios em `persistencia/memoria/` e `persistencia/jdbc/`, além de `ExecutorTransacional`. | **Aceito com ajustes** | Adicionamos validação explícita na `FabricaPersistenciaJdbc` para recusar URLs Oracle (por ser apenas réplica de leitura no cenário) e revisamos o fechamento de transações na `UnidadeDeTrabalhoJdbc`. |
| 5 | 08/11/2026 | Claude Code | Implementar o fluxo de cadastro com **Template Method** (R07) para livros, estudantes e atendentes. | `CadastroTemplate`, `CadastroLivro`, `CadastroPessoa`, `CadastroEstudante`, `CadastroAtendente` e validação de `Cpf`. | **Aceito** | Mantivemos o algoritmo fixo na classe abstrata e isolamos validações específicas (CPF, duplicidade e campos obrigatórios) nas subclasses concretas. |
| 6 | 10/11/2026 | Claude Code | Implementar políticas de multa com **Strategy** (R02) e integração bancária com **Adapter** (R03). | `PoliticaMulta` e estratégias em `financeiro/politica/`; simuladores `BancoAApi`/`BancoBApi` em `externo/` e adaptadores em `financeiro/cobranca/`. | **Corrigido** | O rascunho inicial usava `double` para valores no `BancoAApi` e não tratava notificações duplicadas. Ajustamos o `BancoAApi` para centavos (`long`), o `BancoBApi` para `BigDecimal`, e garantimos idempotência na quitação de boletos. |
| 7 | 12/11/2026 | Claude Code | Implementar **Observer** (R05) na devolução, **Facade** (R04) na circulação e **Command** (R06) para solicitações. | `PublicadorDevolucao`, ouvintes em `devolucao/ouvintes/`, `FachadaCirculacao` e `ExecutorDeSolicitacoes`. | **Corrigido** | Movemos a geração de multa para o `OuvinteMulta` (em vez de disparar dentro da transição de estado do livro) e simplificamos o `ExecutorDeSolicitacoes` com `synchronized` + `HashMap` em memória para evitar reexecução pelo mesmo `id`. |
| 8 | 13/11/2026 | Claude Code | Implementar **Decorator** (R09) na consulta ao acervo, **Composite** (R10) na árvore organizacional, tratadores e menu CLI. | Pacotes `consulta/`, `organizacao/`, `tratadores/`, `Aplicacao.java` e `ui/Main.java`. | **Aceito com ajustes** | Garantimos que `ui.Main` só invoca os tratadores (`IEmprestimo`, `IDevolucao`, `ILivro`, etc.), sem acoplamento direto com domínio ou repositórios, e incluímos o fluxo completo na opção 20 do menu. |
| 9 | 16/11/2026 | Claude Code | Gerar suíte de testes JUnit 5 cobrindo todos os 10 requisitos (`R01` a `R10`). | `test/Cenario.java` e classes `R01_StateTest` até `R10_CompositeTest`. | **Corrigido** | Removemos o uso de reflexão que o modelo havia gerado no `R04_FacadeTest` e ajustamos os testes para validar comportamento público via fachada e tratadores. |
| 10 | 19/11/2026 | Cursor CLI | Revisar o código completo contra os requisitos do enunciado e diagramas do Trabalho 02, apontando lacunas. | Lista de divergências entre requisitos e código; identificou ausência de validação para reserva vencida no empréstimo (R04). | **Corrigido / Parcialmente descartado** | Implementamos reserva válida por 7 dias, prorrogação única de +7 dias antes do vencimento, e recusa automática com liberação do livro para `LIVRE` quando vencida (`ReservaVencidaException` + 4 novos testes em `R01_StateTest`). **Descartamos** a sugestão de mudar renovação de empréstimo para +7 dias / 1x por contrariar R01 e R06. |
| 11 | 20/11/2026 | Claude Code | Diagnosticar falhas encontradas ao rodar o sistema com MySQL 8.4 (`-Dperfil=producao`). | Correção de dois bugs específicos de ambiente relacional e atualização do `schema.sql`. | **Corrigido** | Corrigimos: (1) colisão no número de boleto gerado pelo `BancoAApi` simulado; (2) empate na consulta de "última devolução/multa" no mesmo dia, resolvido com a coluna `seq` no banco. |
| 12 | 21/11/2026 | Cursor CLI | Fazer revisão normativa final entre os diagramas Astah (`Trabalho 03 - MSC - Equipe 12.asta`) e o código, além de revisar o `README.md`. | Conferência de nomes de classes/métodos entre UML e `src/`, e revisão da seção de premissas e limitações no `README.md`. | **Aceito com ajustes** | Alinhamos a nomenclatura nos diagramas da equipe e consolidamos o roteiro de demonstração e as limitações conhecidas no `README.md`. |
| 13 | 23/11/2026 | Cursor CLI | Remover referências a arquivos inexistentes (`docs/` e `O_QUE_FALTA.md`) do `README.md`, e consolidar o `REGISTRO_LLM.md`| Atualização do `README.md` e reescrita completa do `REGISTRO_LLM.md` | **Aceito** | Revisamos o conteúdo final do `README.md` e deste registro para refletir apenas os artefatos presentes no repositório e todas as ferramentas usadas pela equipe. |

---

## Decisões de projeto validadas pela equipe

* **Prazos e limites de circulação (`RegrasCirculacao`):** empréstimo inicial de 14 dias; renovação de +30 dias com limite de 2 renovações (`numRenovacoes < 2`); reserva válida por 7 dias com no máximo 1 prorrogação de +7 dias solicitada antes do vencimento.
* **Cálculo de atraso e multa:** dias de atraso calculados como dias corridos entre a data prevista e a devolução (mínimo 0); multa gerada exclusivamente pelo `OuvinteMulta` (Observer) e nunca dentro da transição de estado.
* **Precisão monetária e identificadores:** todo valor financeiro utiliza `BigDecimal` com 2 casas decimais; identificadores de livro, pessoa e solicitação padronizados como `String`.
* **Simulação externa e testes:** bancos A e B simulados em `externo/` com contratos distintos (centavos vs. decimal) para exercitar o `Adapter`; testes JDBC utilizam H2 em modo MySQL para não exigir servidor externo durante `mvn test`.
* **Estrutura do projeto:** pastas `src/`, `test/` e `resources/` configuradas diretamente no `pom.xml` para manter a navegação simples durante a apresentação.

---

## Sugestões descartadas

1. **Reduzir a renovação de empréstimo para +7 dias e limite de 1 vez:** descartado por contrariar diretamente os requisitos R01 e R06 do enunciado.
2. **Testar estado interno da fachada via reflexão em `R04_FacadeTest`:** descartado por tornar o teste frágil à refatoração; substituído por asserções sobre os retornos e efeitos observáveis.
3. **Emitir multa dentro do método `devolver()` do estado `Emprestado`:** descartado para preservar a separação de responsabilidades do padrão Observer (R05).
4. **Permitir escrita no banco Oracle quando configurado no `biblioteca.properties`:** descartado porque, pelo cenário do trabalho, o Oracle atua apenas como réplica do MySQL.

---

## Limitações conhecidas aceitas

* O registro de solicitações já executadas no padrão **Command** (`ExecutorDeSolicitacoes`) é mantido em memória; em um reinício no perfil MySQL, um `id` antigo poderia ser aceito novamente.
* A árvore organizacional do **Composite** (`GrupoAcervo`) e a `FilaDeInspecao` ficam apenas em memória; no perfil MySQL, após reiniciar a aplicação, a listagem da fila aparece vazia mesmo com livros no estado `DEVOLVIDO` (embora a inspeção em si continue funcionando pelo estado persistido do livro).
* O cache de metadados do **Decorator** (`ConsultaComCache`) não é invalidado automaticamente em `atualizarLivro`, podendo exibir título desatualizado sem afetar operações de reserva ou empréstimo.
* A proteção contra notificação duplicada de pagamento no MySQL poderia ser reforçada no SQL com `UPDATE ... WHERE situacao = 'EMITIDA'` para cenários de concorrência simultânea.
