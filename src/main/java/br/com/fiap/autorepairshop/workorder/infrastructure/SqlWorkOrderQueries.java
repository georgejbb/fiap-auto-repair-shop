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
import java.util.List;
import java.util.UUID;

/**
 * Consultas de leitura em SQL. A listagem faz join com cliente e veículo para trazer nome e placa numa única
 * consulta; o acesso às tabelas desses contextos é somente leitura.
 */
@ApplicationScoped
class SqlWorkOrderQueries implements WorkOrderQueries {

    /**
     * Ordena por prioridade de atendimento: em execução termina primeiro; aguardando aprovação depende do
     * cliente; finalizadas e entregues por último. Dentro do status, a aberta há mais tempo primeiro.
     * As consultas são texto fixo: filtros ausentes viram parâmetros que aceitam todos os valores.
     */
    private static final String LIST = """
            SELECT wo.id, wo.status, c.name, v.license_plate, wo.estimate_total, wo.opened_at
            FROM work_order wo
            JOIN customer c ON c.id = wo.customer_id
            JOIN vehicle v ON v.id = wo.vehicle_id
            WHERE wo.status IN (:statuses)
              AND (:allCustomers = TRUE OR wo.customer_id = :customerId)
            ORDER BY CASE wo.status
                         WHEN 'IN_PROGRESS' THEN 1
                         WHEN 'AWAITING_APPROVAL' THEN 2
                         WHEN 'IN_DIAGNOSIS' THEN 3
                         WHEN 'RECEIVED' THEN 4
                         WHEN 'COMPLETED' THEN 5
                         ELSE 6
                     END,
                     wo.opened_at, wo.id
            LIMIT :size OFFSET :offset""";

    private static final String COUNT = """
            SELECT COUNT(*)
            FROM work_order wo
            WHERE wo.status IN (:statuses)
              AND (:allCustomers = TRUE OR wo.customer_id = :customerId)""";

    private static final String EXECUTION_OVERALL = """
            SELECT COUNT(*), AVG(duration_seconds)
            FROM work_order_execution_time
            WHERE completed_at BETWEEN :from AND :to""";

    private static final String EXECUTION_BY_SERVICE = """
            SELECT service_offering_id, MAX(service_name), COUNT(*), AVG(duration_seconds)
            FROM work_order_service_execution_time
            WHERE completed_at BETWEEN :from AND :to
            GROUP BY service_offering_id
            ORDER BY MAX(service_name), service_offering_id""";

    /** Limites usados quando o período não é informado; abrangem qualquer data gravada pela aplicação. */
    private static final Instant EARLIEST = Instant.parse("1900-01-01T00:00:00Z");
    private static final Instant LATEST = Instant.parse("9999-12-31T23:59:59Z");

    /** Valor do parâmetro de cliente quando o filtro não é usado; a condição allCustomers o torna irrelevante. */
    private static final UUID ANY_CUSTOMER = new UUID(0, 0);

    private final EntityManager entityManager;

    SqlWorkOrderQueries(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<WorkOrderSummary> list(WorkOrderFilter filter, int page, int size) {
        List<WorkOrderStatus> statuses = filter.statuses().isEmpty()
                ? List.of(WorkOrderStatus.values())
                : filter.statuses();
        List<String> statusNames = statuses.stream().map(Enum::name).toList();
        boolean allCustomers = filter.customerId() == null;
        UUID customerId = allCustomers ? ANY_CUSTOMER : filter.customerId();

        Query select = entityManager.createNativeQuery(LIST)
                .setParameter("statuses", statusNames)
                .setParameter("allCustomers", allCustomers)
                .setParameter("customerId", customerId)
                .setParameter("size", size)
                .setParameter("offset", page * size);
        Query count = entityManager.createNativeQuery(COUNT)
                .setParameter("statuses", statusNames)
                .setParameter("allCustomers", allCustomers)
                .setParameter("customerId", customerId);

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
        Instant from = period.from() == null ? EARLIEST : period.from();
        Instant to = period.to() == null ? LATEST : period.to();

        Object[] totals = (Object[]) entityManager.createNativeQuery(EXECUTION_OVERALL)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        List<ExecutionTimeReport.ServiceAverage> services = new ArrayList<>();
        for (Object row : entityManager.createNativeQuery(EXECUTION_BY_SERVICE)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList()) {
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
