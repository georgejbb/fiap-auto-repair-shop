package br.com.fiap.autorepairshop.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor monetário em reais, com duas casas decimais e nunca negativo.
 *
 * @param amount valor com escala 2
 */
public record Money(BigDecimal amount) {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount == null || amount.signum() < 0 || amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Valor monetário inválido: " + amount);
        }
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * Cria o valor a partir de uma entrada externa, associando a falha ao campo informado.
     * Valores com mais de duas casas decimais são rejeitados em vez de arredondados.
     */
    public static Money of(String field, BigDecimal amount) {
        if (amount == null) {
            throw new InvalidValueException(field, "é obrigatório");
        }
        if (amount.signum() < 0) {
            throw new InvalidValueException(field, "deve ser maior ou igual a zero");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidValueException(field, "deve ter no máximo duas casas decimais");
        }
        return new Money(amount);
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    /** Valor multiplicado por uma quantidade, como o total de uma linha de peças. */
    public Money times(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantidade negativa: " + quantity);
        }
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
    }
}
