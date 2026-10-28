package br.com.fiap.autorepairshop.shared.domain;

import java.util.List;
import java.util.function.Function;

/**
 * Resultado paginado de uma consulta de repositório.
 *
 * @param items itens da página
 * @param total total de itens em todas as páginas
 */
public record Page<T>(List<T> items, long total) {

    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(items.stream().map(mapper).toList(), total);
    }
}
