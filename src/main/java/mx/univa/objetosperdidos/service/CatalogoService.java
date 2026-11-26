package mx.univa.objetosperdidos.service;

import java.util.List;
import java.util.Locale;

import mx.univa.objetosperdidos.dto.CatalogoDTO;
import mx.univa.objetosperdidos.exception.ApiException;
import mx.univa.objetosperdidos.repository.ColorRepository;
import mx.univa.objetosperdidos.repository.EtiquetaRepository;
import mx.univa.objetosperdidos.repository.ModuloRepository;
import mx.univa.objetosperdidos.repository.TipoObjetoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catálogos compartidos por el alta del administrador y los filtros del estudiante. */
@Service
public class CatalogoService {

    public static final List<String> TIPOS = List.of("tipos-objeto", "colores", "etiquetas", "modulos");

    private final TipoObjetoRepository tipoObjetoRepository;
    private final ColorRepository colorRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final ModuloRepository moduloRepository;

    public CatalogoService(TipoObjetoRepository tipoObjetoRepository, ColorRepository colorRepository,
                           EtiquetaRepository etiquetaRepository, ModuloRepository moduloRepository) {
        this.tipoObjetoRepository = tipoObjetoRepository;
        this.colorRepository = colorRepository;
        this.etiquetaRepository = etiquetaRepository;
        this.moduloRepository = moduloRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogoDTO> listar(String tipo) {
        return switch (tipo == null ? "" : tipo.trim().toLowerCase(Locale.ROOT)) {
            case "tipos-objeto" -> tipoObjetoRepository.findByEstatusTrueOrderByNombreAsc().stream()
                    .map(CatalogoDTO::de).toList();
            case "colores" -> colorRepository.findByEstatusTrueOrderByNombreAsc().stream()
                    .map(CatalogoDTO::de).toList();
            case "etiquetas" -> etiquetaRepository.findByEstatusTrueOrderByNombreAsc().stream()
                    .map(CatalogoDTO::de).toList();
            case "modulos" -> moduloRepository.findByEstatusTrueOrderByNombreAsc().stream()
                    .map(CatalogoDTO::de).toList();
            default -> throw ApiException.noEncontrado(
                    "El catálogo solicitado no existe. Usa: " + String.join(", ", TIPOS) + ".");
        };
    }
}
