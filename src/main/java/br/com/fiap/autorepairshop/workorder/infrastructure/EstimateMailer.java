package br.com.fiap.autorepairshop.workorder.infrastructure;

import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.workorder.application.EstimateIssued;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import java.text.NumberFormat;
import java.util.Locale;
import org.jboss.logging.Logger;

/**
 * Envia ao cliente o e-mail do orçamento.
 *
 * <p>Observa {@link EstimateIssued} somente depois da confirmação da transação que gerou o orçamento: se a
 * gravação falhar, nenhum e-mail sai. Uma falha no envio é registrada no log e não desfaz o orçamento, que
 * continua disponível na consulta do cliente.
 */
@ApplicationScoped
class EstimateMailer {

    private static final Logger LOG = Logger.getLogger(EstimateMailer.class);
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final Mailer mailer;

    EstimateMailer(Mailer mailer) {
        this.mailer = mailer;
    }

    void onEstimateIssued(@Observes(during = TransactionPhase.AFTER_SUCCESS) EstimateIssued estimate) {
        try {
            mailer.send(Mail.withText(estimate.customerEmail(), subject(estimate), body(estimate)));
        } catch (RuntimeException e) {
            LOG.errorf(e, "Falha ao enviar o e-mail do orçamento da OS %s", estimate.workOrderCode());
        }
    }

    static String subject(EstimateIssued estimate) {
        return "Orçamento do veículo " + estimate.licensePlate() + " aguardando sua aprovação";
    }

    static String body(EstimateIssued estimate) {
        StringBuilder text = new StringBuilder()
                .append("Olá, ").append(estimate.customerName()).append(".\n\n")
                .append("O orçamento do serviço no veículo ").append(estimate.licensePlate())
                .append(" está disponível para sua aprovação.\n\n")
                .append("Itens:\n");
        for (EstimateIssued.Item item : estimate.items()) {
            text.append("- ").append(item.description()).append(" (").append(item.quantity()).append(" x): ")
                    .append(format(item.amount())).append('\n');
        }
        return text.append("\nTotal: ").append(format(estimate.total())).append("\n\n")
                .append("Código da ordem de serviço: ").append(estimate.workOrderCode()).append("\n\n")
                .append("Para acompanhar, aprovar ou rejeitar o orçamento, informe esse código e o seu CPF ou CNPJ ")
                .append("na consulta de ordens de serviço da oficina.\n")
                .toString();
    }

    private static String format(Money money) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(money.amount());
    }
}
