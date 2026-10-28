package br.com.fiap.autorepairshop.workorder.infrastructure;

import br.com.fiap.autorepairshop.workorder.domain.WorkOrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "work_order_status_change")
class StatusChangeEntity {

    @Id
    UUID id;

    @Column(nullable = false)
    int position;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    WorkOrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    WorkOrderStatus toStatus;

    @Column(name = "changed_at", nullable = false)
    Instant changedAt;
}
