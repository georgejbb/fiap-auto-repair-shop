# ADR-0007: Monitoramento do tempo de execução com Grafana sobre views do banco

- **Data:** 28/10/2026
- **Status:** Aceita

## Contexto

A gestão da oficina precisa acompanhar o tempo médio de execução dos serviços numa ferramenta de observabilidade e também consultá-lo pela API. O histórico de status de cada ordem de serviço já é gravado com o instante de cada transição, o que contém tudo o que o cálculo precisa: da entrada em `IN_PROGRESS` até a passagem para `COMPLETED`.

O painel não deve ter acesso de escrita nem a dados pessoais de clientes.

## Decisão

1. **Duas views no PostgreSQL são a fonte única do cálculo:**
   - `work_order_execution_time`: uma linha por OS executada, com início, fim e duração em segundos;
   - `work_order_service_execution_time`: uma linha por OS executada e serviço incluído nela.

   OS com orçamento rejeitado e OS ainda em execução não aparecem nas views.
2. **O endpoint `GET /api/admin/work-orders/metrics/execution-time` e o Grafana leem as mesmas views.** API e painel não divergem.
3. **O Grafana roda no Docker Compose** com datasource e dashboard provisionados por arquivo. O dashboard tem:
   - tempo médio;
   - quantidade de OS executadas;
   - média por serviço;
   - evolução diária;
   - OS por status.
4. **O painel usa o usuário de banco `grafana_reader`.**
   - É criado na inicialização do volume, com senha vinda do `.env`.
   - A migração que cria as views concede a ele apenas `SELECT` nas views e nas colunas `id`, `status` e `opened_at` de `work_order`. Ele não altera dados e não lê nome, documento ou e-mail.
5. No ambiente local, o Grafana permite visualização anônima (papel `Viewer`); o login de administrador só é necessário para editar.

## Alternativas

- **Micrometer com Prometheus e Grafana:** é o padrão para métricas de runtime (latência, erros, uso de recursos). Para esta métrica de negócio, exigiria mais um container, e um timer em memória zera a cada reinício da aplicação, enquanto o histórico gravado no banco já cobre todo o período.
- **Apenas o endpoint, sem painel:** atende ao cálculo, mas não oferece acompanhamento visual nem evolução no tempo.
- **Grafana com o usuário da aplicação:** é mais simples de configurar, mas dá ao painel permissão de escrita e acesso a dados pessoais sem necessidade.
- **Tabela de métricas atualizada pela aplicação a cada finalização:** evitaria o cálculo na consulta, mas duplica informação que o histórico já tem e exige manter as duas consistentes.

## Consequências

- Positivas:
  - Um único cálculo para API e painel.
  - O monitoramento funciona desde a primeira subida do ambiente, sem configuração manual.
  - O painel opera com o menor privilégio necessário.
- Negativas:
  - As views dependem das tabelas da ordem de serviço; uma mudança no histórico de status exige revisar as views junto.
  - O usuário do painel só é criado na inicialização de um volume novo do banco; em volumes antigos é preciso recriar o ambiente com `docker compose down -v`.
  - Métricas de runtime da aplicação ficam fora deste painel.
