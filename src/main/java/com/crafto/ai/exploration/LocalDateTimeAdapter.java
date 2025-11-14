package com.crafto.ai.exploration;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gson адаптер для сериализации/десериализации LocalDateTime
 * Обеспечивает консистентный JSON формат для полей даты/времени
 */
public class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    @Override
    public JsonElement serialize(LocalDateTime localDateTime, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(localDateTime.format(FORMATTER));
    }
    
    @Override
    public LocalDateTime deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) 
            throws JsonParseException {
        try {
            return LocalDateTime.parse(jsonElement.getAsString(), FORMATTER);
        } catch (Exception e) {
            throw new JsonParseException("Не удалось разобрать LocalDateTime: " + jsonElement.getAsString(), e);
        }
    }
}