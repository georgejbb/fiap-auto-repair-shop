package br.com.fiap.autorepairshop.customer.domain;

import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Cliente da oficina. O documento identifica o cliente e não muda depois do cadastro.
 */
public final class Customer {

    private final UUID id;
    private final Document document;
    private String name;
    private String email;
    private String phone;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public Customer(UUID id, Document document, String name, String email, String phone,
            Instant createdAt, Instant updatedAt, Instant deletedAt) {
        this.id = Objects.requireNonNull(id);
        this.document = Objects.requireNonNull(document);
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.deletedAt = deletedAt;
    }

    public static Customer register(Document document, String name, String email, String phone, Instant now) {
        requireText("name", name);
        requireText("email", email);
        return new Customer(UUID.randomUUID(), document, name.strip(), email.strip(), blankToNull(phone),
                now, now, null);
    }

    public void update(String name, String email, String phone, Instant now) {
        requireText("name", name);
        requireText("email", email);
        this.name = name.strip();
        this.email = email.strip();
        this.phone = blankToNull(phone);
        this.updatedAt = now;
    }

    /** Exclusão lógica: o cliente deixa de ser ativo, mas continua referenciado pelas ordens de serviço. */
    public void delete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException(field, "é obrigatório");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public UUID id() {
        return id;
    }

    public Document document() {
        return document;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public String phone() {
        return phone;
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
