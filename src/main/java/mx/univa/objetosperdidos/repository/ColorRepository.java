package mx.univa.objetosperdidos.repository;

import java.util.List;
import java.util.Optional;

import mx.univa.objetosperdidos.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColorRepository extends JpaRepository<Color, Long> {

    List<Color> findByEstatusTrueOrderByNombreAsc();

    Optional<Color> findByIdAndEstatusTrue(Long id);
}
