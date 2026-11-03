package br.com.fiap.autorepairshop.shared.domain;

import java.util.List;
import java.util.function.Function;

public record Page<T>(List<T> items, long total) {

    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(items.stream().map(mapper).toList(), total);
    }
}
