# Trabalho 3 - Biblioteca com padrões GoF

PUCPR - Modelagem de Sistemas Computacionais - Prof. Edson Emílio Scalabrin - 2º semestre/2026

**Equipe:**
- Pedro da Fonseca Schwertner
- Sophia Oelke Verdi
- Maria Paula de Jesus Santos
- Rafael Longhi Varella Guimarães

Continuação do Trabalho 02: implementamos o sistema de cadastro, reserva, empréstimo, renovação e
devolução de livros usando os 10 padrões pedidos (State, Strategy, Adapter, Facade, Observer,
Command, Template Method, Abstract Factory, Decorator e Composite).

## Como rodar

Precisa só do **JDK 17** (ou mais novo). Tem que ser JDK, não JRE: com só o JRE (ou o Java 8) o
Maven falha com "No compiler is provided in this environment". No Windows dá pra instalar com
`winget install EclipseAdoptium.Temurin.17.JDK` e abrir um terminal novo (confira com `java -version`).
O Maven não precisa estar instalado: o projeto tem o Maven Wrapper (`mvnw.cmd` no Windows, `./mvnw`
no Linux/Mac), que baixa o Maven sozinho na primeira vez.

| O que | Comando (Windows) |
|---|---|
| Compilar e rodar os testes | `mvnw.cmd test` |
| Rodar só os testes de um requisito | `mvnw.cmd test -Dtest=R06*` |
| Abrir o sistema (em memória) | `mvnw.cmd -q compile exec:java` |
| Abrir o sistema com MySQL | `mvnw.cmd -q compile exec:java -Dperfil=producao` |

No Linux/Mac é igual, trocando `mvnw.cmd` por `./mvnw`.

Dá pra mudar a configuração na linha de comando, por exemplo:
`mvnw.cmd -q compile exec:java -Dbanco=B -Dpolitica.multa=TOLERANCIA_2_DIAS`.
Todas as opções estão em `resources/biblioteca.properties`.

### Roteiro da demonstração

1. `mvnw.cmd test`: os 48 testes passam (os cenários de verificação são os `R01_` até `R10_`).
2. `mvnw.cmd -q compile exec:java` e escolher a opção **20** (fluxo completo): reserva → empréstimo →
   renovação → passam 49 dias → devolução atrasada → multa de R$ 5,00 → boleto no banco A →
   inspeção → pagamento → o banco manda a notificação duas vezes e a segunda é ignorada.
3. Opção **24**: manda de novo a última solicitação (mesmo id) e o sistema não executa outra vez.
4. Fluxo manual: opção 1 (carrega dados) e depois 7, 8, 9, 23 (avançar dias), 10, 17, 12, 18.
5. Prorrogação de reserva: 7 (reservar), 23 (avançar 5 dias), 25 (prorroga +7), 25 de novo (recusada),
   23 (avançar 10 dias) e 8 (empréstimo recusado: reserva vencida, livro volta a LIVRE).

### MySQL (perfil produção)

```sql
CREATE DATABASE biblioteca;
CREATE USER 'biblioteca'@'localhost' IDENTIFIED BY 'biblioteca';
GRANT ALL ON biblioteca.* TO 'biblioteca'@'localhost';
```

Se o MySQL foi instalado sem serviço do Windows (ex.: `winget install Oracle.MySQL`), dá pra subir
o servidor numa pasta do usuário, sem precisar de administrador (PowerShell):

```powershell
$bin = "C:\Program Files\MySQL\MySQL Server 8.4\bin"
$data = "$env:USERPROFILE\mysql-biblioteca\data"
& "$bin\mysqld.exe" --initialize-insecure --datadir="$data"   # só na primeira vez (root sem senha)
& "$bin\mysqld.exe" --datadir="$data" --console               # deixa esse terminal aberto
# em outro terminal, crie o banco e o usuário com o script acima:
& "$bin\mysql.exe" -uroot
```

Se o banco foi criado por uma versão anterior do projeto, apague e crie de novo
(`DROP DATABASE biblioteca; CREATE DATABASE biblioteca;`), porque o `schema.sql` só cria tabelas que
ainda não existem e não acrescenta colunas novas.

As tabelas são criadas sozinhas a partir de `resources/schema.sql`. Usuário, senha e URL estão em
`resources/biblioteca.properties`. O Oracle é só réplica do MySQL, então o sistema nunca grava nele
(a fábrica recusa URL do Oracle). Os testes não precisam de MySQL: a parte JDBC é testada com o H2
em modo MySQL.

## Estrutura

```
src/          código
test/         testes JUnit (R01 a R10)
resources/    biblioteca.properties e schema.sql
```

