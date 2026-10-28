package br.com.fiap.autorepairshop.support;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Gera CPF, CNPJ e placas válidos e distintos, para que os testes de integração não colidam nas
 * restrições de unicidade do banco compartilhado entre os testes.
 */
public final class TestDocuments {

    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private TestDocuments() {
    }

    public static String cpf() {
        int[] digits = new int[11];
        for (int i = 0; i < 9; i++) {
            digits[i] = random(10);
        }
        digits[9] = checkDigit(digits, 9, 10);
        digits[10] = checkDigit(digits, 10, 11);
        return join(digits);
    }

    public static String cnpj() {
        StringBuilder base = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            base.append(random(10));
        }
        return withCnpjDigits(base.toString());
    }

    public static String alphanumericCnpj() {
        StringBuilder base = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            base.append(ALPHANUMERIC.charAt(random(ALPHANUMERIC.length())));
        }
        base.setCharAt(0, 'A');
        return withCnpjDigits(base.toString());
    }

    /** Placa no padrão Mercosul (AAA9A99). */
    public static String mercosulPlate() {
        return "" + letter() + letter() + letter() + random(10) + letter() + random(10) + random(10);
    }

    /** Placa no padrão antigo (AAA9999). */
    public static String oldPlate() {
        return "" + letter() + letter() + letter() + random(10) + random(10) + random(10) + random(10);
    }

    private static String withCnpjDigits(String base) {
        int[] values = new int[14];
        for (int i = 0; i < 12; i++) {
            values[i] = base.charAt(i) - '0';
        }
        values[12] = checkDigit(values, new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        values[13] = checkDigit(values, new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return base + values[12] + values[13];
    }

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

    private static String join(int[] digits) {
        StringBuilder text = new StringBuilder();
        for (int digit : digits) {
            text.append(digit);
        }
        return text.toString();
    }

    private static char letter() {
        return LETTERS.charAt(random(LETTERS.length()));
    }

    private static int random(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }
}
