package mx.univa.objetosperdidos.dto;

import jakarta.validation.constraints.NotNull;
import mx.univa.objetosperdidos.entity.TipoPeticion;

/**
 * PeticionDTO de entrada (F-03). El tipo es opcional: si se envía y ya no coincide con el
 * estado del objeto, la API responde «Este objeto cambió de estado...». La matrícula se toma
 * del token del estudiante identificado, por lo que no se recibe en el cuerpo.
 */
public record PeticionRequest(
        @NotNull(message = "Indica el objeto que deseas reclamar.") Long objetoId,
        TipoPeticion tipo) {
}
