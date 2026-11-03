package br.com.fiap.autorepairshop.shared.api;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Página de resultados de uma listagem")
public record PageResponse<T>(List<T> items, int page, int size, long totalItems) {
}
