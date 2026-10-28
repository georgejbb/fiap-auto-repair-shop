# fiap-auto-repair-shop

Back-end do Sistema Integrado de Atendimento e Execução de Serviços de uma oficina mecânica: ordens de serviço, clientes, veículos, serviços, peças e insumos.

Projeto do Tech Challenge da Fase 1 da pós-graduação FIAP SOAT (Software Architecture), Grupo 132.

## Objetivo

Organizar o atendimento da oficina, do recebimento do veículo à entrega, com acompanhamento do status pelo cliente, orçamento gerado a partir dos serviços e peças, autorização do cliente e controle de estoque.

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

Na primeira execução, o serviço `jwt-keys` gera o par de chaves RSA usado para assinar os tokens e o guarda num volume Docker; as execuções seguintes reaproveitam as mesmas chaves. Para recriar banco e chaves do zero, use `docker compose down -v`.

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

As exclusões são lógicas e as alterações usam `PUT` com o recurso completo ([ADR-0005](docs/adr/0005-exclusao-logica-e-put.md)). Listagens aceitam `page` (a partir de 0) e `size` (1 a 100, padrão 20).

## Documentação da API e saúde

| Recurso | Endereço |
|---|---|
| Swagger UI | http://localhost:8080/q/swagger-ui |
| Especificação OpenAPI | http://localhost:8080/q/openapi |
| Liveness | http://localhost:8080/q/health/live |
| Readiness (inclui o banco) | http://localhost:8080/q/health/ready |

Erros da API seguem o formato Problem Details (RFC 9457), com `Content-Type: application/problem+json`.

## Testes

```bash
./mvnw verify
```

No Windows, use `mvnw.cmd verify`. O comando executa os testes unitários e de integração e gera o relatório de cobertura em `target/jacoco-report/index.html`. O build falha se a cobertura de linhas ou de ramificações das camadas `domain` e `application` ficar abaixo de 80%.

## Integração contínua

O workflow [ci.yml](.github/workflows/ci.yml) roda em cada pull request e em cada push na `main`:
- build e testes;
- relatório de cobertura;
- análise de qualidade e segurança de código no SonarCloud;
- varredura de vulnerabilidades do repositório e da imagem com Trivy.

## Estrutura de diretórios

```
.
├── .github/workflows/      pipeline de CI
├── docs/adr/               decisões arquiteturais
├── src/main/java/br/com/fiap/autorepairshop/
│   ├── workorder/          contexto Ordem de Serviço
│   ├── customer/           contexto Cadastro (clientes e veículos)
│   ├── catalog/            contexto Catálogo e Estoque
│   ├── identity/           contexto Identidade
│   └── shared/             código transversal (erros, paginação, valor monetário)
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/       migrações Flyway
├── src/test/java/          testes unitários e de integração
├── Dockerfile
└── docker-compose.yml
```
