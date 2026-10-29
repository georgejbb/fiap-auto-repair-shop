# Relatório de cobertura de testes

Data: 28/10/2026. Versão: branch `docs/delivery`, criada a partir do commit `f17b8a7` da `main`. Fonte: relatório JaCoCo de `./mvnw clean verify` (`target/jacoco-report/jacoco.csv`).

## Resultado

- 288 testes, em 34 classes de teste, sem falhas.
- Requisito do enunciado: 80% de cobertura nos domínios críticos.
- Critério adotado: cobertura de linhas e de ramificações das camadas `domain` e `application` de todos os contextos. O build falha abaixo de 80% (regra `jacoco:check` no `pom.xml`).

| Escopo | Linhas | Ramificações |
|---|---|---|
| Camadas `domain` e `application` (critério do build) | 99,8% (665 de 666) | 97,7% (168 de 172) |
| Projeto inteiro | 99,0% (1.198 de 1.210) | 97,4% (261 de 268) |

O SonarCloud calcula 99,3% na análise da `main`, com 99,8% das linhas e 97,4% das ramificações. A diferença em relação ao JaCoCo local vem das exclusões configuradas no Sonar (`package-info.java`) e da forma como ele combina linhas e ramificações num único indicador.

## Por contexto e camada

Ramificações aparecem como "não se aplica" onde a camada não tem desvio condicional.

| Contexto | Camada | Linhas | Ramificações |
|---|---|---|---|
| Ordem de Serviço (`workorder`) | domain | 100,0% (130 de 130) | 100,0% (32 de 32) |
| | application | 100,0% (109 de 109) | 100,0% (22 de 22) |
| | api | 100,0% (67 de 67) | 100,0% (4 de 4) |
| | infrastructure | 100,0% (161 de 161) | 100,0% (48 de 48) |
| Cadastro (`customer`) | domain | 100,0% (136 de 136) | 98,4% (61 de 62) |
| | application | 100,0% (58 de 58) | 100,0% (8 de 8) |
| | api | 97,7% (43 de 44) | 80,0% (8 de 10) |
| | infrastructure | 100,0% (57 de 57) | não se aplica |
| Catálogo e Estoque (`catalog`) | domain | 100,0% (108 de 108) | 92,9% (26 de 28) |
| | application | 100,0% (55 de 55) | não se aplica |
| | api | 97,7% (43 de 44) | não se aplica |
| | infrastructure | 98,5% (66 de 67) | 50,0% (1 de 2) |
| Identidade (`identity`) | domain | 92,3% (12 de 13) | não se aplica |
| | application | 100,0% (26 de 26) | 100,0% (6 de 6) |
| | api | 100,0% (7 de 7) | não se aplica |
| | infrastructure | 57,9% (11 de 19) | não se aplica |
| Compartilhado (`shared`) | domain | 100,0% (31 de 31) | 92,9% (13 de 14) |
| | api | 100,0% (76 de 76) | 100,0% (32 de 32) |
| | infrastructure | 100,0% (2 de 2) | não se aplica |

O menor índice é o da infraestrutura de Identidade. As linhas não cobertas são do método `add` do repositório Panache do usuário administrativo. Esse método só grava o usuário quando ele ainda não existe no banco. O banco de teste dos Dev Services é reaproveitado entre execuções, e nesta execução o usuário já existia quando a aplicação subiu. A regra de criação do usuário é testada em `AdminUserSeederTest`, com repositório simulado. Essa camada fica fora do critério de 80%.

## Tipos de teste

| Tipo | O que cobre | Exemplos |
|---|---|---|
| Unitário de domínio | Regras dos agregados e objetos de valor, sem framework | Transições de status da OS, cálculo do orçamento, validação de CPF, CNPJ e placa, estoque que não fica negativo |
| Unitário de aplicação | Serviços com repositórios simulados (Mockito) | Abertura de OS com cliente e veículo ativos, decisão do cliente |
| Integração | API REST com `@QuarkusTest`, banco PostgreSQL real em container (Dev Services) e migrações Flyway | Fluxo completo da OS, autenticação JWT, baixa de estoque atômica na aprovação, envio do e-mail, consultas por prioridade e tempo de execução |

## Como reproduzir

```bash
./mvnw clean verify
```

O relatório navegável fica em `target/jacoco-report/index.html`. O CI publica o mesmo relatório como artefato `jacoco-report` em cada execução.
