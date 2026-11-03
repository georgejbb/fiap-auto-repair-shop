package br.com.fiap.autorepairshop.catalog.domain;

import br.com.fiap.autorepairshop.shared.domain.Page;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOfferingRepository {

    Optional<ServiceOffering> findActiveById(UUID id);

    Page<ServiceOffering> listActive(int page, int size);

    void add(ServiceOffering offering);

    void update(ServiceOffering offering);
}
