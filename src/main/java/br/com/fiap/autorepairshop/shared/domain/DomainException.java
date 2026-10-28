package br.com.fiap.autorepairshop.shared.domain;

/**
 * Base das exceções que representam violações de regras do domínio.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
