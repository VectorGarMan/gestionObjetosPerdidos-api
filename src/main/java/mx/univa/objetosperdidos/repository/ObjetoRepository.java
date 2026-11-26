package mx.univa.objetosperdidos.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import mx.univa.objetosperdidos.entity.Objeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObjetoRepository extends JpaRepository<Objeto, Long>, ObjetoBusquedaRepository {

    Optional<Objeto> findByIdAndEstatusTrue(Long id);

    /**
     * Bloquea la fila (SELECT ... FOR UPDATE) para que dos administradores no
     * liberen, editen o eliminen el mismo objeto al mismo tiempo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Objeto o where o.id = :id")
    Optional<Objeto> findByIdParaActualizar(@Param("id") Long id);
}
