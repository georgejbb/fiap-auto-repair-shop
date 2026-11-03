# Relatório de vulnerabilidades

Data da análise: 28/10/2026 (SonarCloud e Trivy) e 29/10/2026 (OWASP ZAP). Versões analisadas: branch `docs/delivery`, a partir do commit `f17b8a7` da `main`, e branch `docs/ddd-exports`, a partir do commit `8e7e1f4`.

## Ferramentas e cobertura da análise

| Ferramenta | Tipo de análise | O que verifica | Quando roda |
|---|---|---|---|
| SonarCloud | Análise estática do código (SAST) | Vulnerabilidades, hotspots de segurança, bugs, code smells, duplicação e cobertura | A cada push na `main` e a cada PR, no job `build` do CI |
| Trivy, modo `fs` | Dependências (SCA), segredos e configuração | `pom.xml`, segredos em arquivos versionados e boas práticas do `Dockerfile` | A cada push e PR, no job `security` do CI |
| Trivy, modo `image` | Sistema operacional e bibliotecas da imagem | Pacotes do Alpine e todos os JARs empacotados na imagem final | A cada push e PR, no job `security` do CI |
| OWASP ZAP | Análise dinâmica (DAST) | Ataques reais contra a API em execução: injeção, XSS, travessia de diretório, cabeçalhos de segurança, vazamento de informação | Manualmente, contra o ambiente do `docker compose` |

O CI bloqueia o merge quando o Quality Gate do SonarCloud falha ou quando o Trivy encontra vulnerabilidade CRITICAL com correção disponível. A regra de proteção `protect-main` exige o PR e os dois jobs aprovados.

## SonarCloud

Painel público: https://sonarcloud.io/project/overview?id=georgejbb_fiap-auto-repair-shop

Última análise da `main` (commit `f17b8a7`, 28/10/2026):

| Métrica | Resultado |
|---|---|
| Quality Gate | Aprovado |
| Vulnerabilidades | 0 (nota A) |
| Security hotspots | 0, 100% revisados (nota A) |
| Bugs | 0 (nota A) |
| Code smells | 2 (nota A de manutenibilidade) |
| Duplicação | 0,0% |
| Cobertura | 99,3% (linhas 99,8%, ramificações 97,4%) |
| Testes | 288 |
| Linhas de código | 3.767 |

Condições do Quality Gate (código novo): nota de confiabilidade A, nota de segurança A, nota de manutenibilidade A, cobertura de 99,3%, 0,0% de duplicação e 100% dos hotspots revisados. Todas aprovadas.

Os dois code smells apontados eram do mesmo tipo (regra S8786, expressão regular com retrocesso excessivo), num teste de autenticação que extraía as datas do token com `replaceAll`. Foram corrigidos nesta versão: o teste passou a ler o conteúdo do token como JSON. A análise do PR desta versão confirma o resultado.

Problemas apontados pelo SonarCloud durante o desenvolvimento e já corrigidos:
- Actions do GitHub referenciadas por tag (regra S7637, nota de segurança C no PR da estrutura inicial). Passaram a ser fixadas pelo hash do commit.
- SQL montado por concatenação na listagem de ordens de serviço (regra S2077, hotspot de injeção de SQL). As consultas passaram a ser constantes, com todos os filtros em parâmetros.

## Trivy

Relatórios completos:
- [trivy-repositorio.txt](trivy-repositorio.txt)
- [trivy-imagem.txt](trivy-imagem.txt)

### Repositório

| Alvo | Vulnerabilidades | Segredos | Configuração |
|---|---|---|---|
| `pom.xml` | 0 | 0 | não se aplica |
| `Dockerfile` | não se aplica | 0 | 0 |

### Imagem Docker

| Severidade | Primeira análise | Depois da correção |
|---|---|---|
| CRITICAL | 0 | 0 |
| HIGH | 1 | 0 |
| MEDIUM | 0 | 0 |
| LOW | 0 | 0 |
| UNKNOWN | 0 | 0 |

