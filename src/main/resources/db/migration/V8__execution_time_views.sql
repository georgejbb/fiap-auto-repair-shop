-- Tempo de execução das ordens de serviço: da entrada em IN_PROGRESS até a mudança seguinte para COMPLETED.
-- OS com orçamento rejeitado (AWAITING_APPROVAL para COMPLETED) e OS ainda em execução não aparecem.
-- As views são a fonte única do endpoint de métricas e do painel de observabilidade.

CREATE VIEW work_order_execution_time AS
SELECT wo.id                                                           AS work_order_id,
       started.changed_at                                              AS started_at,
       completed.changed_at                                            AS completed_at,
       EXTRACT(EPOCH FROM (completed.changed_at - started.changed_at)) AS duration_seconds
FROM work_order wo
JOIN work_order_status_change started
  ON started.work_order_id = wo.id
 AND started.to_status = 'IN_PROGRESS'
JOIN work_order_status_change completed
  ON completed.work_order_id = wo.id
 AND completed.from_status = 'IN_PROGRESS'
 AND completed.to_status = 'COMPLETED';

-- Uma linha por OS executada e serviço do catálogo incluído nela, para a média por serviço.
CREATE VIEW work_order_service_execution_time AS
SELECT et.work_order_id,
       line.service_offering_id,
       line.name AS service_name,
       et.completed_at,
       et.duration_seconds
FROM work_order_execution_time et
JOIN work_order_service_line line ON line.work_order_id = et.work_order_id;

-- O usuário do painel (criado pelo ambiente local) lê apenas as views e o status das OS, sem dados pessoais.
-- Onde o usuário não existe, como nos testes, as concessões são ignoradas.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'grafana_reader') THEN
        GRANT SELECT ON work_order_execution_time TO grafana_reader;
        GRANT SELECT ON work_order_service_execution_time TO grafana_reader;
        GRANT SELECT (id, status, opened_at) ON work_order TO grafana_reader;
    END IF;
END
$$;
