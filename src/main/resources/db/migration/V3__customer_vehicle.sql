-- Contexto Cadastro: clientes e veículos.
-- A exclusão é lógica (deleted_at); a unicidade de documento e placa vale apenas entre registros ativos.

CREATE TABLE customer (
    id            UUID         PRIMARY KEY,
    document_type VARCHAR(4)   NOT NULL CHECK (document_type IN ('CPF', 'CNPJ')),
    document      VARCHAR(14)  NOT NULL,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(254) NOT NULL,
    phone         VARCHAR(30),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    deleted_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_customer_document_active ON customer (document) WHERE deleted_at IS NULL;

CREATE TABLE vehicle (
    id            UUID        PRIMARY KEY,
    license_plate VARCHAR(7)  NOT NULL,
    brand         VARCHAR(60) NOT NULL,
    model         VARCHAR(60) NOT NULL,
    year          INTEGER     NOT NULL,
    owner_id      UUID        NOT NULL REFERENCES customer (id),
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    deleted_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_vehicle_license_plate_active ON vehicle (license_plate) WHERE deleted_at IS NULL;
CREATE INDEX ix_vehicle_owner ON vehicle (owner_id);
