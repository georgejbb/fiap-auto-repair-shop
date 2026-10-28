-- Contexto Ordem de Serviço: OS, itens com nome e preço copiados do catálogo, orçamento e histórico de status.

CREATE TABLE work_order (
    id                    UUID          PRIMARY KEY,
    customer_id           UUID          NOT NULL REFERENCES customer (id),
    vehicle_id            UUID          NOT NULL REFERENCES vehicle (id),
    status                VARCHAR(20)   NOT NULL CHECK (status IN
        ('RECEIVED', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'IN_PROGRESS', 'COMPLETED', 'DELIVERED')),
    estimate_total        NUMERIC(12,2) CHECK (estimate_total >= 0),
    estimate_generated_at TIMESTAMPTZ,
    estimate_decision     VARCHAR(10)   CHECK (estimate_decision IN ('APPROVED', 'REJECTED')),
    estimate_decided_at   TIMESTAMPTZ,
    version               BIGINT        NOT NULL,
    opened_at             TIMESTAMPTZ   NOT NULL
);

CREATE INDEX ix_work_order_status ON work_order (status);
CREATE INDEX ix_work_order_customer ON work_order (customer_id);

CREATE TABLE work_order_service_line (
    id                  UUID          PRIMARY KEY,
    work_order_id       UUID          NOT NULL REFERENCES work_order (id),
    service_offering_id UUID          NOT NULL REFERENCES service_offering (id),
    name                VARCHAR(120)  NOT NULL,
    price               NUMERIC(12,2) NOT NULL CHECK (price >= 0)
);

CREATE INDEX ix_work_order_service_line_order ON work_order_service_line (work_order_id);

CREATE TABLE work_order_part_line (
    id            UUID          PRIMARY KEY,
    work_order_id UUID          NOT NULL REFERENCES work_order (id),
    part_id       UUID          NOT NULL REFERENCES part (id),
    name          VARCHAR(120)  NOT NULL,
    unit_price    NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    quantity      INTEGER       NOT NULL CHECK (quantity > 0)
);

CREATE INDEX ix_work_order_part_line_order ON work_order_part_line (work_order_id);

-- O histórico é somente acrescentado; position preserva a ordem quando duas mudanças têm o mesmo instante.
CREATE TABLE work_order_status_change (
    id            UUID        PRIMARY KEY,
    work_order_id UUID        NOT NULL REFERENCES work_order (id),
    position      INTEGER     NOT NULL,
    from_status   VARCHAR(20),
    to_status     VARCHAR(20) NOT NULL,
    changed_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_work_order_status_change_position UNIQUE (work_order_id, position)
);
