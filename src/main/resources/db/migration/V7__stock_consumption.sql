-- Baixa de estoque pela aprovação do orçamento: novo tipo de movimentação vinculado à OS de origem.

ALTER TABLE stock_movement ADD COLUMN work_order_id UUID REFERENCES work_order (id);

ALTER TABLE stock_movement DROP CONSTRAINT stock_movement_type_check;
ALTER TABLE stock_movement ADD CONSTRAINT stock_movement_type_check
    CHECK (type IN ('INBOUND', 'ADJUSTMENT', 'CONSUMPTION'));

-- Toda baixa referencia a OS; entradas e ajustes não referenciam.
ALTER TABLE stock_movement ADD CONSTRAINT ck_stock_movement_consumption_work_order
    CHECK ((type = 'CONSUMPTION') = (work_order_id IS NOT NULL));
