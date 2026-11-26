package mx.univa.objetosperdidos.repository;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import mx.univa.objetosperdidos.entity.EstadoObjeto;
import mx.univa.objetosperdidos.entity.Objeto;

/** Implementación con JPQL dinámico (Spring Data la detecta por el sufijo "Impl"). */
public class ObjetoBusquedaRepositoryImpl implements ObjetoBusquedaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Objeto> buscar(EstadoObjeto estado, Long tipoObjetoId, Long colorId, List<Long> etiquetaIds) {
        StringBuilder jpql = new StringBuilder("""
                select o from Objeto o
                join fetch o.tipoObjeto
                join fetch o.color
                join fetch o.modulo
                where o.estatus = true and o.estado = :estado""");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("estado", estado);

        if (tipoObjetoId != null) {
            jpql.append(" and o.tipoObjeto.id = :tipoObjetoId");
            parametros.put("tipoObjetoId", tipoObjetoId);
        }
        if (colorId != null) {
            jpql.append(" and o.color.id = :colorId");
            parametros.put("colorId", colorId);
        }
        Set<Long> etiquetas = etiquetaIds == null ? Set.of() : new LinkedHashSet<>(etiquetaIds.stream()
                .filter(Objects::nonNull).toList());
        if (!etiquetas.isEmpty()) {
            // El objeto debe tener activas todas las etiquetas seleccionadas
            jpql.append(" and (select count(distinct oe.etiqueta.id) from ObjetoEtiqueta oe")
                    .append(" where oe.objeto = o and oe.estatus = true and oe.etiqueta.id in :etiquetaIds)")
                    .append(" = :totalEtiquetas");
            parametros.put("etiquetaIds", etiquetas);
            parametros.put("totalEtiquetas", (long) etiquetas.size());
        }
        // Perdidos: el más reciente primero. Historial: el último reclamado primero.
        jpql.append(estado == EstadoObjeto.RECLAMADO
                ? " order by o.fechaReclamo desc, o.id desc"
                : " order by o.fechaRegistro desc, o.id desc");

        TypedQuery<Objeto> query = entityManager.createQuery(jpql.toString(), Objeto.class);
        parametros.forEach(query::setParameter);
        return query.getResultList();
    }
}
