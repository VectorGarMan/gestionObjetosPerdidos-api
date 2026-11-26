package mx.univa.objetosperdidos.repository;

import java.util.List;

import mx.univa.objetosperdidos.entity.EstadoObjeto;
import mx.univa.objetosperdidos.entity.Objeto;

/** Búsqueda con filtros opcionales de PNT-03, PNT-05 y PNT-09. */
public interface ObjetoBusquedaRepository {

    /**
     * Devuelve los objetos activos con el estado indicado. Los filtros nulos se ignoran.
     * Si se envían etiquetas, el objeto debe tener TODAS las seleccionadas.
     */
    List<Objeto> buscar(EstadoObjeto estado, Long tipoObjetoId, Long colorId, List<Long> etiquetaIds);
}
