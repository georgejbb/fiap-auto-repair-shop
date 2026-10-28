# ADR-0001: Stack e versões

- **Data:** 28/10/2026
- **Status:** Aceita

## Contexto

O back-end é um monolito Java que precisa de suporte de longo prazo, inicialização rápida em container e integração simples com REST, validação, persistência, migração de esquema, documentação OpenAPI e health check. O sistema deve evoluir em etapas, então as versões escolhidas precisam continuar suportadas durante a evolução.

## Decisão

- **Java 25 (LTS)**, distribuição Eclipse Temurin.
- **Quarkus 3.40 (LTS)**, plataforma 3.40.1.1. É a versão LTS mais recente e recomenda Java 25. A compatibilidade com Java 17, 21 e 25 foi confirmada na API oficial do gerador de projetos do Quarkus (`code.quarkus.io/api/streams`).
- **Maven 3.9** via Maven Wrapper, para que o build não dependa da versão instalada na máquina.
- Extensões Quarkus usadas:
  - REST com Jackson;
  - Hibernate ORM com Panache;
  - Hibernate Validator;
  - JDBC PostgreSQL e Flyway;
  - SmallRye OpenAPI e SmallRye Health.
- Execução em modo JVM.

## Alternativas

- **Spring Boot 3:** ecossistema maior, mas inicialização e consumo de memória maiores em container. O Quarkus oferece o mesmo conjunto de funcionalidades com Dev Services, que sobe o banco de teste em container sem configuração.
- **Quarkus 3.33 LTS:** também suporta Java 25, porém com menos tempo de suporte restante.
- **Quarkus 4.0:** está em beta.
- **Java 21 LTS:** é a versão LTS anterior; não há restrição que impeça a mais recente.
- **Imagem nativa (GraalVM):** reduz o tempo de inicialização, mas aumenta muito o tempo de build e restringe reflexão. Não há requisito de inicialização que justifique esse custo.

## Consequências

- Positivas:
  - Versões com suporte durante a evolução do sistema.
  - Build reproduzível pelo wrapper.
  - Testes de integração contra PostgreSQL real, sem configuração manual.
- Negativas:
  - Desenvolvimento local exige JDK 25 e Docker.
  - A atualização para o Quarkus 4 exigirá revisar as extensões quando a versão for estável.
