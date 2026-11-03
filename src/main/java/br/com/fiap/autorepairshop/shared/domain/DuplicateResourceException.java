package br.com.fiap.autorepairshop.shared.domain;

public class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
