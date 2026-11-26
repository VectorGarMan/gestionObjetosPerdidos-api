package mx.univa.objetosperdidos.dto;

import java.time.LocalDateTime;

import mx.univa.objetosperdidos.entity.EstadoPeticion;
import mx.univa.objetosperdidos.entity.Peticion;
import mx.univa.objetosperdidos.entity.TipoPeticion;

/** Detalle completo de una petición para el administrador (PNT-08). */
public record PeticionDetalleDTO(
        Long id,
        TipoPeticion tipo,
        EstadoPeticion estado,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaLiberacion,
        ObjetoDTO objeto,
        PersonaDTO solicitante,
        PersonaDTO liberadaPor) {

    public static PeticionDetalleDTO de(Peticion p) {
        return new PeticionDetalleDTO(p.getId(), p.getTipo(), p.getEstado(), p.getFechaRegistro(),
                p.getFechaLiberacion(), ObjetoDTO.de(p.getObjeto()), PersonaDTO.de(p.getSolicitante()),
                p.getLiberadaPor() == null ? null : PersonaDTO.de(p.getLiberadaPor()));
    }
}
