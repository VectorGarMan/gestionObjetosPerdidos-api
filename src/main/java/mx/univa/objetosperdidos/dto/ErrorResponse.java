package mx.univa.objetosperdidos.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Formato único de error. «mensaje» es el texto para el usuario; «campos» detalla errores por campo. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String mensaje,
        Map<String, String> campos,
        LocalDateTime timestamp) {
}