| Pasta (dentro de `src/`) | O que tem | Padrão |
|---|---|---|
| `dominio/livro` | `Livro` e as 7 classes de estado | State |
| `dominio/movimentacao`, `dominio/pessoa` | Reserva, Empréstimo, Devolução, Multa, Pessoa, Estudante, Atendente | - |
| `acesso` | Acervo, Movimentacao e Relacionamento (pontos de acesso do T02) | - |
| `persistencia` (+ `memoria`, `jdbc`) | fábricas, unidade de trabalho e repositórios | Abstract Factory |
| `circulacao` | fachada de circulação e comprovantes | Facade |
| `circulacao/comando` | solicitações de reserva e renovação | Command |
| `devolucao` (+ `ouvintes`) | publicador e ouvintes da devolução | Observer |
| `financeiro/politica` | políticas de multa | Strategy |
| `financeiro/cobranca` | contrato de cobrança e adaptadores dos bancos | Adapter |
| `externo` | simulação dos bancos A e B (sistemas externos) | - |
| `cadastro` | rotina comum de cadastro | Template Method |
| `consulta` | consulta ao acervo com cache e medição | Decorator |
| `organizacao` | unidades, seções e livros em árvore | Composite |
| `tratadores` | ILivro, IPessoa, IEstudante, IAtendente, IEmprestimo, IDevolucao e os tratadores | - |
| `config` | `Aplicacao` (monta tudo) e `Relogio` | - |
| `ui` | menu de linha de comando (só chama os tratadores) | - |

Como as camadas conversam:

```
ui.Main -> tratadores -> ExecutorDeSolicitacoes (Command) -> FachadaCirculacao (Facade)
                                                             -> Acervo / Movimentacao / Relacionamento
                                                             -> Livro -> EstadoLivro (State)
                                                             -> PublicadorDevolucao -> ouvintes (Observer)
ServicoDeMultas -> PoliticaMulta (Strategy) e ICobranca -> AdaptadorBancoA/B (Adapter)
```

## Premissas que adotamos

- Prazo do empréstimo de 14 dias (o enunciado não define). Renovação de +30 dias, no máximo 2
  (`numRenovacoes < 2`). Reserva vale 7 dias e pode ser prorrogada **uma única vez** por mais 7, desde que o pedido seja feito
  antes de vencer. Com a reserva vencida, o empréstimo é recusado, a reserva é removida e o livro volta
  a `LIVRE`. Tudo isso fica em `dominio/RegrasCirculacao.java`.
- Dias de atraso = dias corridos entre o vencimento e a devolução (nunca negativo).
- Dinheiro sempre em `BigDecimal` com 2 casas.
- Livro devolvido fica `DEVOLVIDO` até a inspeção, que manda pra `LIVRE` ou `REPARANDO`. Em
  `REPARANDO`, o reparo concluído volta pra `LIVRE` e o laudo irrecuperável leva a `INCINERADO`.
- A multa é gerada por um ouvinte da devolução (Observer), e não dentro da transição de estado.
- Atraso dentro da tolerância (política com tolerância) não gera multa.

## Limitações conhecidas

Não violam o enunciado, mas é bom saber explicar:

- O registro de solicitações já executadas (Command) fica em memória: com o MySQL, se o sistema
  reiniciar, um id antigo poderia ser executado de novo. O certo seria uma tabela com o id como chave.
- A árvore do acervo (Composite) e a fila de inspeção ficam em memória. No MySQL, depois de reiniciar,
  a opção 15 aparece vazia mesmo com livros em `DEVOLVIDO` (a inspeção continua funcionando, porque
  quem decide é o estado do livro).
- O cache de metadados não é limpo quando o livro é editado (`atualizarLivro`): pode mostrar um título
  antigo, mas nunca afeta a reserva, que sempre lê o estado atual do `Livro`.
- Quitação no MySQL com duas notificações chegando ao mesmo tempo: o resultado final é o mesmo
  (`QUITADA`), mas daria pra blindar com `UPDATE ... WHERE situacao = 'EMITIDA'`.

## Uso de LLM

| Ferramenta / modelo | Onde usamos |
|---|---|
| Claude Code (Claude Opus 5.5, Anthropic) | Estrutura do projeto, código dos 10 padrões, testes R01-R10, menu de linha de comando e rascunho da documentação. |
| Cursor CLI (Grok 4.6 High, SpaceXAI)| Consulta teórica sobre o projeto e suporte e revisão na implementação final dos diagramas, revisão de normativas entre diagramas e código. | | _(outras ferramentas que a equipe usar)_ | |

Os pedidos feitos e o que a equipe aceitou, corrigiu ou descartou estão em [REGISTRO_LLM.md](REGISTRO_LLM.md).
