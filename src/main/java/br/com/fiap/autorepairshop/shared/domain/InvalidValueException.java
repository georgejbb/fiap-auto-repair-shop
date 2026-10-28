package br.com.fiap.autorepairshop.shared.domain;

/**
 * Valor rejeitado por um objeto de valor ou agregado, associado ao campo de entrada que o originou.
 */
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
