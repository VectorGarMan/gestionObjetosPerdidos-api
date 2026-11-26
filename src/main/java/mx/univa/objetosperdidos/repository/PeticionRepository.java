package mx.univa.objetosperdidos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import mx.univa.objetosperdidos.entity.EstadoPeticion;
import mx.univa.objetosperdidos.entity.Peticion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PeticionRepository extends JpaRepository<Peticion, Long> {

    Optional<Peticion> findByIdAndEstatusTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Peticion p where p.id = :id")
    Optional<Peticion> findByIdParaActualizar(@Param("id") Long id);

    @Query("select p.objeto.id from Peticion p where p.id = :id and p.estatus = true")
    Optional<Long> findObjetoIdDePeticionActiva(@Param("id") Long id);

    @EntityGraph(attributePaths = {"objeto", "solicitante"})
    List<Peticion> findByEstatusTrueOrderByFechaRegistroDesc();

    @EntityGraph(attributePaths = {"objeto", "solicitante"})
    List<Peticion> findBySolicitanteMatriculaIgnoreCaseAndEstatusTrueOrderByFechaRegistroDesc(String matricula);

    boolean existsByObjetoIdAndSolicitanteIdAndEstadoAndEstatusTrue(
            Long objetoId, Long solicitanteId, EstadoPeticion estado);

    List<Peticion> findByObjetoIdAndEstadoAndEstatusTrue(Long objetoId, EstadoPeticion estado);

    /**
     * Última entrega del objeto anterior a la fecha indicada: identifica a la persona
     * que tenía el objeto cuando se levantó (o se resolvió) un reclamo de error.
     */
    @EntityGraph(attributePaths = {"solicitante"})
    Optional<Peticion> findFirstByObjetoIdAndEstadoAndIdNotAndFechaLiberacionBeforeOrderByFechaLiberacionDesc(
            Long objetoId, EstadoPeticion estado, Long idExcluido, LocalDateTime antesDe);
}
