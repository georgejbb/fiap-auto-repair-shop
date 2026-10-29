# Relatório de vulnerabilidades

Data da análise: 28/10/2026. Versão analisada: branch `docs/delivery`, criada a partir do commit `f17b8a7` da `main`.

## Ferramentas e cobertura da análise

| Ferramenta | Tipo de análise | O que verifica | Quando roda |
|---|---|---|---|
| SonarCloud | Análise estática do código (SAST) | Vulnerabilidades, hotspots de segurança, bugs, code smells, duplicação e cobertura | A cada push na `main` e a cada PR, no job `build` do CI |
| Trivy, modo `fs` | Dependências (SCA), segredos e configuração | `pom.xml`, segredos em arquivos versionados e boas práticas do `Dockerfile` | A cada push e PR, no job `security` do CI |
| Trivy, modo `image` | Sistema operacional e bibliotecas da imagem | Pacotes do Alpine e todos os JARs empacotados na imagem final | A cada push e PR, no job `security` do CI |

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

Os relatórios do CI ficam nos artefatos de cada execução do workflow, com o nome `trivy-reports`.
