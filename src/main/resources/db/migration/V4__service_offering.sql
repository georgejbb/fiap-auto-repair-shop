-- Contexto Catálogo e Estoque: serviços de mão de obra oferecidos pela oficina.
CREATE TABLE service_offering (
    id          UUID          PRIMARY KEY,
    name        VARCHAR(120)  NOT NULL,
    description VARCHAR(500),
    price       NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    deleted_at  TIMESTAMPTZ
);
