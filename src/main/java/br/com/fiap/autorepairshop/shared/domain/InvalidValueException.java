package br.com.fiap.autorepairshop.shared.domain;

public class InvalidValueException extends DomainException {

    private final String field;

    public InvalidValueException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
