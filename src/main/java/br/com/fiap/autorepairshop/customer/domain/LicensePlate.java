package br.com.fiap.autorepairshop.customer.domain;

import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Placa do veículo no formato antigo (AAA9999) ou Mercosul (AAA9A99), sem hífen e em maiúsculas.
 *
 * @param value placa normalizada
 */
public record LicensePlate(String value) {

    private static final String FIELD = "licensePlate";
    private static final Pattern OLD_FORMAT = Pattern.compile("[A-Z]{3}\\d{4}");
    private static final Pattern MERCOSUL_FORMAT = Pattern.compile("[A-Z]{3}\\d[A-Z]\\d{2}");
    private static final Pattern FORMATTING = Pattern.compile("[-\\s]");

    /**
     * Valida e normaliza uma placa informada com ou sem hífen, em qualquer caixa.
     *
     * @throws InvalidValueException se a placa não seguir nenhum dos dois formatos
     */
    public static LicensePlate of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidValueException(FIELD, "é obrigatória");
        }
        String normalized = normalize(raw);
        if (!OLD_FORMAT.matcher(normalized).matches() && !MERCOSUL_FORMAT.matcher(normalized).matches()) {
            throw new InvalidValueException(FIELD, "deve seguir o formato AAA9999 ou AAA9A99");
        }
        return new LicensePlate(normalized);
    }

    /** Remove hífen e espaços e converte para maiúsculas, sem validar. */
    public static String normalize(String raw) {
        return FORMATTING.matcher(raw).replaceAll("").toUpperCase(Locale.ROOT);
    }
}
