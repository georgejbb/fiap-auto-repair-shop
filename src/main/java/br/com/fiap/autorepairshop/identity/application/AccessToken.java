package br.com.fiap.autorepairshop.identity.application;

public record AccessToken(String value, long expiresIn) {
}
