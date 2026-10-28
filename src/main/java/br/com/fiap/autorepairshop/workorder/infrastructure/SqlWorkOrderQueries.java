package br.com.fiap.autorepairshop.workorder.infrastructure;

import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.shared.domain.Page;
import br.com.fiap.autorepairshop.workorder.application.ExecutionPeriod;
import br.com.fiap.autorepairshop.workorder.application.ExecutionTimeReport;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderFilter;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderQueries;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderSummary;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consultas de leitura em SQL. A listagem faz join com cliente e veículo para trazer nome e placa numa única
 * consulta; o acesso às tabelas desses contextos é somente leitura.
 */
@ApplicationScoped
class SqlWorkOrderQueries implements WorkOrderQueries {

    /** Em execução termina primeiro; aguardando aprovação depende do cliente; finalizadas e entregues por último. */
    private static final String PRIORITY = """
            CASE wo.status
                WHEN 'IN_PROGRESS' THEN 1
                WHEN 'AWAITING_APPROVAL' THEN 2
                WHEN 'IN_DIAGNOSIS' THEN 3
                WHEN 'RECEIVED' THEN 4
                WHEN 'COMPLETED' THEN 5
                ELSE 6
            END""";

    private final EntityManager entityManager;

    SqlWorkOrderQueries(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<WorkOrderSummary> list(WorkOrderFilter filter, int page, int size) {
        Map<String, Object> parameters = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (!filter.statuses().isEmpty()) {
            where.append(" AND wo.status IN (:statuses)");
            parameters.put("statuses", filter.statuses().stream().map(Enum::name).toList());
        }
        if (filter.customerId() != null) {
            where.append(" AND wo.customer_id = :customerId");
            parameters.put("customerId", filter.customerId());
        }
        String from = " FROM work_order wo"
                + " JOIN customer c ON c.id = wo.customer_id"
                + " JOIN vehicle v ON v.id = wo.vehicle_id";

        Query select = entityManager.createNativeQuery(
                "SELECT wo.id, wo.status, c.name, v.license_plate, wo.estimate_total, wo.opened_at" + from + where
                        + " ORDER BY " + PRIORITY + ", wo.opened_at, wo.id LIMIT :size OFFSET :offset");
        Query count = entityManager.createNativeQuery("SELECT COUNT(*)" + from + where);
        parameters.forEach((name, value) -> {
            select.setParameter(name, value);
            count.setParameter(name, value);
        });
        select.setParameter("size", size);
        select.setParameter("offset", page * size);

        List<WorkOrderSummary> items = new ArrayList<>();
        for (Object row : select.getResultList()) {
            Object[] columns = (Object[]) row;
            BigDecimal total = (BigDecimal) columns[4];
            items.add(new WorkOrderSummary((UUID) columns[0], WorkOrderStatus.valueOf((String) columns[1]),
                    (String) columns[2], (String) columns[3], total == null ? null : new Money(total),
                    instant(columns[5])));
        }
        return new Page<>(items, ((Number) count.getSingleResult()).longValue());
    }

    @Override
    public ExecutionTimeReport executionTime(ExecutionPeriod period) {
        Map<String, Object> parameters = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (period.from() != null) {
            where.append(" AND completed_at >= :from");
            parameters.put("from", period.from());
        }
        if (period.to() != null) {
            where.append(" AND completed_at <= :to");
            parameters.put("to", period.to());
        }

        Query overall = entityManager.createNativeQuery(
                "SELECT COUNT(*), AVG(duration_seconds) FROM work_order_execution_time" + where);
        Query byService = entityManager.createNativeQuery(
                "SELECT service_offering_id, MAX(service_name), COUNT(*), AVG(duration_seconds)"
                        + " FROM work_order_service_execution_time" + where
                        + " GROUP BY service_offering_id ORDER BY MAX(service_name), service_offering_id");
        parameters.forEach((name, value) -> {
            overall.setParameter(name, value);
            byService.setParameter(name, value);
        });

        Object[] totals = (Object[]) overall.getSingleResult();
        List<ExecutionTimeReport.ServiceAverage> services = new ArrayList<>();
        for (Object row : byService.getResultList()) {
            Object[] columns = (Object[]) row;
            services.add(new ExecutionTimeReport.ServiceAverage((UUID) columns[0], (String) columns[1],
                    ((Number) columns[2]).longValue(), seconds(columns[3])));
        }
        Long average = totals[1] == null ? null : seconds(totals[1]);
        return new ExecutionTimeReport(((Number) totals[0]).longValue(), average, services);
    }

    private static long seconds(Object average) {
        return Math.round(((Number) average).doubleValue());
    }

    /** O driver pode devolver o timestamp com fuso em tipos diferentes conforme a versão. */
    static Instant instant(Object value) {
        return switch (value) {
            case Instant instant -> instant;
            case OffsetDateTime offset -> offset.toInstant();
            case Timestamp timestamp -> timestamp.toInstant();
            default -> throw new IllegalStateException("Tipo de data inesperado: " + value.getClass());
        };
    }
}
