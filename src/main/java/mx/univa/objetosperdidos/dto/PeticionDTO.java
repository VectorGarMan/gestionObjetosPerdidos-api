package mx.univa.objetosperdidos.dto;

import java.time.LocalDateTime;

import mx.univa.objetosperdidos.entity.EstadoPeticion;
import mx.univa.objetosperdidos.entity.Peticion;
import mx.univa.objetosperdidos.entity.TipoPeticion;

/** Fila de la lista de peticiones (PNT-08) y respuesta al crear una petición. */
public record PeticionDTO(
        Long id,
        TipoPeticion tipo,
        EstadoPeticion estado,
        Long objetoId,
        String objetoNombre,
        String matricula,
        String solicitanteNombre,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaLiberacion) {

    public static PeticionDTO de(Peticion p) {
        return new PeticionDTO(p.getId(), p.getTipo(), p.getEstado(), p.getObjeto().getId(),
                p.getObjeto().getNombre(), p.getSolicitante().getMatricula(), p.getSolicitante().getNombre(),
                p.getFechaRegistro(), p.getFechaLiberacion());
    }
}
