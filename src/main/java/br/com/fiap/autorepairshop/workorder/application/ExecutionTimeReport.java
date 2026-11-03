package br.com.fiap.autorepairshop.workorder.application;

import java.util.List;
import java.util.UUID;

public record ExecutionTimeReport(long executedCount, Long averageSeconds, List<ServiceAverage> byService) {

    public record ServiceAverage(UUID serviceOfferingId, String name, long executedCount, long averageSeconds) {
    }
}
