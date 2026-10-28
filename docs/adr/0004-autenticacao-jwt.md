# ADR-0004: Autenticação JWT com chaves RSA emitidas pela aplicação

- **Data:** 28/10/2026
- **Status:** Aceita

## Contexto

As APIs administrativas (cadastros, estoque e gestão das ordens de serviço) devem ser acessadas apenas por usuários da oficina autenticados com JWT. O projeto tem um único papel administrativo, roda localmente por Docker Compose e não pode versionar senhas nem chaves privadas.

## Decisão

1. **Emissão própria.** A aplicação emite o token em `POST /api/auth/login`.
   - Assinatura RS256.
   - Emissor `fiap-auto-repair-shop`.
   - Login no claim `upn` e papel no claim `groups` (`ADMIN`).
   - Validade de 1 hora, sem refresh token.
2. **Chaves RSA por ambiente.**
   - Em desenvolvimento e testes, o Quarkus gera o par de chaves automaticamente.
   - No Docker Compose, o serviço `jwt-keys` gera o par num volume na primeira execução e o reaproveita nas seguintes. A aplicação lê as chaves pelos caminhos em `JWT_PUBLIC_KEY_LOCATION` e `JWT_PRIVATE_KEY_LOCATION`.
3. **Proteção por caminho.**
   - Toda rota sob `/api/admin/` exige o papel `ADMIN`, por uma única política configurada.
   - A verificação acontece na camada REST, com autenticação não proativa, para que 401 e 403 sejam respondidos em Problem Details como os demais erros.
   - Os recursos também levam `@RolesAllowed("ADMIN")`, que publica o requisito de segurança no OpenAPI.
4. **Usuário de demonstração e senha.**
   - O usuário é criado na inicialização a partir de `ADMIN_USERNAME` e `ADMIN_PASSWORD`, se ainda não existir.
   - A senha é guardada como hash bcrypt de custo 12.
   - O login responde com a mesma mensagem para login inexistente e para senha errada, e executa a verificação bcrypt também quando o login não existe, para que o tempo de resposta não revele quais logins estão cadastrados.

## Alternativas

- **Assinatura HS256 com segredo compartilhado:** é mais simples de configurar, mas a mesma chave assina e verifica. Com RSA, a verificação pode ser feita por outro serviço sem que ele consiga emitir tokens.
- **Provedor OIDC (Keycloak) no compose:** traria refresh token, gestão de usuários e papéis, ao custo de um serviço pesado, configuração de realm e um fluxo de login mais longo, para um único papel.
- **Chaves versionadas para o ambiente local:** facilitariam a primeira execução, mas uma chave privada no repositório é um segredo exposto e é apontada pela análise de segredos.
- **Apenas anotações nos recursos, sem política por caminho:** um recurso novo sem anotação ficaria público.

## Consequências

- Positivas:
  - Nenhuma chave ou senha real no repositório.
  - Um único ponto de configuração protege todas as rotas administrativas.
  - O formato de erro é uniforme.
- Negativas:
  - Sem refresh token, o usuário faz novo login a cada hora.
  - No ambiente local, a chave privada fica legível no volume; em outro ambiente ela viria de um gerenciador de segredos pelo mesmo caminho configurável.
  - Não há gestão de usuários pela API.
