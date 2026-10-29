# fiap-auto-repair-shop

Back-end do Sistema Integrado de Atendimento e Execução de Serviços de uma oficina mecânica: ordens de serviço, clientes, veículos, serviços, peças e insumos.

Projeto do Tech Challenge da Fase 1 da pós-graduação FIAP SOAT (Software Architecture), Grupo 132.

## Objetivo

Organizar o atendimento da oficina, do recebimento do veículo à entrega, com acompanhamento do status pelo cliente, orçamento gerado a partir dos serviços e peças, autorização do cliente e controle de estoque.

## Solução

API REST com duas áreas:
- **Administrativa** (`/api/admin`, com token JWT): cadastro de clientes, veículos, serviços, peças e insumos; controle de estoque; abertura e condução da ordem de serviço (OS) do diagnóstico à entrega; listagem por prioridade e tempo médio de execução.
- **Pública** (`/api/public`, sem login): o cliente consulta a OS e aprova ou rejeita o orçamento com o código da OS e o seu CPF ou CNPJ.

Ao concluir o diagnóstico, o sistema calcula o orçamento e envia por e-mail o valor e o código da OS. A aprovação do cliente baixa o estoque das peças e insumos na mesma transação que muda o status da OS. O tempo de execução de cada OS alimenta a API de métricas e um painel no Grafana.

## Documentação

| Documento | Local |
|---|---|
| Documentação DDD no Miro (Event Storming, mapa de contexto, agregados, C4 e diagrama de sequência) | a preencher com o link público do quadro |
| Exportação da documentação DDD | [docs/ddd/](docs/ddd/) |
| Linguagem ubíqua e relação com o código | [docs/ddd/linguagem-ubiqua.md](docs/ddd/linguagem-ubiqua.md) |
| Decisões arquiteturais (ADR) | [docs/adr/](docs/adr/README.md) |
| Relatório de vulnerabilidades | [docs/relatorios/vulnerabilidades.md](docs/relatorios/vulnerabilidades.md) |
| Relatório de cobertura de testes | [docs/relatorios/cobertura.md](docs/relatorios/cobertura.md) |
| Coleção Postman com o fluxo completo | [docs/api/](docs/api/fiap-auto-repair-shop.postman_collection.json) |
| Vídeo de apresentação | a preencher com o link do YouTube |

## Arquitetura

Monolito em camadas organizado por bounded context, seguindo Domain-Driven Design. Cada contexto (`workorder`, `customer`, `catalog`, `identity`) é um pacote com as camadas `api`, `application`, `domain` e `infrastructure`; a camada de domínio não depende de framework. Decisão detalhada na [ADR-0002](docs/adr/0002-monolito-em-camadas-por-contexto.md).

## Tecnologias

| Item | Versão |
|---|---|
| Java (Eclipse Temurin) | 25 LTS |
| Quarkus | 3.40 LTS |
| PostgreSQL | 17 |
| Flyway | gerenciado pelo Quarkus |
| Maven (via wrapper) | 3.9.11 |
| Docker e Docker Compose | 24 ou superior |

Justificativa das versões na [ADR-0001](docs/adr/0001-stack-e-versoes.md).

## Banco de dados

PostgreSQL, escolhido porque o domínio é relacional e transacional: a aprovação de um orçamento altera a ordem de serviço e o estoque na mesma transação, e o estoque nunca pode ficar negativo. Decisão e alternativas na [ADR-0003](docs/adr/0003-banco-de-dados-postgresql.md).

## Pré-requisitos

- Para executar: Docker com Docker Compose.
- Para desenvolver e rodar os testes: JDK 25 e Docker em execução, porque os testes sobem um PostgreSQL em container automaticamente.

## Execução

```bash
cp .env.example .env
docker compose up --build
```

A aplicação fica disponível em `http://localhost:8080` depois que o banco estiver pronto. Para encerrar:

```bash
docker compose down
```

As variáveis de ambiente estão descritas em [.env.example](.env.example). Os valores do arquivo servem apenas para o ambiente local.

Na primeira execução:
- o serviço `jwt-keys` gera o par de chaves RSA usado para assinar os tokens e o guarda num volume Docker;
- o banco cria o usuário somente leitura `grafana_reader`, usado pelo painel de observabilidade.

As execuções seguintes reaproveitam os volumes. Para recriar banco, chaves e usuário do painel do zero, use `docker compose down -v`.

## Autenticação

As rotas sob `/api/admin/` exigem um token JWT com o papel `ADMIN`, válido por 1 hora. Decisão detalhada na [ADR-0004](docs/adr/0004-autenticacao-jwt.md).

