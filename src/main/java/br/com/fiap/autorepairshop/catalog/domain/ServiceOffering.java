package br.com.fiap.autorepairshop.catalog.domain;

import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import br.com.fiap.autorepairshop.shared.domain.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Serviço de mão de obra oferecido pela oficina, com o preço usado no orçamento.
 */
public final class ServiceOffering {

    private final UUID id;
    private String name;
    private String description;
    private Money price;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public ServiceOffering(UUID id, String name, String description, Money price, Instant createdAt,
            Instant updatedAt, Instant deletedAt) {
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.description = description;
        this.price = Objects.requireNonNull(price);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.deletedAt = deletedAt;
    }

    public static ServiceOffering create(String name, String description, Money price, Instant now) {
        requireName(name);
        return new ServiceOffering(UUID.randomUUID(), name.strip(), blankToNull(description),
                Objects.requireNonNull(price), now, now, null);
    }

    public void update(String name, String description, Money price, Instant now) {
        requireName(name);
        this.name = name.strip();
        this.description = blankToNull(description);
        this.price = Objects.requireNonNull(price);
        this.updatedAt = now;
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Money price() {
        return price;
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
