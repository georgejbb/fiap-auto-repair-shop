package br.com.fiap.autorepairshop.shared.domain;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
