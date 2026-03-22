package ru.javaprac.bank.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClientTypeConverter implements AttributeConverter<ClientType, String> {

    @Override
    public String convertToDatabaseColumn(ClientType attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public ClientType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ClientType.fromDb(dbData);
    }
}
