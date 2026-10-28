package br.com.fiap.autorepairshop.shared.domain;

/**
 * Comando válido em formato, mas que viola uma regra de negócio no estado atual.
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