A análise cobre o Alpine 3.24.3 e as 209 bibliotecas Java empacotadas na imagem; nenhuma biblioteca Java tem vulnerabilidade conhecida.

Vulnerabilidade encontrada e tratada:

| CVE | Pacote | Severidade | Versão instalada | Versão corrigida | Tratamento |
|---|---|---|---|---|---|
| CVE-2026-111497 | `brotli-libs` (Alpine) | HIGH | 1.2.0-r1 | 1.2.1-r0 | `apk upgrade --no-cache` no estágio de execução do `Dockerfile` |

A falha está na imagem base `eclipse-temurin:25-jre-alpine`, que ainda trazia a versão vulnerável na data da análise. Como o Alpine já publicou a correção, o `Dockerfile` atualiza os pacotes do sistema ao construir a imagem. Assim, a correção não depende de uma nova versão da imagem base, e correções futuras entram no próximo build.

## OWASP ZAP

Relatório completo: [zap-api.txt](zap-api.txt)

O ZAP leu a definição OpenAPI da aplicação e atacou as 40 operações documentadas, num total de 111 URLs, com o token do usuário `admin`. Assim as rotas administrativas foram testadas de verdade, e não apenas recusadas com 401.

| Resultado | Primeira análise | Depois das correções |
|---|---|---|
| Falhas (FAIL) | 0 | 0 |
| Alertas (WARN) | 3 tipos | 0 |
| Regras aprovadas | 118 | 120 |

Alertas encontrados e tratados:

| Alerta | Onde | Causa | Tratamento |
|---|---|---|---|
| Resposta 500 do servidor | Busca de cliente por documento e cadastro de serviço e de peça | Texto com caractere nulo (`%00`) chegava ao PostgreSQL, que recusa o valor. A resposta já vinha sem detalhes internos, mas com status de erro do servidor | `PersistenceExceptionMapper` passou a responder 400 quando o banco recusa um valor enviado pelo cliente. Testes em `CustomerResourceTest` e `DomainAndPersistenceMappersTest` |
| Cabeçalho `X-Content-Type-Options` ausente | Todas as respostas | Configuração padrão do servidor HTTP | `X-Content-Type-Options: nosniff` em `application.properties` |
| Cabeçalho `Cross-Origin-Resource-Policy` ausente | Todas as respostas | Configuração padrão do servidor HTTP | `Cross-Origin-Resource-Policy: same-origin` em `application.properties`. Teste em `ErrorResponsesTest` |

Os itens informativos que restam no relatório são esperados: respostas 404, 400 e 405 às tentativas de ataque e a identificação da rota de login.

## OWASP Top 10

Situação da aplicação em relação a cada categoria do OWASP Top 10 (edição 2021).

| Categoria | Controles na aplicação | Limitação conhecida |
|---|---|---|
| A01 Quebra de controle de acesso | Papel `ADMIN` exigido em `/api/admin/*`; a rota pública só devolve a OS quando código e documento conferem, com a mesma resposta 404 para OS inexistente e documento errado; código da OS não sequencial (UUID) | Um único papel administrativo para atendente, mecânico e gestor de estoque |
| A02 Falhas criptográficas | Token assinado com RSA (RS256); senha guardada só como hash bcrypt; chaves fora da imagem e do repositório | O `docker compose` local usa HTTP. Em produção o TLS fica num proxy ou balanceador à frente da aplicação |
| A03 Injeção | Consultas parametrizadas no Panache e SQL constante nas consultas de leitura; validação de entrada com Bean Validation e objetos de valor; regras de injeção do ZAP aprovadas | Nenhuma |
| A04 Design inseguro | Regras de negócio no agregado, com transições de status controladas; estoque nunca negativo, garantido também no banco; aprovação e baixa de estoque na mesma transação; controle de concorrência por versão | Sem limite de requisições no login e na rota pública |
| A05 Configuração incorreta de segurança | Erros em Problem Details sem detalhes internos; cabeçalhos `nosniff` e `same-origin`; contêiner sem root; `Dockerfile` sem problemas de configuração no Trivy | Swagger UI disponível em todos os ambientes, para a demonstração |
| A06 Componentes vulneráveis e desatualizados | BOM do Quarkus 3.40 LTS; Trivy no repositório e na imagem a cada PR; CVE da imagem base corrigida; CI bloqueia CRITICAL | Nenhuma |
| A07 Falhas de identificação e autenticação | Token com validade de 1 hora e assinatura verificada; resposta de login com o mesmo tempo para usuário inexistente e senha errada | Sem bloqueio após tentativas seguidas e sem revogação de token antes de expirar |
| A08 Falhas de integridade de software e dados | Actions do CI fixadas pelo hash; migrações Flyway com verificação de checksum; dependências do Maven Central | Nenhuma |
| A09 Falhas de registro e monitoramento | Toda falha inesperada vai para o log com um identificador, que também é devolvido ao cliente; health checks de liveness e readiness | Sem central de logs nem alertas |
| A10 Falsificação de requisição do lado do servidor (SSRF) | A aplicação não faz requisições a endereços informados pelo usuário; a única conexão de saída é o servidor SMTP configurado | Não se aplica |

