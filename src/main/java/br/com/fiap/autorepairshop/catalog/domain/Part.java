package br.com.fiap.autorepairshop.catalog.domain;

import br.com.fiap.autorepairshop.shared.domain.BusinessRuleException;
import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import br.com.fiap.autorepairshop.shared.domain.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Peça ou insumo controlado em estoque. O saldo nunca fica negativo e só muda por movimentação:
 * cada alteração devolve a {@link StockMovement} que a registra.
 */
public final class Part {

    private final UUID id;
    private String name;
    private PartCategory category;
    private Money unitPrice;
    private int stockQuantity;
    private final long version;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public Part(UUID id, String name, PartCategory category, Money unitPrice, int stockQuantity, long version,
            Instant createdAt, Instant updatedAt, Instant deletedAt) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Estoque negativo: " + stockQuantity);
        }
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.category = Objects.requireNonNull(category);
        this.unitPrice = Objects.requireNonNull(unitPrice);
        this.stockQuantity = stockQuantity;
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.deletedAt = deletedAt;
    }

    /** Cadastra o item com estoque zero; o saldo inicial entra por uma entrada de estoque. */
    public static Part create(String name, PartCategory category, Money unitPrice, Instant now) {
        requireName(name);
        requireCategory(category);
        return new Part(UUID.randomUUID(), name.strip(), category, Objects.requireNonNull(unitPrice), 0, 0, now, now,
                null);
    }

    /** Altera os dados cadastrais; o estoque não é alterado por este caminho. */
    public void update(String name, PartCategory category, Money unitPrice, Instant now) {
        requireName(name);
        requireCategory(category);
        this.name = name.strip();
        this.category = category;
        this.unitPrice = Objects.requireNonNull(unitPrice);
        this.updatedAt = now;
    }

    public StockMovement registerInbound(int quantity, Instant now) {
        if (quantity <= 0) {
            throw new InvalidValueException("quantity", "deve ser maior que zero");
        }
        stockQuantity += quantity;
        updatedAt = now;
        return StockMovement.of(id, StockMovementType.INBOUND, quantity, now);
    }

    /** Fixa o saldo na quantidade contada e registra a diferença em relação ao saldo anterior. */
    public StockMovement adjustTo(int countedQuantity, Instant now) {
        if (countedQuantity < 0) {
            throw new InvalidValueException("countedQuantity", "deve ser maior ou igual a zero");
        }
        int delta = countedQuantity - stockQuantity;
        stockQuantity = countedQuantity;
        updatedAt = now;
        return StockMovement.of(id, StockMovementType.ADJUSTMENT, delta, now);
    }

    /**
     * Baixa a quantidade usada por uma ordem de serviço cujo orçamento foi aprovado.
     *
     * @throws BusinessRuleException se o estoque atual não cobrir a quantidade
     */
    public StockMovement consume(int quantity, UUID workOrderId, Instant now) {
        if (quantity <= 0) {
            throw new InvalidValueException("quantity", "deve ser maior que zero");
        }
        if (quantity > stockQuantity) {
            throw new BusinessRuleException("Estoque insuficiente de " + name + ": a ordem de serviço precisa de "
                    + quantity + " unidade(s) e há " + stockQuantity + " em estoque.");
        }
        stockQuantity -= quantity;
        updatedAt = now;
        return new StockMovement(UUID.randomUUID(), id, StockMovementType.CONSUMPTION, -quantity, now,
                Objects.requireNonNull(workOrderId));
    }

    public void delete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    private static void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("name", "é obrigatório");
        }
    }

    private static void requireCategory(PartCategory category) {
        if (category == null) {
            throw new InvalidValueException("category", "é obrigatória");
        }
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public PartCategory category() {
        return category;
    }

    public Money unitPrice() {
        return unitPrice;
    }

    public int stockQuantity() {
        return stockQuantity;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant deletedAt() {
        return deletedAt;
    }
}
