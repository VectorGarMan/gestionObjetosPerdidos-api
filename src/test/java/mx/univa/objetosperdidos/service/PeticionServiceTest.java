package mx.univa.objetosperdidos.service;

import static mx.univa.objetosperdidos.Fabrica.administrador;
import static mx.univa.objetosperdidos.Fabrica.estudiante;
import static mx.univa.objetosperdidos.Fabrica.objeto;
import static mx.univa.objetosperdidos.Fabrica.objetoReclamadoPor;
import static mx.univa.objetosperdidos.Fabrica.peticion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PeticionServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
    private static final Instant INSTANTE = Instant.parse("2026-11-26T18:00:00Z");
    private static final LocalDateTime AHORA = LocalDateTime.ofInstant(INSTANTE, ZONA);

    @Mock
    private PeticionRepository peticionRepository;
    @Mock
    private ObjetoRepository objetoRepository;
    @Mock
    private UsuarioService usuarioService;

    private PeticionService service;

    private final Usuario ana = estudiante(3L, "A2023001");
    private final Usuario carlos = estudiante(6L, "A2022087");
    private final Usuario laura = administrador();

    @BeforeEach
    void setUp() {
        service = new PeticionService(peticionRepository, objetoRepository, usuarioService,
                Clock.fixed(INSTANTE, ZONA));
    }

    private void estudianteActivo(Usuario u) {
        when(usuarioService.obtenerActivo(u.getId(), RolUsuario.ESTUDIANTE)).thenReturn(u);
    }

    private void guardarDevuelveConId() {
        when(peticionRepository.saveAndFlush(any(Peticion.class))).thenAnswer(inv -> {
            Peticion p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });
    }

    // ---------- Crear (PNT-04) ----------

    @Test
    @DisplayName("Un objeto PERDIDO genera una petición de tipo RECLAMO pendiente")
    void crearReclamoSobreObjetoPerdido() {
        estudianteActivo(ana);
        when(objetoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(objeto(1L, EstadoObjeto.PERDIDO)));
        guardarDevuelveConId();

        PeticionDTO dto = service.crear(new PeticionRequest(1L, null), ana.getId());

        assertThat(dto.tipo()).isEqualTo(TipoPeticion.RECLAMO);
        assertThat(dto.estado()).isEqualTo(EstadoPeticion.PENDIENTE);
        assertThat(dto.matricula()).isEqualTo("A2023001");
        assertThat(dto.objetoId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Un objeto RECLAMADO genera una petición de tipo RECLAMO_ERROR")
    void crearReclamoErrorSobreObjetoReclamado() {
        estudianteActivo(carlos);
        when(objetoRepository.findByIdParaActualizar(5L)).thenReturn(Optional.of(objetoReclamadoPor(5L, ana)));
        guardarDevuelveConId();

        PeticionDTO dto = service.crear(new PeticionRequest(5L, TipoPeticion.RECLAMO_ERROR), carlos.getId());

        assertThat(dto.tipo()).isEqualTo(TipoPeticion.RECLAMO_ERROR);
    }

    @Test
    @DisplayName("No permite dos peticiones pendientes del mismo estudiante sobre el mismo objeto")
    void rechazaPeticionDuplicada() {
        estudianteActivo(ana);
        when(objetoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(objeto(1L, EstadoObjeto.PERDIDO)));
        when(peticionRepository.existsByObjetoIdAndSolicitanteIdAndEstadoAndEstatusTrue(1L, 3L,
                EstadoPeticion.PENDIENTE)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new PeticionRequest(1L, null), ana.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage(Mensajes.PETICION_DUPLICADA)
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        verify(peticionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Si el tipo enviado ya no coincide con el estado del objeto responde «cambió de estado»")
    void rechazaTipoQueNoCoincideConEstado() {
        estudianteActivo(ana);
        when(objetoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(objetoReclamadoPor(1L, carlos)));

        assertThatThrownBy(() -> service.crear(new PeticionRequest(1L, TipoPeticion.RECLAMO), ana.getId()))
                .hasMessage(Mensajes.OBJETO_CAMBIO_ESTADO);
    }

    @Test
    @DisplayName("No se puede reclamar un objeto eliminado")
    void rechazaObjetoEliminado() {
        estudianteActivo(ana);
        Objeto eliminado = objeto(4L, EstadoObjeto.PERDIDO);
        eliminado.setEstatus(false);
        when(objetoRepository.findByIdParaActualizar(4L)).thenReturn(Optional.of(eliminado));

        assertThatThrownBy(() -> service.crear(new PeticionRequest(4L, null), ana.getId()))
                .hasMessage(Mensajes.OBJETO_CAMBIO_ESTADO);
    }

    @Test
    @DisplayName("Quien recibió el objeto no puede reportar un reclamo de error sobre él")
    void rechazaReclamoErrorPropio() {
        estudianteActivo(ana);
        when(objetoRepository.findByIdParaActualizar(5L)).thenReturn(Optional.of(objetoReclamadoPor(5L, ana)));

        assertThatThrownBy(() -> service.crear(new PeticionRequest(5L, null), ana.getId()))
                .hasMessage(Mensajes.RECLAMO_ERROR_PROPIO);
    }

    @Test
    @DisplayName("Objeto inexistente responde 404")
    void objetoInexistente() {
        estudianteActivo(ana);
        when(objetoRepository.findByIdParaActualizar(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new PeticionRequest(77L, null), ana.getId()))
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- Liberar (PNT-08) ----------

    private void prepararLiberacion(Peticion p, Objeto o) {
        when(usuarioService.obtenerActivo(laura.getId(), RolUsuario.ADMINISTRADOR)).thenReturn(laura);
        when(peticionRepository.findObjetoIdDePeticionActiva(p.getId())).thenReturn(Optional.of(o.getId()));
        when(objetoRepository.findByIdParaActualizar(o.getId())).thenReturn(Optional.of(o));
        when(peticionRepository.findByIdParaActualizar(p.getId())).thenReturn(Optional.of(p));
    }

    @Test
    @DisplayName("Liberar un reclamo entrega el objeto y desactiva los demás reclamos pendientes")
    void liberarReclamo() {
        Objeto mochila = objeto(1L, EstadoObjeto.PERDIDO);
        Peticion deMaria = peticion(3L, mochila, estudiante(5L, "A2024015"), TipoPeticion.RECLAMO);
        Peticion deAna = peticion(8L, mochila, ana, TipoPeticion.RECLAMO);
        prepararLiberacion(deMaria, mochila);
        when(peticionRepository.findByObjetoIdAndEstadoAndEstatusTrue(1L, EstadoPeticion.PENDIENTE))
                .thenReturn(List.of(deMaria, deAna));
        when(peticionRepository.save(deMaria)).thenReturn(deMaria);

        PeticionDetalleDTO dto = service.liberar(3L, laura.getId());

        assertThat(dto.estado()).isEqualTo(EstadoPeticion.LIBERADA);
        assertThat(dto.fechaLiberacion()).isEqualTo(AHORA);
        assertThat(dto.liberadaPor().id()).isEqualTo(laura.getId());
        assertThat(mochila.getEstado()).isEqualTo(EstadoObjeto.RECLAMADO);
        assertThat(mochila.getReclamadoPor().getId()).isEqualTo(5L);
        assertThat(mochila.getFechaReclamo()).isEqualTo(AHORA);
        assertThat(deAna.getEstatus()).isFalse();
        assertThat(deMaria.getEstatus()).isTrue();
    }

    @Test
    @DisplayName("Liberar un reclamo de error deja el objeto RECLAMADO a nombre de quien reportó")
    void liberarReclamoError() {
        Objeto celular = objetoReclamadoPor(5L, ana);
        Peticion deCarlos = peticion(4L, celular, carlos, TipoPeticion.RECLAMO_ERROR);
        prepararLiberacion(deCarlos, celular);
        when(peticionRepository.save(deCarlos)).thenReturn(deCarlos);

        service.liberar(4L, laura.getId());

        assertThat(celular.getEstado()).isEqualTo(EstadoObjeto.RECLAMADO);
        assertThat(celular.getReclamadoPor()).isSameAs(carlos);
        assertThat(celular.getFechaReclamo()).isEqualTo(AHORA);
        assertThat(deCarlos.getEstado()).isEqualTo(EstadoPeticion.LIBERADA);
        verify(peticionRepository, never()).findByObjetoIdAndEstadoAndEstatusTrue(any(), any());
    }

    @Test
    @DisplayName("Una petición ya liberada no se puede liberar otra vez")
    void liberarPeticionYaLiberada() {
        Objeto celular = objetoReclamadoPor(5L, ana);
        Peticion liberada = peticion(1L, celular, ana, TipoPeticion.RECLAMO);
        liberada.setEstado(EstadoPeticion.LIBERADA);
        prepararLiberacion(liberada, celular);

        assertThatThrownBy(() -> service.liberar(1L, laura.getId()))
                .hasMessage(Mensajes.PETICION_YA_LIBERADA)
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("No se libera un reclamo si el objeto ya se entregó a otra persona")
    void liberarReclamoDeObjetoYaReclamado() {
        Objeto celular = objetoReclamadoPor(5L, ana);
        Peticion tardia = peticion(9L, celular, carlos, TipoPeticion.RECLAMO);
        prepararLiberacion(tardia, celular);

        assertThatThrownBy(() -> service.liberar(9L, laura.getId()))
                .hasMessage(Mensajes.OBJETO_CAMBIO_ESTADO);
        assertThat(celular.getReclamadoPor()).isSameAs(ana);
    }

    @Test
    @DisplayName("Liberar una petición inexistente responde 404")
    void liberarInexistente() {
        when(usuarioService.obtenerActivo(laura.getId(), RolUsuario.ADMINISTRADOR)).thenReturn(laura);
        when(peticionRepository.findObjetoIdDePeticionActiva(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.liberar(50L, laura.getId()))
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- Listar y contacto (PNT-08) ----------

    @Test
    @DisplayName("Una matrícula con símbolos se rechaza antes de buscar")
    void matriculaInvalida() {
        assertThatThrownBy(() -> service.listar("A20-23"))
                .hasMessage(Mensajes.MATRICULA_INVALIDA);
        verify(peticionRepository, never())
                .findBySolicitanteMatriculaIgnoreCaseAndEstatusTrueOrderByFechaRegistroDesc(anyString());
    }

    @Test
    @DisplayName("Sin matrícula se listan todas las peticiones activas")
    void listarSinMatricula() {
        Peticion p = peticion(3L, objeto(1L, EstadoObjeto.PERDIDO), ana, TipoPeticion.RECLAMO);
        when(peticionRepository.findByEstatusTrueOrderByFechaRegistroDesc()).thenReturn(List.of(p));

        assertThat(service.listar("  ")).extracting(PeticionDTO::id).containsExactly(3L);
    }

    @Test
    @DisplayName("Con matrícula válida se busca sin espacios alrededor")
    void listarPorMatricula() {
        when(peticionRepository.findBySolicitanteMatriculaIgnoreCaseAndEstatusTrueOrderByFechaRegistroDesc("A2022087"))
                .thenReturn(List.of());

        assertThat(service.listar(" A2022087 ")).isEmpty();
    }

    @Test
    @DisplayName("El contacto previo solo existe en reclamos de error")
    void contactoSoloEnReclamoError() {
        Peticion reclamo = peticion(3L, objeto(1L, EstadoObjeto.PERDIDO), ana, TipoPeticion.RECLAMO);
        when(peticionRepository.findByIdAndEstatusTrue(3L)).thenReturn(Optional.of(reclamo));

        assertThatThrownBy(() -> service.contactoPrevio(3L))
                .hasMessage(Mensajes.CONTACTO_SOLO_RECLAMO_ERROR);
    }

    @Test
    @DisplayName("El contacto previo es la persona a quien se entregó antes el objeto")
    void contactoPrevioDevuelveEntregaAnterior() {
        Objeto celular = objetoReclamadoPor(5L, ana);
        Peticion error = peticion(4L, celular, carlos, TipoPeticion.RECLAMO_ERROR);
        Peticion entrega = peticion(1L, celular, ana, TipoPeticion.RECLAMO);
        entrega.setEstado(EstadoPeticion.LIBERADA);
        when(peticionRepository.findByIdAndEstatusTrue(4L)).thenReturn(Optional.of(error));
        when(peticionRepository.findFirstByObjetoIdAndEstadoAndIdNotAndFechaLiberacionBeforeOrderByFechaLiberacionDesc(
                eq(5L), eq(EstadoPeticion.LIBERADA), eq(4L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(entrega));

        ContactoDTO contacto = service.contactoPrevio(4L);

        assertThat(contacto.matricula()).isEqualTo("A2023001");
        assertThat(contacto.telefono()).isNotBlank();
        assertThat(contacto.aviso()).isEqualTo(Mensajes.CONTACTO_AVISO);
    }
}
