package br.com.fiap.autorepairshop.workorder.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.workorder.application.EstimateIssued;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EstimateMailerTest {

    private static final UUID CODE = UUID.fromString("3f1c2a9e-5b7d-4c1a-9e2f-0a1b2c3d4e5f");

    private final EstimateIssued estimate = new EstimateIssued(CODE, "Maria Silva", "maria@email.com", "BRA2E19",
            List.of(new EstimateIssued.Item("Troca de óleo", 1, money("120.00")),
                    new EstimateIssued.Item("Filtro de óleo", 2, money("71.00"))),
            money("191.00"));

    @Test
    void messageContainsCodePlateItemsAndTotal() {
        String body = EstimateMailer.body(estimate);

        assertThat(EstimateMailer.subject(estimate)).contains("BRA2E19");
        assertThat(body)
                .contains("Olá, Maria Silva.")
                .contains("Troca de óleo (1 x)")
                .contains("Filtro de óleo (2 x)")
                .contains("191,00")
                .contains(CODE.toString());
    }

    @Test
    void sendsToCustomerEmail() {
        Mailer mailer = mock(Mailer.class);

        new EstimateMailer(mailer).onEstimateIssued(estimate);

        ArgumentCaptor<Mail> mail = ArgumentCaptor.forClass(Mail.class);
        verify(mailer).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly("maria@email.com");
    }

    @Test
    void sendingFailureIsLoggedAndNotPropagated() {
        Mailer mailer = mock(Mailer.class);
        doThrow(new IllegalStateException("SMTP indisponível")).when(mailer).send(any(Mail[].class));

        assertThatCode(() -> new EstimateMailer(mailer).onEstimateIssued(estimate)).doesNotThrowAnyException();
    }

    private static Money money(String value) {
        return Money.of("price", new BigDecimal(value));
    }
}
