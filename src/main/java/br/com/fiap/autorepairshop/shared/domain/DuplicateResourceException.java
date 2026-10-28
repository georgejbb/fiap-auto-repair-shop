package br.com.fiap.autorepairshop.shared.domain;

/**
 * Identificador único (documento, placa) já usado por outro registro ativo.
 */
public class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
