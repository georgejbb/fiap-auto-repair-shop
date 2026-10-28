package br.com.fiap.autorepairshop.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", " 529 982 247 25 "})
    void acceptsValidCpfWithOrWithoutFormatting(String raw) {
        Document document = Document.of(raw);

        assertThat(document.type()).isEqualTo(DocumentType.CPF);
        assertThat(document.number()).isEqualTo("52998224725");
    }

    @ParameterizedTest
    @ValueSource(strings = {"11222333000181", "11.222.333/0001-81"})
    void acceptsValidNumericCnpj(String raw) {
        Document document = Document.of(raw);

        assertThat(document.type()).isEqualTo(DocumentType.CNPJ);
        assertThat(document.number()).isEqualTo("11222333000181");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.ABC.345/01DE-35", "12abc34501de35"})
    void acceptsValidAlphanumericCnpjInAnyCase(String raw) {
        Document document = Document.of(raw);

        assertThat(document.type()).isEqualTo(DocumentType.CNPJ);
        assertThat(document.number()).isEqualTo("12ABC34501DE35");
    }

    @ParameterizedTest
    @CsvSource({
            "52998224715, primeiro dígito verificador de CPF errado",
            "52998224724, segundo dígito verificador de CPF errado",
            "11111111111, CPF com todos os dígitos iguais",
            "11222333000191, primeiro dígito verificador de CNPJ errado",
            "11222333000182, segundo dígito verificador de CNPJ errado",
            "00000000000000, CNPJ com todos os dígitos iguais",
            "12ABC34501DE36, CNPJ alfanumérico com dígito errado",
            "12ABC34501DEA5, CNPJ alfanumérico com letra no dígito verificador",
            "5299822472, tamanho de CPF incompleto",
            "529982247251, tamanho entre CPF e CNPJ",
            "ABCDEFGHIJK, CPF com letras"
    })
    void rejectsInvalidDocuments(String raw, String reason) {
        assertThatThrownBy(() -> Document.of(raw))
                .as(reason)
                .isInstanceOfSatisfying(InvalidValueException.class,
                        e -> assertThat(e.field()).isEqualTo("document"))
                .hasMessage("não é um CPF ou CNPJ válido");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void requiresValue(String raw) {
        assertThatThrownBy(() -> Document.of(raw))
                .isInstanceOf(InvalidValueException.class)
                .hasMessage("é obrigatório");
    }

    @Test
    void checkDigitRemainderBelowTwoBecomesZero() {
        // Em 123.456.789-09 a soma ponderada do primeiro dígito deixa resto 1, que resulta em dígito 0.
        assertThat(Document.of("123.456.789-09").number()).isEqualTo("12345678909");
    }

    @Test
    void normalizeRemovesFormattingAndUppercases() {
        assertThat(Document.normalize("12.abc.345/01de-35")).isEqualTo("12ABC34501DE35");
    }
}
