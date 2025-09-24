package br.com.nfe.processor.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nfe.processor.core.domain.valueobject.Cnpj;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CnpjTest {

    @Test
    @DisplayName("Deve aceitar CNPJ válido e normalizar dígitos")
    void shouldAcceptValidCnpj() {
        Cnpj cnpj = new Cnpj("11.222.333/0001-81");

        assertThat(cnpj.value()).isEqualTo("11222333000181");
        assertThat(cnpj.getFormatted()).isEqualTo("11.222.333/0001-81");
        assertThat(cnpj.toString()).isEqualTo("11.222.333/0001-81");
    }

    @Test
    @DisplayName("Deve rejeitar CNPJ com dígitos verificadores inválidos")
    void shouldRejectInvalidDigits() {
        assertThatThrownBy(() -> new Cnpj("11.222.333/0001-80"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dígitos verificadores incorretos");
    }

    @Test
    @DisplayName("Deve rejeitar sequência repetida de dígitos")
    void shouldRejectRepeatedSequence() {
        assertThatThrownBy(() -> new Cnpj("11.111.111/1111-11"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sequência repetida");
    }
}