Credenciais de demonstração, definidas em `.env.example` e criadas na primeira inicialização:

| Login | Senha |
|---|---|
| `admin` | `admin123` |

Obter o token e usá-lo:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}' | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')

curl -X POST http://localhost:8080/api/admin/customers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"document": "529.982.247-25", "name": "Maria Silva", "email": "maria.silva@email.com"}'
```

No Swagger UI, use o botão **Authorize** e informe o token obtido em `POST /api/auth/login`.

## Rotas

| Recurso | Rotas | Observações |
|---|---|---|
| Autenticação | `POST /api/auth/login` | Pública |
| Clientes | `POST`, `GET` (filtro `document`), `GET /{id}`, `PUT /{id}`, `DELETE /{id}` em `/api/admin/customers` | CPF ou CNPJ, inclusive alfanumérico; documento imutável |
| Veículos | `POST`, `GET` (filtros `licensePlate`, `ownerId`), `GET /{id}`, `PUT /{id}`, `DELETE /{id}` em `/api/admin/vehicles` | Placa antiga ou Mercosul; placa imutável |
| Serviços | `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` em `/api/admin/service-offerings` | Preço com até duas casas decimais |
| Peças e insumos | `POST`, `GET` (filtro `category`), `GET /{id}`, `PUT /{id}`, `DELETE /{id}` em `/api/admin/parts` | Estoque inicial zero |
| Estoque | `POST /api/admin/parts/{id}/stock-entries`, `POST /api/admin/parts/{id}/stock-adjustments`, `GET /api/admin/parts/{id}/movements` | Estoque nunca negativo |
| Ordens de serviço | `POST /api/admin/work-orders`, `GET /api/admin/work-orders` (filtros `status`, repetível, e `customerId`), `GET /api/admin/work-orders/{código}` | Abertura, listagem por prioridade e detalhamento |
| Tempo de execução | `GET /api/admin/work-orders/metrics/execution-time` (filtros `from` e `to`, ISO-8601) | Média geral e por serviço |
| Itens da OS | `POST` e `DELETE` em `/api/admin/work-orders/{código}/services` e `/parts` | Somente durante o diagnóstico |
| Transições da OS | `POST /api/admin/work-orders/{código}/start-diagnosis`, `/complete-diagnosis`, `/finish`, `/deliver` | Cada transição é uma rota |
| Acompanhamento pelo cliente | `GET /api/public/work-orders/{código}`, `POST .../approve`, `POST .../reject` | Sem token; exige o cabeçalho `X-Customer-Document` |

As exclusões são lógicas e as alterações usam `PUT` com o recurso completo ([ADR-0005](docs/adr/0005-exclusao-logica-e-put.md)). Listagens aceitam `page` (a partir de 0) e `size` (1 a 100, padrão 20).

## Ciclo da ordem de serviço

```
RECEIVED --start-diagnosis--> IN_DIAGNOSIS --complete-diagnosis--> AWAITING_APPROVAL
                              (inclui itens)  (gera orçamento e     |            |
                                               envia e-mail)     approve       reject
                                                                    |            |
                              IN_PROGRESS <-------------------------+            |
                              (estoque baixado)                                  |
                                   | finish                                      v
                                   +--------------------------------------> COMPLETED --deliver--> DELIVERED
