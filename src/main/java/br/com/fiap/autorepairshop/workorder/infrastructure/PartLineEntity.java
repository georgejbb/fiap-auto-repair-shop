package br.com.fiap.autorepairshop.workorder.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "work_order_part_line")
class PartLineEntity {

    @Id
    UUID id;

    @Column(name = "part_id", nullable = false)
    UUID partId;

    @Column(nullable = false)
    String name;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    BigDecimal unitPrice;

    @Column(nullable = false)
    int quantity;
}
