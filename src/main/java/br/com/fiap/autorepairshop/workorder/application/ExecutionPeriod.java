package br.com.fiap.autorepairshop.workorder.application;

import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Período de conclusão das ordens de serviço consideradas no tempo médio. Os dois limites são opcionais e
 * inclusivos.
 *
 * @param from início; nulo para sem limite
 * @param to   fim; nulo para sem limite
 */
public record ExecutionPeriod(Instant from, Instant to) {

    public static ExecutionPeriod of(String from, String to) {
        Instant start = parse("from", from);
        Instant end = parse("to", to);
        if (start != null && end != null && start.isAfter(end)) {
            throw new InvalidValueException("from", "deve ser anterior ou igual a to");
        }
        return new ExecutionPeriod(start, end);
    }

    private static Instant parse(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.strip());
        } catch (DateTimeParseException e) {
            throw new InvalidValueException(field, "deve estar no formato ISO-8601, por exemplo 2026-10-28T00:00:00Z");
        }
    }
}
