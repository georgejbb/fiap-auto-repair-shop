package br.com.fiap.autorepairshop.workorder.application;

import br.com.fiap.autorepairshop.shared.domain.Money;
import java.util.List;
import java.util.UUID;

public record EstimateIssued(UUID workOrderCode, String customerName, String customerEmail, String licensePlate,
        List<Item> items, Money total) {

    public record Item(String description, int quantity, Money amount) {
    }
}
