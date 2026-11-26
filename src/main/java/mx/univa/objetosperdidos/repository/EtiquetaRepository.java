package mx.univa.objetosperdidos.repository;

import java.util.List;
import java.util.Optional;

import mx.univa.objetosperdidos.entity.Etiqueta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtiquetaRepository extends JpaRepository<Etiqueta, Long> {

    List<Etiqueta> findByEstatusTrueOrderByNombreAsc();

    Optional<Etiqueta> findByIdAndEstatusTrue(Long id);

    List<Etiqueta> findByIdInAndEstatusTrue(java.util.Collection<Long> ids);
}
