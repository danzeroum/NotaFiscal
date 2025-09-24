package br.com.nfe.processor.core.domain.valueobject;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CnpjAttributeConverter implements AttributeConverter<Cnpj, String> {

    @Override
    public String convertToDatabaseColumn(Cnpj attribute) {
        return attribute != null ? attribute.value() : null;
    }

    @Override
    public Cnpj convertToEntityAttribute(String dbData) {
        return dbData != null ? new Cnpj(dbData) : null;
    }
}
