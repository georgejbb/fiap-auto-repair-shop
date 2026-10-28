package br.com.fiap.autorepairshop.support;

import io.smallrye.jwt.build.Jwt;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Set;

/**
 * Tokens para os testes de integração, assinados com a chave gerada pelo Quarkus no perfil de teste.
 */
public final class TestTokens {

    private TestTokens() {
    }

    public static String admin() {
        return Jwt.upn("admin").groups(Set.of("ADMIN")).expiresIn(600).sign();
    }

    public static String withoutAdminRole() {
        return Jwt.upn("visitante").groups(Set.of("GUEST")).expiresIn(600).sign();
    }

    public static String expired() {
        Instant past = Instant.now().minusSeconds(7200);
        return Jwt.upn("admin").groups(Set.of("ADMIN")).issuedAt(past).expiresAt(past.plusSeconds(60)).sign();
    }

    public static String signedWithUnknownKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return Jwt.upn("admin").groups(Set.of("ADMIN")).expiresIn(600)
                    .sign(generator.generateKeyPair().getPrivate());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
