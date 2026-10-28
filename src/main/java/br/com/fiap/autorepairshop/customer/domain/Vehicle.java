package br.com.fiap.autorepairshop.customer.domain;

import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * Veículo atendido pela oficina, vinculado a um cliente. A placa identifica o veículo e não muda.
 */
public final class Vehicle {

    static final int FIRST_ACCEPTED_YEAR = 1900;

    private final UUID id;
    private final LicensePlate licensePlate;
    private String brand;
    private String model;
    private int year;
    private UUID ownerId;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public Vehicle(UUID id, LicensePlate licensePlate, String brand, String model, int year, UUID ownerId,
            Instant createdAt, Instant updatedAt, Instant deletedAt) {
        this.id = Objects.requireNonNull(id);
        this.licensePlate = Objects.requireNonNull(licensePlate);
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.ownerId = Objects.requireNonNull(ownerId);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.deletedAt = deletedAt;
    }

    /**
     * Cadastra o veículo. O ano é aceito entre 1900 e o ano seguinte ao de {@code now}, porque modelos
     * do ano seguinte já são vendidos no ano corrente.
     */
    public static Vehicle register(LicensePlate licensePlate, String brand, String model, int year, UUID ownerId,
            Instant now) {
        requireText("brand", brand);
        requireText("model", model);
        requireValidYear(year, now);
        Objects.requireNonNull(ownerId, "ownerId");
        return new Vehicle(UUID.randomUUID(), licensePlate, brand.strip(), model.strip(), year, ownerId, now, now,
                null);
    }

    public void update(String brand, String model, int year, UUID ownerId, Instant now) {
        requireText("brand", brand);
        requireText("model", model);
        requireValidYear(year, now);
        this.brand = brand.strip();
        this.model = model.strip();
        this.year = year;
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.updatedAt = now;
    }

    public void delete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    private static void requireValidYear(int year, Instant now) {
        int lastAcceptedYear = now.atZone(ZoneOffset.UTC).getYear() + 1;
        if (year < FIRST_ACCEPTED_YEAR || year > lastAcceptedYear) {
            throw new InvalidValueException("year",
                    "deve estar entre " + FIRST_ACCEPTED_YEAR + " e " + lastAcceptedYear);
        }
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException(field, "é obrigatório");
        }
    }

    public UUID id() {
        return id;
    }

    public LicensePlate licensePlate() {
        return licensePlate;
    }

    public String brand() {
        return brand;
    }

    public String model() {
        return model;
    }

    public int year() {
        return year;
    }

    public UUID ownerId() {
        return ownerId;
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
