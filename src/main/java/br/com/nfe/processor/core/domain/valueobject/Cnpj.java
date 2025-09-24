package br.com.nfe.processor.core.domain.valueobject;

import java.util.Objects;

/**
 * Value object that encapsulates a Brazilian CNPJ number with full validation of
 * formatting, repeated sequences and verification digits.
 */
public record Cnpj(String value) {

    private static final int CNPJ_LENGTH = 14;
    private static final int[] FIRST_DIGIT_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_DIGIT_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public Cnpj {
        Objects.requireNonNull(value, "CNPJ não pode ser nulo");

        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() != CNPJ_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("CNPJ deve ter %d dígitos. Encontrado: %d", CNPJ_LENGTH, digits.length()));
        }
        if (isRepeatedSequence(digits)) {
            throw new IllegalArgumentException("CNPJ não pode ser uma sequência repetida de dígitos");
        }
        if (!isValidCnpj(digits)) {
            throw new IllegalArgumentException("CNPJ inválido: dígitos verificadores incorretos");
        }
        value = digits;
    }

    public String formatted() {
        return String.format(
                "%s.%s.%s/%s-%s",
                value.substring(0, 2),
                value.substring(2, 5),
                value.substring(5, 8),
                value.substring(8, 12),
                value.substring(12, 14));
    }

    private static boolean isRepeatedSequence(String cnpj) {
        char firstChar = cnpj.charAt(0);
        for (int i = 1; i < cnpj.length(); i++) {
            if (cnpj.charAt(i) != firstChar) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidCnpj(String cnpj) {
        int firstDigit = calculateDigit(cnpj.substring(0, 12), FIRST_DIGIT_WEIGHTS);
        int secondDigit = calculateDigit(cnpj.substring(0, 13), SECOND_DIGIT_WEIGHTS);
        return firstDigit == Character.getNumericValue(cnpj.charAt(12))
                && secondDigit == Character.getNumericValue(cnpj.charAt(13));
    }

    private static int calculateDigit(String base, int[] weights) {
        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            sum += Character.getNumericValue(base.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    @Override
    public String toString() {
        return value;
    }
}
