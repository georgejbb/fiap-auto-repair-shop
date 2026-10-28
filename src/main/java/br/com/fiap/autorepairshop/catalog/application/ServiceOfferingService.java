package br.com.fiap.autorepairshop.catalog.application;

import br.com.fiap.autorepairshop.catalog.domain.ServiceOffering;
import br.com.fiap.autorepairshop.catalog.domain.ServiceOfferingRepository;
import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.shared.domain.Page;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Casos de uso do catálogo de serviços.
 */
@ApplicationScoped
public class ServiceOfferingService {

    private final ServiceOfferingRepository offerings;
    private final Clock clock;

    public ServiceOfferingService(ServiceOfferingRepository offerings, Clock clock) {
        this.offerings = offerings;
        this.clock = clock;
    }

    @Transactional
    public ServiceOffering create(String name, String description, BigDecimal price) {
        ServiceOffering offering = ServiceOffering.create(name, description, Money.of("price", price), now());
        offerings.add(offering);
        return offering;
    }

    public ServiceOffering get(UUID id) {
        return offerings.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado."));
    }

    public Page<ServiceOffering> list(int page, int size) {
        return offerings.listActive(page, size);
    }

    @Transactional
    public ServiceOffering update(UUID id, String name, String description, BigDecimal price) {
        ServiceOffering offering = get(id);
        offering.update(name, description, Money.of("price", price), now());
        offerings.update(offering);
        return offering;
    }

    @Transactional
    public void delete(UUID id) {
        ServiceOffering offering = get(id);
        offering.delete(now());
        offerings.update(offering);
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
