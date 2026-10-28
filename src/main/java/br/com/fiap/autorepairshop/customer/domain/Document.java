package br.com.fiap.autorepairshop.customer.domain;

import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * CPF ou CNPJ do cliente, normalizado e com dígitos verificadores válidos.
 *
 * <p>O CNPJ aceita os 12 primeiros caracteres alfanuméricos (formato em vigor desde julho de 2026) e os
 * 2 dígitos verificadores numéricos. O cálculo é o mesmo do CNPJ numérico: cada caractere vale seu código
 * ASCII menos 48, o que dá aos dígitos os valores de 0 a 9 e às letras os valores de 17 a 42.
 *
 * @param type   tipo do documento
 * @param number documento sem pontuação, com letras em maiúsculas
 */
public record Document(DocumentType type, String number) {

    private static final String FIELD = "document";
    private static final Pattern CPF = Pattern.compile("\\d{11}");
    private static final Pattern CNPJ = Pattern.compile("[0-9A-Z]{12}\\d{2}");
    private static final Pattern FORMATTING = Pattern.compile("[.\\-/\\s]");

    private static final int[] CNPJ_FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CNPJ_SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    /**
     * Valida e normaliza um documento informado com ou sem pontuação.
     *
     * @throws InvalidValueException se não for um CPF ou CNPJ válido
     */
    public static Document of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidValueException(FIELD, "é obrigatório");
        }
        String normalized = normalize(raw);
        if (CPF.matcher(normalized).matches() && hasValidCpfDigits(normalized)) {
            return new Document(DocumentType.CPF, normalized);
        }
        if (CNPJ.matcher(normalized).matches() && hasValidCnpjDigits(normalized)) {
            return new Document(DocumentType.CNPJ, normalized);
        }
        throw new InvalidValueException(FIELD, "não é um CPF ou CNPJ válido");
    }

    /** Remove pontuação e espaços e converte letras para maiúsculas, sem validar. */
    public static String normalize(String raw) {
        return FORMATTING.matcher(raw).replaceAll("").toUpperCase(Locale.ROOT);
    }

    private static boolean hasValidCpfDigits(String cpf) {
        if (allSameCharacter(cpf)) {
            return false;
        }
        int[] values = values(cpf);
        int first = checkDigit(values, 9, 10);
        int second = checkDigit(values, 10, 11);
        return values[9] == first && values[10] == second;
    }

    private static boolean hasValidCnpjDigits(String cnpj) {
        if (allSameCharacter(cnpj)) {
            return false;
        }
        int[] values = values(cnpj);
        int first = checkDigit(values, CNPJ_FIRST_WEIGHTS);
        int second = checkDigit(values, CNPJ_SECOND_WEIGHTS);
        return values[12] == first && values[13] == second;
    }

    /** Pesos decrescentes a partir de {@code firstWeight}, aplicados às primeiras {@code length} posições. */
    private static int checkDigit(int[] values, int length, int firstWeight) {
        int[] weights = new int[length];
        for (int i = 0; i < length; i++) {
            weights[i] = firstWeight - i;
        }
        return checkDigit(values, weights);
    }

    private static int checkDigit(int[] values, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += values[i] * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int[] values(String document) {
        int[] values = new int[document.length()];
        for (int i = 0; i < document.length(); i++) {
            values[i] = document.charAt(i) - '0';
        }
        return values;
    }

    private static boolean allSameCharacter(String document) {
        return document.chars().distinct().count() == 1;
    }
}
