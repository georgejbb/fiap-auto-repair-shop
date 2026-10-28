package br.com.fiap.autorepairshop.shared.domain;

/**
 * Agregado inexistente ou excluído logicamente.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
