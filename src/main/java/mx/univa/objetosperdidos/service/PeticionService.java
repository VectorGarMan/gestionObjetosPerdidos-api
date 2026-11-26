package mx.univa.objetosperdidos.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import mx.univa.objetosperdidos.dto.ContactoDTO;
import mx.univa.objetosperdidos.dto.PeticionDTO;
import mx.univa.objetosperdidos.dto.PeticionDetalleDTO;
import mx.univa.objetosperdidos.dto.PeticionRequest;
import mx.univa.objetosperdidos.entity.EstadoObjeto;
import mx.univa.objetosperdidos.entity.EstadoPeticion;
import mx.univa.objetosperdidos.entity.Objeto;
import mx.univa.objetosperdidos.entity.Peticion;
import mx.univa.objetosperdidos.entity.RolUsuario;
import mx.univa.objetosperdidos.entity.TipoPeticion;
import mx.univa.objetosperdidos.entity.Usuario;
import mx.univa.objetosperdidos.exception.ApiException;
import mx.univa.objetosperdidos.exception.Mensajes;
import mx.univa.objetosperdidos.repository.ObjetoRepository;
import mx.univa.objetosperdidos.repository.PeticionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de las peticiones (F-03 y F-05).
 * Orden de bloqueo: primero el objeto y luego la petición, igual que al eliminar un objeto,
 * para evitar bloqueos cruzados entre dos administradores.
 */
@Service
public class PeticionService {

    private static final String PATRON_MATRICULA = "^[A-Za-z0-9]{1,15}$";

    private final PeticionRepository peticionRepository;
    private final ObjetoRepository objetoRepository;
    private final UsuarioService usuarioService;
    private final Clock clock;

    public PeticionService(PeticionRepository peticionRepository, ObjetoRepository objetoRepository,
                           UsuarioService usuarioService, Clock clock) {
        this.peticionRepository = peticionRepository;
        this.objetoRepository = objetoRepository;
        this.usuarioService = usuarioService;
        this.clock = clock;
    }

    /** PNT-04: el tipo lo determina el estado del objeto (Perdido → Reclamo, Reclamado → Reclamo de error). */
    @Transactional
    public PeticionDTO crear(PeticionRequest request, Long estudianteId) {
        Usuario estudiante = usuarioService.obtenerActivo(estudianteId, RolUsuario.ESTUDIANTE);
        Objeto objeto = objetoRepository.findByIdParaActualizar(request.objetoId())
                .orElseThrow(() -> ApiException.noEncontrado(Mensajes.OBJETO_NO_ENCONTRADO));
        if (!objeto.isActivo()) {
            throw ApiException.conflicto(Mensajes.OBJETO_CAMBIO_ESTADO);
        }
        TipoPeticion tipo = objeto.getEstado() == EstadoObjeto.PERDIDO
                ? TipoPeticion.RECLAMO
                : TipoPeticion.RECLAMO_ERROR;
        if (request.tipo() != null && request.tipo() != tipo) {
            throw ApiException.conflicto(Mensajes.OBJETO_CAMBIO_ESTADO);
        }
        if (tipo == TipoPeticion.RECLAMO_ERROR && objeto.getReclamadoPor() != null
                && Objects.equals(objeto.getReclamadoPor().getId(), estudiante.getId())) {
            throw ApiException.conflicto(Mensajes.RECLAMO_ERROR_PROPIO);
        }
        if (peticionRepository.existsByObjetoIdAndSolicitanteIdAndEstadoAndEstatusTrue(
                objeto.getId(), estudiante.getId(), EstadoPeticion.PENDIENTE)) {
            throw ApiException.conflicto(Mensajes.PETICION_DUPLICADA);
        }

        Peticion peticion = new Peticion();
        peticion.setObjeto(objeto);
        peticion.setSolicitante(estudiante);
        peticion.setTipo(tipo);
        peticion.setEstado(EstadoPeticion.PENDIENTE);
        // saveAndFlush: si otra solicitud gana la carrera, uq_peticion_pendiente falla aquí
        // y GlobalExceptionHandler responde con el mensaje de petición duplicada.
        return PeticionDTO.de(peticionRepository.saveAndFlush(peticion));
    }

