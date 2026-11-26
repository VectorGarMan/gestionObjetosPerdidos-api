package mx.univa.objetosperdidos.dto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import mx.univa.objetosperdidos.entity.EstadoObjeto;
import mx.univa.objetosperdidos.entity.Objeto;
import mx.univa.objetosperdidos.entity.ObjetoEtiqueta;

/**
 * Datos de la tarjeta y del detalle (PNT-03 a PNT-06 y PNT-09).
 * No incluye quién reclamó el objeto: el estudiante nunca debe verlo (F-03).
 */
public record ObjetoDTO(
        Long id,
        String nombre,
        String descripcion,
        String marca,
        String lugarPerdida,
        String rutaImagen,
        EstadoObjeto estado,
        CatalogoDTO tipoObjeto,
        CatalogoDTO color,
        CatalogoDTO modulo,
        List<CatalogoDTO> otrasEtiquetas,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaReclamo) {

    public static ObjetoDTO de(Objeto o) {
        List<CatalogoDTO> etiquetas = o.getEtiquetas().stream()
                .filter(ObjetoEtiqueta::isActivo)
                .map(ObjetoEtiqueta::getEtiqueta)
                .map(CatalogoDTO::de)
                .sorted(Comparator.comparing(CatalogoDTO::nombre))
                .toList();
        return new ObjetoDTO(o.getId(), o.getNombre(), o.getDescripcion(), o.getMarca(), o.getLugarPerdida(),
                o.getRutaImagen(), o.getEstado(), CatalogoDTO.de(o.getTipoObjeto()), CatalogoDTO.de(o.getColor()),
                CatalogoDTO.de(o.getModulo()), etiquetas, o.getFechaRegistro(), o.getFechaReclamo());
    }
}
