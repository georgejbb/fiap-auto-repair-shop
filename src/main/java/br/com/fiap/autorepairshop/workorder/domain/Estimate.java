package br.com.fiap.autorepairshop.workorder.domain;

import br.com.fiap.autorepairshop.shared.domain.Money;
import java.time.Instant;
import java.util.Objects;

/**
 * Orçamento da OS, gerado na conclusão do diagnóstico.
 *
 * @param total       soma dos itens
 * @param generatedAt instante da geração
 * @param decision    decisão do cliente; nula enquanto aguarda
 * @param decidedAt   instante da decisão; nulo enquanto aguarda
 */
public record Estimate(Money total, Instant generatedAt, EstimateDecision decision, Instant decidedAt) {

    public Estimate {
        Objects.requireNonNull(total);
        Objects.requireNonNull(generatedAt);
    }

    static Estimate generated(Money total, Instant now) {
        return new Estimate(total, now, null, null);
    }

    Estimate decide(EstimateDecision newDecision, Instant now) {
        return new Estimate(total, generatedAt, newDecision, now);
    }
}
