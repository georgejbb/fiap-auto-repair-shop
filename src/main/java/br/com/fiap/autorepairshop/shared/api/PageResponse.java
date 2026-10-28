package br.com.fiap.autorepairshop.shared.api;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Página de resultados de uma listagem.
 *
 * @param items      itens da página
 * @param page       número da página, a partir de 0
 * @param size       tamanho da página solicitado
 * @param totalItems total de itens em todas as páginas
 */
@Schema(description = "Página de resultados de uma listagem")
public record PageResponse<T>(List<T> items, int page, int size, long totalItems) {
}
