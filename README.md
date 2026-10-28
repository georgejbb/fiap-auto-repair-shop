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

No Windows, use `mvnw.cmd verify`. O comando executa os testes unitários e de integração e gera o relatório de cobertura em `target/site/jacoco/index.html`. O build falha se a cobertura de linhas ou de ramificações das camadas `domain` e `application` ficar abaixo de 80%.

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
│   └── shared/             código transversal (tratamento de erros)
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/       migrações Flyway
├── src/test/java/          testes unitários e de integração
├── Dockerfile
└── docker-compose.yml
```
