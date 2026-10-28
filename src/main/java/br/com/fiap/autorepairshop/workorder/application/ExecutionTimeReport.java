package br.com.fiap.autorepairshop.workorder.application;

import java.util.List;
import java.util.UUID;

/**
 * Tempo de execução das ordens de serviço concluídas no período.
 *
 * @param executedCount  quantidade de OS executadas
 * @param averageSeconds tempo médio em segundos; nulo sem OS executadas
 * @param byService      média por serviço do catálogo presente nessas OS
 */
public record ExecutionTimeReport(long executedCount, Long averageSeconds, List<ServiceAverage> byService) {

    /**
     * Média de um serviço: quantas OS executadas o incluíam e o tempo médio dessas OS.
     */
    public record ServiceAverage(UUID serviceOfferingId, String name, long executedCount, long averageSeconds) {
    }
}