    /** PNT-08: lista de peticiones activas, opcionalmente por matrícula. */
    @Transactional(readOnly = true)
    public List<PeticionDTO> listar(String matricula) {
        String filtro = matricula == null ? null : matricula.trim();
        if (filtro == null || filtro.isEmpty()) {
            return peticionRepository.findByEstatusTrueOrderByFechaRegistroDesc().stream()
                    .map(PeticionDTO::de).toList();
        }
        if (!filtro.matches(PATRON_MATRICULA)) {
            throw ApiException.solicitudInvalida(Mensajes.MATRICULA_INVALIDA);
        }
        return peticionRepository.findBySolicitanteMatriculaIgnoreCaseAndEstatusTrueOrderByFechaRegistroDesc(filtro)
                .stream().map(PeticionDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public PeticionDetalleDTO obtener(Long id) {
        return PeticionDetalleDTO.de(buscarActiva(id));
    }

    /**
     * «Contactar estudiante» (solo reclamo de error): devuelve a quien se le entregó el objeto
     * antes de este caso. Se busca en las entregas registradas para que el dato siga siendo
     * correcto aunque la petición ya se haya liberado.
     */
    @Transactional(readOnly = true)
    public ContactoDTO contactoPrevio(Long id) {
        Peticion peticion = buscarActiva(id);
        if (peticion.getTipo() != TipoPeticion.RECLAMO_ERROR) {
            throw ApiException.conflicto(Mensajes.CONTACTO_SOLO_RECLAMO_ERROR);
        }
        LocalDateTime referencia = peticion.getEstado() == EstadoPeticion.LIBERADA
                ? peticion.getFechaLiberacion()
                : LocalDateTime.now(clock).plusSeconds(1);
        Usuario previo = peticionRepository
                .findFirstByObjetoIdAndEstadoAndIdNotAndFechaLiberacionBeforeOrderByFechaLiberacionDesc(
                        peticion.getObjeto().getId(), EstadoPeticion.LIBERADA, peticion.getId(), referencia)
                .map(Peticion::getSolicitante)
                .orElseGet(() -> peticion.getEstado() == EstadoPeticion.PENDIENTE
                        ? peticion.getObjeto().getReclamadoPor()
                        : null);
        if (previo == null) {
            throw ApiException.noEncontrado("No hay un estudiante registrado que haya reclamado antes este objeto.");
        }
        return ContactoDTO.de(previo, Mensajes.CONTACTO_AVISO);
    }

    /**
     * PNT-08 «Liberar».
     * Reclamo: el objeto pasa a RECLAMADO a nombre del solicitante y los demás reclamos
     * pendientes de ese objeto se desactivan (estatus = FALSE).
     * Reclamo de error: el caso se resolvió a favor de quien reportó; el objeto sigue RECLAMADO
     * pero ahora a su nombre. La entrega anterior queda registrada en su propia petición.
     */
    @Transactional
    public PeticionDetalleDTO liberar(Long id, Long administradorId) {
        Usuario administrador = usuarioService.obtenerActivo(administradorId, RolUsuario.ADMINISTRADOR);
        // Solo se lee el id del objeto (sin cargar la petición) para que la lectura con bloqueo
        // de más abajo traiga el estado más reciente y no una copia en caché.
        Long objetoId = peticionRepository.findObjetoIdDePeticionActiva(id)
                .orElseThrow(() -> ApiException.noEncontrado(Mensajes.PETICION_NO_ENCONTRADA));

        Objeto objeto = objetoRepository.findByIdParaActualizar(objetoId)
                .orElseThrow(() -> ApiException.noEncontrado(Mensajes.OBJETO_NO_ENCONTRADO));
        Peticion peticion = peticionRepository.findByIdParaActualizar(id)
                .filter(Peticion::isActivo)
                .orElseThrow(() -> ApiException.noEncontrado(Mensajes.PETICION_NO_ENCONTRADA));
        if (peticion.getEstado() == EstadoPeticion.LIBERADA) {
            throw ApiException.conflicto(Mensajes.PETICION_YA_LIBERADA);
        }
        EstadoObjeto estadoEsperado = peticion.getTipo() == TipoPeticion.RECLAMO
                ? EstadoObjeto.PERDIDO
                : EstadoObjeto.RECLAMADO;
        if (!objeto.isActivo() || objeto.getEstado() != estadoEsperado) {
            throw ApiException.conflicto(Mensajes.OBJETO_CAMBIO_ESTADO);
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        objeto.setEstado(EstadoObjeto.RECLAMADO);
        objeto.setReclamadoPor(peticion.getSolicitante());
        objeto.setFechaReclamo(ahora);

        peticion.setEstado(EstadoPeticion.LIBERADA);
        peticion.setLiberadaPor(administrador);
        peticion.setFechaLiberacion(ahora);

        if (peticion.getTipo() == TipoPeticion.RECLAMO) {
            peticionRepository.findByObjetoIdAndEstadoAndEstatusTrue(objeto.getId(), EstadoPeticion.PENDIENTE)
                    .stream()
                    .filter(otra -> !otra.getId().equals(peticion.getId()))
                    .filter(otra -> otra.getTipo() == TipoPeticion.RECLAMO)
                    .forEach(otra -> otra.setEstatus(Boolean.FALSE));
        }
        objetoRepository.save(objeto);
        return PeticionDetalleDTO.de(peticionRepository.save(peticion));
    }

    private Peticion buscarActiva(Long id) {
        return peticionRepository.findByIdAndEstatusTrue(id)
                .orElseThrow(() -> ApiException.noEncontrado(Mensajes.PETICION_NO_ENCONTRADA));
    }
}
