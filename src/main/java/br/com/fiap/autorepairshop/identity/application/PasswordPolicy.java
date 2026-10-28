package br.com.fiap.autorepairshop.identity.application;

final class PasswordPolicy {

    /** Custo do bcrypt: 2^12 iterações, equilíbrio entre resistência a força bruta e tempo de login. */
    static final int BCRYPT_COST = 12;

    private PasswordPolicy() {
    }
}
