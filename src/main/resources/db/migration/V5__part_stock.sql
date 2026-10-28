-- Contexto Catálogo e Estoque: peças, insumos e movimentações de estoque.
-- O saldo nunca fica negativo (CHECK) e a coluna version detecta alterações concorrentes no mesmo item.

CREATE TABLE part (
    id             UUID          PRIMARY KEY,
    name           VARCHAR(120)  NOT NULL,
    category       VARCHAR(10)   NOT NULL CHECK (category IN ('PART', 'SUPPLY')),
    unit_price     NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    stock_quantity INTEGER       NOT NULL CHECK (stock_quantity >= 0),
    version        BIGINT        NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    deleted_at     TIMESTAMPTZ
);

CREATE TABLE stock_movement (
    id          UUID        PRIMARY KEY,
    part_id     UUID        NOT NULL REFERENCES part (id),
    type        VARCHAR(20) NOT NULL CHECK (type IN ('INBOUND', 'ADJUSTMENT')),
    quantity    INTEGER     NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_stock_movement_part ON stock_movement (part_id, occurred_at DESC);
