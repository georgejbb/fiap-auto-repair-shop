package br.com.fiap.autorepairshop.workorder.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderStatus;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkOrderFilterTest {

    @Test
    void parsesStatusesIgnoringCaseAndBlanks() {
        UUID customer = UUID.randomUUID();

        WorkOrderFilter filter = WorkOrderFilter.of(Arrays.asList("received", " IN_DIAGNOSIS ", "", null), customer);

        assertThat(filter.statuses()).containsExactly(WorkOrderStatus.RECEIVED, WorkOrderStatus.IN_DIAGNOSIS);
        assertThat(filter.customerId()).isEqualTo(customer);
    }

    @Test
    void absentStatusesMeanAll() {
        assertThat(WorkOrderFilter.of(null, null).statuses()).isEmpty();
        assertThat(WorkOrderFilter.of(List.of(), null).statuses()).isEmpty();
    }

    @Test
    void invalidStatusIsInputError() {
        assertThatThrownBy(() -> WorkOrderFilter.of(List.of("CANCELLED"), null))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("status"))
                .hasMessage("valor inválido: CANCELLED");
    }
}
