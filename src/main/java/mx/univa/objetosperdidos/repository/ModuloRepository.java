package mx.univa.objetosperdidos.repository;

import java.util.List;
import java.util.Optional;

import mx.univa.objetosperdidos.entity.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuloRepository extends JpaRepository<Modulo, Long> {

    List<Modulo> findByEstatusTrueOrderByNombreAsc();

    Optional<Modulo> findByIdAndEstatusTrue(Long id);
}
