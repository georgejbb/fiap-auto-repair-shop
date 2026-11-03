package br.com.fiap.autorepairshop.shared.domain;

public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
