package br.com.fiap.autorepairshop.workorder.infrastructure;

import br.com.fiap.autorepairshop.workorder.domain.EstimateDecision;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "work_order")
class WorkOrderEntity {

    @Id
    UUID id;

    @Column(name = "customer_id", nullable = false)
    UUID customerId;

    @Column(name = "vehicle_id", nullable = false)
    UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    WorkOrderStatus status;

    @Column(name = "estimate_total", precision = 12, scale = 2)
    BigDecimal estimateTotal;

    @Column(name = "estimate_generated_at")
    Instant estimateGeneratedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "estimate_decision")
    EstimateDecision estimateDecision;

    @Column(name = "estimate_decided_at")
    Instant estimateDecidedAt;

    @Version
    long version;

    @Column(name = "opened_at", nullable = false)
    Instant openedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "work_order_id", nullable = false)
    List<ServiceLineEntity> serviceLines = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "work_order_id", nullable = false)
    List<PartLineEntity> partLines = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "work_order_id", nullable = false)
    @OrderBy("position")
    List<StatusChangeEntity> statusHistory = new ArrayList<>();
}