```

Fluxo de ponta a ponta, na ordem de uso:

1. `POST /api/auth/login` para obter o token.
2. Cadastros:
   - `POST /api/admin/customers` e `POST /api/admin/vehicles`, para o cliente e o veículo dele;
   - `POST /api/admin/service-offerings`, `POST /api/admin/parts` e `POST /api/admin/parts/{id}/stock-entries`, para o catálogo e o estoque.
3. `POST /api/admin/work-orders` com `customerId` e `vehicleId`. A resposta traz o `code` da OS.
4. `POST /api/admin/work-orders/{código}/start-diagnosis`.
5. Itens:
   - `POST /api/admin/work-orders/{código}/services` com `serviceOfferingId`;
   - `POST /api/admin/work-orders/{código}/parts` com `partId` e `quantity`.
6. `POST /api/admin/work-orders/{código}/complete-diagnosis`. O orçamento é gerado e o cliente recebe o e-mail com o código da OS.
7. O cliente aprova, sem token:

   ```bash
   curl -X POST http://localhost:8080/api/public/work-orders/{código}/approve \
     -H "X-Customer-Document: 529.982.247-25"
   ```

   A OS passa a `IN_PROGRESS` e o estoque é baixado na mesma transação ([ADR-0006](docs/adr/0006-consistencia-os-estoque-notificacao.md)). Com `/reject`, a OS passa a `COMPLETED` sem execução.
8. `POST /api/admin/work-orders/{código}/finish` e `POST /api/admin/work-orders/{código}/deliver`.

O documento do cliente vai no cabeçalho `X-Customer-Document`, nunca na URL. Código inexistente e documento de outra pessoa recebem a mesma resposta 404.

A coleção [docs/api/fiap-auto-repair-shop.postman_collection.json](docs/api/fiap-auto-repair-shop.postman_collection.json) executa esse fluxo com testes em cada requisição. Ela gera um CPF e uma placa novos a cada execução e pode ser importada no Postman ou executada com o ambiente no ar:

```bash
npx newman run docs/api/fiap-auto-repair-shop.postman_collection.json
```

## E-mail do orçamento

No ambiente do Docker Compose, os e-mails são entregues ao Mailpit, um servidor de testes que não envia nada para fora. Os orçamentos enviados aparecem em http://localhost:8025.

## Prioridade de atendimento e tempo de execução

A listagem de ordens de serviço segue a prioridade de atendimento:
1. `IN_PROGRESS`
2. `AWAITING_APPROVAL`
3. `IN_DIAGNOSIS`
4. `RECEIVED`
5. `COMPLETED`
6. `DELIVERED`

Dentro do mesmo status, a OS aberta há mais tempo vem primeiro.

O tempo de execução de uma OS vai da aprovação do orçamento (`IN_PROGRESS`) até a finalização (`COMPLETED`); OS com orçamento rejeitado não entram no cálculo. A API e o painel leem as mesmas views do banco ([ADR-0007](docs/adr/0007-monitoramento-tempo-execucao.md)).

O Grafana, em http://localhost:3000, abre o dashboard "Tempo de execução das ordens de serviço" sem login. Ele mostra:
- o tempo médio;
- as OS executadas;
- a média por serviço;
- a evolução diária;
- as OS por status.

O painel usa um usuário de banco somente leitura, sem acesso a dados de clientes.

## Documentação da API e saúde

| Recurso | Endereço |
|---|---|
| Swagger UI | http://localhost:8080/q/swagger-ui |
| Caixa de e-mail local (Mailpit) | http://localhost:8025 |
| Painel de observabilidade (Grafana) | http://localhost:3000 |
| Especificação OpenAPI | http://localhost:8080/q/openapi |
| Liveness | http://localhost:8080/q/health/live |
| Readiness (inclui o banco) | http://localhost:8080/q/health/ready |

Erros da API seguem o formato Problem Details (RFC 9457), com `Content-Type: application/problem+json`.

## Testes

```bash
./mvnw verify
```

No Windows, use `mvnw.cmd verify`. O comando executa os testes unitários e de integração e gera o relatório de cobertura em `target/jacoco-report/index.html`. O build falha se a cobertura de linhas ou de ramificações das camadas `domain` e `application` ficar abaixo de 80%. A cobertura da versão entregue está em [docs/relatorios/cobertura.md](docs/relatorios/cobertura.md).

## Integração contínua

O workflow [ci.yml](.github/workflows/ci.yml) roda em cada pull request e em cada push na `main`:
- build e testes;
- relatório de cobertura;
- análise de qualidade e segurança de código no SonarCloud;
- varredura de vulnerabilidades do repositório e da imagem com Trivy.

O resultado da análise de vulnerabilidades da versão entregue está em [docs/relatorios/vulnerabilidades.md](docs/relatorios/vulnerabilidades.md).

## Estrutura de diretórios

```
.
├── .github/workflows/      pipeline de CI
├── docs/
│   ├── adr/                decisões arquiteturais
│   ├── ddd/                documentação DDD exportada e linguagem ubíqua
│   ├── relatorios/         vulnerabilidades e cobertura
│   └── api/                coleção Postman
├── src/main/java/br/com/fiap/autorepairshop/
│   ├── workorder/          contexto Ordem de Serviço
│   ├── customer/           contexto Cadastro (clientes e veículos)
│   ├── catalog/            contexto Catálogo e Estoque
│   ├── identity/           contexto Identidade
│   └── shared/             código transversal (erros, paginação, valor monetário, relógio)
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/       migrações Flyway
├── src/test/java/          testes unitários e de integração
├── observability/          Grafana provisionado e usuário somente leitura do banco
├── Dockerfile
└── docker-compose.yml
```
