package mx.univa.objetosperdidos.repository;

import java.util.List;
import java.util.Optional;

import mx.univa.objetosperdidos.entity.TipoObjeto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoObjetoRepository extends JpaRepository<TipoObjeto, Long> {

    List<TipoObjeto> findByEstatusTrueOrderByNombreAsc();

    Optional<TipoObjeto> findByIdAndEstatusTrue(Long id);
}