## Medidas de segurança no código e na infraestrutura

| Medida | Onde |
|---|---|
| Autenticação JWT RS256 com validade de 1 hora; papel `ADMIN` exigido em `/api/admin/*` | [ADR 0004](../adr/0004-autenticacao-jwt.md) |
| Senha guardada só como hash bcrypt; login com tempo de resposta igual para usuário inexistente e senha errada | `identity` |
| Chaves JWT fora da imagem e do repositório, geradas no primeiro `docker compose up` | `docker-compose.yml`, serviço `jwt-keys` |
| Credenciais e endereços por variáveis de ambiente; `.env` fora do versionamento | `.env.example` |
| Consulta do cliente sem login: OS inexistente e documento incorreto devolvem a mesma resposta 404, sem revelar se o código existe | `CustomerDecisionService` |
| SQL constante, com todos os valores em parâmetros | `SqlWorkOrderQueries` |
| Erros no formato Problem Details sem pilha de exceção nem detalhes internos | `shared.api` |
| Cabeçalhos `X-Content-Type-Options: nosniff` e `Cross-Origin-Resource-Policy: same-origin` em todas as respostas | `application.properties` |
| Contêiner da aplicação com usuário sem privilégio | `Dockerfile` |
| Usuário do Grafana somente leitura, com acesso apenas às views de tempo de execução e ao status das OS | [ADR 0007](../adr/0007-monitoramento-tempo-execucao.md) |
| Dependências gerenciadas pelo BOM do Quarkus 3.40 LTS; actions do CI fixadas pelo hash | `pom.xml`, `.github/workflows/ci.yml` |

## Limitações

- O resultado vale para a data da análise. Novas CVEs podem ser publicadas depois, e o CI repete as análises a cada alteração.
- No modo `fs`, o Trivy resolve as dependências transitivas do `pom.xml` a partir do repositório Maven local. A análise da imagem é a verificação definitiva das dependências, porque examina os JARs que de fato vão para produção.

## Como reproduzir

```bash
docker compose build app
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy:0.76.1 image fiap-auto-repair-shop-app:latest
docker run --rm -v "$PWD:/src" -v "$HOME/.m2:/root/.m2:ro" aquasec/trivy:0.76.1 fs --scanners vuln,secret,misconfig --skip-dirs /src/target /src
```

Para o ZAP, com a aplicação no ar (`docker compose up -d`) e um token obtido em `POST /api/auth/login`:

```bash
docker run --rm -e ZAP_AUTH_HEADER_VALUE="Bearer <token>" -v "$PWD:/zap/wrk" zaproxy/zap-stable:2.17.0   zap-api-scan.py -t http://host.docker.internal:8080/q/openapi -f openapi -O http://host.docker.internal:8080 -r zap-report.html
```

Os relatórios do CI ficam nos artefatos de cada execução do workflow, com o nome `trivy-reports`.
