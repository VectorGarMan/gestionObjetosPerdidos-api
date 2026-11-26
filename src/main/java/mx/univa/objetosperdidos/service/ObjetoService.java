package mx.univa.objetosperdidos.service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import mx.univa.objetosperdidos.dto.ObjetoDTO;
import mx.univa.objetosperdidos.dto.ObjetoForm;
import mx.univa.objetosperdidos.entity.Color;
import mx.univa.objetosperdidos.entity.EstadoObjeto;
import mx.univa.objetosperdidos.entity.EstadoPeticion;
import mx.univa.objetosperdidos.entity.Etiqueta;
import mx.univa.objetosperdidos.entity.Modulo;
import mx.univa.objetosperdidos.entity.Objeto;
import mx.univa.objetosperdidos.entity.ObjetoEtiqueta;
import mx.univa.objetosperdidos.entity.RolUsuario;
import mx.univa.objetosperdidos.entity.TipoObjeto;
import mx.univa.objetosperdidos.entity.Usuario;
import mx.univa.objetosperdidos.exception.ApiException;
import mx.univa.objetosperdidos.exception.Mensajes;
import mx.univa.objetosperdidos.repository.ColorRepository;
import mx.univa.objetosperdidos.repository.EtiquetaRepository;
import mx.univa.objetosperdidos.repository.ModuloRepository;
import mx.univa.objetosperdidos.repository.ObjetoRepository;
import mx.univa.objetosperdidos.repository.PeticionRepository;
import mx.univa.objetosperdidos.repository.TipoObjetoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ObjetoService {

    private final ObjetoRepository objetoRepository;
    private final PeticionRepository peticionRepository;
    private final TipoObjetoRepository tipoObjetoRepository;
    private final ColorRepository colorRepository;
    private final ModuloRepository moduloRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final UsuarioService usuarioService;
    private final ImagenService imagenService;

    public ObjetoService(ObjetoRepository objetoRepository, PeticionRepository peticionRepository,
                         TipoObjetoRepository tipoObjetoRepository, ColorRepository colorRepository,
                         ModuloRepository moduloRepository, EtiquetaRepository etiquetaRepository,
                         UsuarioService usuarioService, ImagenService imagenService) {
        this.objetoRepository = objetoRepository;
        this.peticionRepository = peticionRepository;
        this.tipoObjetoRepository = tipoObjetoRepository;
        this.colorRepository = colorRepository;
        this.moduloRepository = moduloRepository;
        this.etiquetaRepository = etiquetaRepository;
        this.usuarioService = usuarioService;
        this.imagenService = imagenService;
    }

    /**
     * PNT-03, PNT-05 y PNT-09. El estudiante debe elegir el estado; el administrador,
     * si no lo envía, ve los perdidos (su pantalla principal).
     */
    @Transactional(readOnly = true)
    public List<ObjetoDTO> buscar(EstadoObjeto estado, Long tipoObjetoId, Long colorId, List<Long> etiquetaIds,
                                  RolUsuario rol) {
        if (estado == null) {
            if (rol != RolUsuario.ADMINISTRADOR) {
                throw ApiException.solicitudInvalida(Mensajes.ESTADO_OBLIGATORIO);
            }
            estado = EstadoObjeto.PERDIDO;
        }
        return objetoRepository.buscar(estado, tipoObjetoId, colorId, etiquetaIds).stream()
                .map(ObjetoDTO::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ObjetoDTO obtener(Long id) {
        return ObjetoDTO.de(objetoRepository.findByIdAndEstatusTrue(id)
                .orElseThrow(() -> ApiException.noEncontrado(Mensajes.OBJETO_NO_ENCONTRADO)));
    }

    /** PNT-07: alta de objeto perdido. */
    @Transactional
    public ObjetoDTO crear(ObjetoForm form, Long administradorId) {
        Usuario administrador = usuarioService.obtenerActivo(administradorId, RolUsuario.ADMINISTRADOR);
        if (form.getFotografia() == null || form.getFotografia().isEmpty()) {
            throw ApiException.solicitudInvalida(Mensajes.CAMPOS_OBLIGATORIOS,
                    Map.of("fotografia", "La fotografía es obligatoria."));
        }
        Catalogos catalogos = resolverCatalogos(form);

        Objeto objeto = new Objeto();
        objeto.setEstado(EstadoObjeto.PERDIDO);
        objeto.setRegistradoPor(administrador);
        aplicarDatos(objeto, form, catalogos);
        objeto.setRutaImagen(guardarImagen(form.getFotografia()));
        sincronizarEtiquetas(objeto, catalogos.etiquetas());

        return ObjetoDTO.de(objetoRepository.save(objeto));
    }

    /** PNT-06: edición con el mismo modelo que el alta; la fotografía es opcional. */
    @Transactional
    public ObjetoDTO actualizar(Long id, ObjetoForm form, Long administradorId) {
        usuarioService.obtenerActivo(administradorId, RolUsuario.ADMINISTRADOR);
        Objeto objeto = objetoRepository.findByIdParaActualizar(id)
                .filter(Objeto::isActivo)
                .orElseThrow(() -> ApiException.noEncontrado(Mensajes.OBJETO_NO_ENCONTRADO));
        Catalogos catalogos = resolverCatalogos(form);

        aplicarDatos(objeto, form, catalogos);
        MultipartFile foto = form.getFotografia();
        if (foto != null && !foto.isEmpty()) {
            objeto.setRutaImagen(guardarImagen(foto));
        }
        sincronizarEtiquetas(objeto, catalogos.etiquetas());

        return ObjetoDTO.de(objetoRepository.save(objeto));
    }

    /**
     * PNT-06: «eliminar» es borrado lógico (estatus = FALSE). Las peticiones pendientes del
     * objeto también se desactivan para que no queden en la bandeja del administrador.
     */
    @Transactional
    public void eliminar(Long id, Long administradorId) {
        usuarioService.obtenerActivo(administradorId, RolUsuario.ADMINISTRADOR);
        Objeto objeto = objetoRepository.findByIdParaActualizar(id)
                .filter(Objeto::isActivo)
                .orElseThrow(() -> ApiException.noEncontrado(Mensajes.OBJETO_NO_ENCONTRADO));
        objeto.setEstatus(Boolean.FALSE);
        peticionRepository.findByObjetoIdAndEstadoAndEstatusTrue(objeto.getId(), EstadoPeticion.PENDIENTE)
                .forEach(p -> p.setEstatus(Boolean.FALSE));
    }

    private void aplicarDatos(Objeto objeto, ObjetoForm form, Catalogos catalogos) {
        objeto.setNombre(form.getNombre().trim());
        objeto.setDescripcion(form.getDescripcion().trim());
        objeto.setMarca(form.getMarca() == null || form.getMarca().isBlank() ? null : form.getMarca().trim());
        objeto.setLugarPerdida(form.getLugarPerdida().trim());
        objeto.setTipoObjeto(catalogos.tipoObjeto());
        objeto.setColor(catalogos.color());
        objeto.setModulo(catalogos.modulo());
    }

    /** Las etiquetas se activan o desactivan (nunca se borran) por la restricción uq_objeto_etiqueta. */
    static void sincronizarEtiquetas(Objeto objeto, List<Etiqueta> seleccionadas) {
        Map<Long, Etiqueta> porId = seleccionadas.stream()
                .collect(Collectors.toMap(Etiqueta::getId, Function.identity(), (a, b) -> a));
        Set<Long> existentes = new LinkedHashSet<>();
        for (ObjetoEtiqueta relacion : objeto.getEtiquetas()) {
            Long etiquetaId = relacion.getEtiqueta().getId();
            existentes.add(etiquetaId);
            relacion.setEstatus(porId.containsKey(etiquetaId));
        }
        porId.forEach((etiquetaId, etiqueta) -> {
            if (!existentes.contains(etiquetaId)) {
                objeto.getEtiquetas().add(new ObjetoEtiqueta(objeto, etiqueta));
            }
        });
    }

    private Catalogos resolverCatalogos(ObjetoForm form) {
        Map<String, String> errores = new HashMap<>();
        TipoObjeto tipo = tipoObjetoRepository.findByIdAndEstatusTrue(form.getTipoObjetoId()).orElse(null);
        if (tipo == null) {
            errores.put("tipoObjetoId", "El tipo de objeto seleccionado no existe.");
        }
        Color color = colorRepository.findByIdAndEstatusTrue(form.getColorId()).orElse(null);
        if (color == null) {
            errores.put("colorId", "El color seleccionado no existe.");
        }
        Modulo modulo = moduloRepository.findByIdAndEstatusTrue(form.getModuloId()).orElse(null);
        if (modulo == null) {
            errores.put("moduloId", "El módulo seleccionado no existe.");
        }
        Set<Long> etiquetaIds = form.getOtrasEtiquetaIds() == null ? Set.of()
                : form.getOtrasEtiquetaIds().stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Etiqueta> etiquetas = etiquetaIds.isEmpty() ? List.of()
                : etiquetaRepository.findByIdInAndEstatusTrue(etiquetaIds);
        if (etiquetas.size() != etiquetaIds.size()) {
            errores.put("otrasEtiquetaIds", "Alguna de las etiquetas seleccionadas no existe.");
        }
        if (!errores.isEmpty()) {
            throw ApiException.solicitudInvalida("Revisa las opciones seleccionadas en las listas.", errores);
        }
        return new Catalogos(tipo, color, modulo, etiquetas);
    }

    /** Si la transacción se revierte, se borra la fotografía recién guardada para no dejar archivos huérfanos. */
    private String guardarImagen(MultipartFile foto) {
        String ruta = imagenService.guardar(foto);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        imagenService.eliminar(ruta);
                    }
                }
            });
        }
        return ruta;
    }

    private record Catalogos(TipoObjeto tipoObjeto, Color color, Modulo modulo, List<Etiqueta> etiquetas) {
    }
}
