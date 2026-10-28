package br.com.fiap.autorepairshop.shared.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;

/**
 * Relógio da aplicação. Injetado onde o domínio depende da data atual, para que os testes fixem o instante.
 */
@ApplicationScoped
public class ClockProducer {

    @Produces
    @ApplicationScoped
    Clock clock() {
        return Clock.systemUTC();
    }
}
