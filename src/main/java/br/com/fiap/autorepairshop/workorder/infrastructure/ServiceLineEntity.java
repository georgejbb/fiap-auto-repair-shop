package br.com.fiap.autorepairshop.workorder.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "work_order_service_line")
class ServiceLineEntity {

    @Id
    UUID id;

    @Column(name = "service_offering_id", nullable = false)
    UUID serviceOfferingId;

    @Column(nullable = false)
    String name;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal price;
}
