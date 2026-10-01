package nogrunt.springboot.Controllers.component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(LocalDateTime localDateTime) {
        return Optional.ofNullable(localDateTime)
                .map(Timestamp::valueOf)  // Converts LocalDateTime to Timestamp
                .orElse(null);  // If null, return null
    }

    @Override
    public LocalDateTime convertToEntityAttribute(Timestamp timestamp) {
        return Optional.ofNullable(timestamp)
                .map(Timestamp::toLocalDateTime)  // Converts Timestamp to LocalDateTime
                .orElse(null);  // If null, return null
    }
}
