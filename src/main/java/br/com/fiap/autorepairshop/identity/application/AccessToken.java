package br.com.fiap.autorepairshop.identity.application;

/**
 * Token de acesso emitido no login.
 *
 * @param value     JWT assinado
 * @param expiresIn validade em segundos
 */
public record AccessToken(String value, long expiresIn) {
}
