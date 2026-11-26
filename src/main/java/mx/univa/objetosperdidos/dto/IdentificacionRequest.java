package mx.univa.objetosperdidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mx.univa.objetosperdidos.exception.Mensajes;

/** IdentificacionDTO de la especificación (PNT-01). */
public record IdentificacionRequest(
        @NotBlank(message = Mensajes.CORREO_VACIO)
        @Size(max = 100, message = Mensajes.CORREO_INVALIDO)
        @Pattern(regexp = "^$|^\\s*[^@\\s]+@[^@\\s]+\\.[^@\\s]+\\s*$", message = Mensajes.CORREO_INVALIDO)
        String correo) {
}
