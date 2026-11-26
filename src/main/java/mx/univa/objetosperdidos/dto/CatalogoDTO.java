package mx.univa.objetosperdidos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import mx.univa.objetosperdidos.entity.Color;
import mx.univa.objetosperdidos.entity.Etiqueta;
import mx.univa.objetosperdidos.entity.Modulo;
import mx.univa.objetosperdidos.entity.TipoObjeto;

/** Elemento de un catálogo. Solo se envían los campos que tiene cada catálogo. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CatalogoDTO(
        Long id,
        String nombre,
        String descripcion,
        String codigoHex,
        String ubicacion,
        String horarioAtencion) {

    public static CatalogoDTO de(TipoObjeto t) {
        return new CatalogoDTO(t.getId(), t.getNombre(), t.getDescripcion(), null, null, null);
    }

    public static CatalogoDTO de(Color c) {
        return new CatalogoDTO(c.getId(), c.getNombre(), null, c.getCodigoHex(), null, null);
    }

    public static CatalogoDTO de(Etiqueta e) {
        return new CatalogoDTO(e.getId(), e.getNombre(), null, null, null, null);
    }

    public static CatalogoDTO de(Modulo m) {
        return new CatalogoDTO(m.getId(), m.getNombre(), null, null, m.getUbicacion(), m.getHorarioAtencion());
    }
}
