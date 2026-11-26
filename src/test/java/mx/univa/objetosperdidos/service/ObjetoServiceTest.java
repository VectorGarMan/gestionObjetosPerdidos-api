package mx.univa.objetosperdidos.service;

import static mx.univa.objetosperdidos.Fabrica.administrador;
import static mx.univa.objetosperdidos.Fabrica.color;
import static mx.univa.objetosperdidos.Fabrica.estudiante;
import static mx.univa.objetosperdidos.Fabrica.etiqueta;
import static mx.univa.objetosperdidos.Fabrica.modulo;
import static mx.univa.objetosperdidos.Fabrica.objeto;
import static mx.univa.objetosperdidos.Fabrica.peticion;
import static mx.univa.objetosperdidos.Fabrica.tipo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import mx.univa.objetosperdidos.dto.ObjetoDTO;
import mx.univa.objetosperdidos.dto.ObjetoForm;
import mx.univa.objetosperdidos.entity.EstadoObjeto;
import mx.univa.objetosperdidos.entity.EstadoPeticion;
import mx.univa.objetosperdidos.entity.Etiqueta;
import mx.univa.objetosperdidos.entity.Objeto;
import mx.univa.objetosperdidos.entity.ObjetoEtiqueta;
import mx.univa.objetosperdidos.entity.Peticion;
import mx.univa.objetosperdidos.entity.RolUsuario;
import mx.univa.objetosperdidos.entity.TipoPeticion;
import mx.univa.objetosperdidos.exception.ApiException;
import mx.univa.objetosperdidos.exception.Mensajes;
import mx.univa.objetosperdidos.repository.ColorRepository;
import mx.univa.objetosperdidos.repository.EtiquetaRepository;
import mx.univa.objetosperdidos.repository.ModuloRepository;
import mx.univa.objetosperdidos.repository.ObjetoRepository;
import mx.univa.objetosperdidos.repository.PeticionRepository;
import mx.univa.objetosperdidos.repository.TipoObjetoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ObjetoServiceTest {

    @Mock
    private ObjetoRepository objetoRepository;
    @Mock
    private PeticionRepository peticionRepository;
    @Mock
    private TipoObjetoRepository tipoObjetoRepository;
    @Mock
    private ColorRepository colorRepository;
    @Mock
    private ModuloRepository moduloRepository;
    @Mock
    private EtiquetaRepository etiquetaRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private ImagenService imagenService;

    @InjectMocks
    private ObjetoService service;

    private ObjetoForm form;

    @BeforeEach
    void setUp() {
        form = new ObjetoForm();
        form.setFotografia(new MockMultipartFile("fotografia", "mochila.jpg", "image/jpeg", new byte[]{1, 2, 3}));
        form.setNombre("  Mochila negra  ");
        form.setDescripcion("Mochila con bolsillo lateral para botella.");
        form.setMarca("   ");
        form.setLugarPerdida("Cafetería central");
        form.setTipoObjetoId(1L);
        form.setColorId(1L);
        form.setModuloId(1L);
        form.setOtrasEtiquetaIds(new ArrayList<>(List.of(2L, 4L)));
    }

    private void catalogosValidos() {
        when(tipoObjetoRepository.findByIdAndEstatusTrue(1L)).thenReturn(Optional.of(tipo(1L)));
        when(colorRepository.findByIdAndEstatusTrue(1L)).thenReturn(Optional.of(color(1L)));
        when(moduloRepository.findByIdAndEstatusTrue(1L)).thenReturn(Optional.of(modulo(1L)));
    }

    @Test
    @DisplayName("El estudiante debe elegir «Perdidos» o «Reclamados»")
    void estudianteSinEstado() {
        assertThatThrownBy(() -> service.buscar(null, null, null, null, RolUsuario.ESTUDIANTE))
                .hasMessage(Mensajes.ESTADO_OBLIGATORIO);
    }

    @Test
    @DisplayName("El administrador sin estado ve los objetos perdidos")
    void administradorSinEstadoVePerdidos() {
        when(objetoRepository.buscar(EstadoObjeto.PERDIDO, 3L, null, List.of(1L)))
                .thenReturn(List.of(objeto(2L, EstadoObjeto.PERDIDO)));

        List<ObjetoDTO> resultado = service.buscar(null, 3L, null, List.of(1L), RolUsuario.ADMINISTRADOR);

        assertThat(resultado).extracting(ObjetoDTO::id).containsExactly(2L);
    }

    @Test
    @DisplayName("El alta sin fotografía responde con el mensaje de campos obligatorios")
    void crearSinFotografia() {
        when(usuarioService.obtenerActivo(1L, RolUsuario.ADMINISTRADOR)).thenReturn(administrador());
        form.setFotografia(null);

        assertThatThrownBy(() -> service.crear(form, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessage(Mensajes.CAMPOS_OBLIGATORIOS)
                .satisfies(e -> assertThat(((ApiException) e).getCampos()).containsKey("fotografia"));
        verify(imagenService, never()).guardar(any());
    }

    @Test
    @DisplayName("Un catálogo inexistente se informa por campo y no guarda la fotografía")
    void crearConCatalogoInexistente() {
        when(usuarioService.obtenerActivo(1L, RolUsuario.ADMINISTRADOR)).thenReturn(administrador());
        when(tipoObjetoRepository.findByIdAndEstatusTrue(1L)).thenReturn(Optional.empty());
        when(colorRepository.findByIdAndEstatusTrue(1L)).thenReturn(Optional.of(color(1L)));
        when(moduloRepository.findByIdAndEstatusTrue(1L)).thenReturn(Optional.of(modulo(1L)));
        when(etiquetaRepository.findByIdInAndEstatusTrue(any()))
                .thenReturn(List.of(etiqueta(2L, "Con llavero"), etiqueta(4L, "Con estampas")));

        assertThatThrownBy(() -> service.crear(form, 1L))
                .satisfies(e -> assertThat(((ApiException) e).getCampos()).containsOnlyKeys("tipoObjetoId"));
        verify(imagenService, never()).guardar(any());
    }

    @Test
    @DisplayName("El alta guarda un objeto PERDIDO con datos limpios, su imagen y sus etiquetas")
    void crearObjeto() {
        when(usuarioService.obtenerActivo(1L, RolUsuario.ADMINISTRADOR)).thenReturn(administrador());
        catalogosValidos();
        when(etiquetaRepository.findByIdInAndEstatusTrue(any()))
                .thenReturn(List.of(etiqueta(2L, "Con llavero"), etiqueta(4L, "Con estampas")));
        when(imagenService.guardar(form.getFotografia())).thenReturn("/uploads/objetos/abc.jpg");
        when(objetoRepository.save(any(Objeto.class))).thenAnswer(inv -> inv.getArgument(0));

        ObjetoDTO dto = service.crear(form, 1L);

        ArgumentCaptor<Objeto> captor = ArgumentCaptor.forClass(Objeto.class);
        verify(objetoRepository).save(captor.capture());
        Objeto guardado = captor.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoObjeto.PERDIDO);
        assertThat(guardado.getNombre()).isEqualTo("Mochila negra");
        assertThat(guardado.getMarca()).isNull();
        assertThat(guardado.getRutaImagen()).isEqualTo("/uploads/objetos/abc.jpg");
        assertThat(guardado.getRegistradoPor().getId()).isEqualTo(1L);
        assertThat(guardado.getReclamadoPor()).isNull();
        assertThat(dto.otrasEtiquetas()).extracting("nombre").containsExactly("Con estampas", "Con llavero");
    }

    @Test
    @DisplayName("Un estudiante no puede dar de alta objetos aunque llegue al servicio")
    void crearComoEstudiante() {
        when(usuarioService.obtenerActivo(3L, RolUsuario.ADMINISTRADOR))
                .thenThrow(ApiException.prohibido(Mensajes.SIN_PERMISO));

        assertThatThrownBy(() -> service.crear(form, 3L)).hasMessage(Mensajes.SIN_PERMISO);
        verify(objetoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Eliminar es lógico y desactiva las peticiones pendientes del objeto")
    void eliminarLogico() {
        Objeto mochila = objeto(1L, EstadoObjeto.PERDIDO);
        Peticion pendiente = peticion(3L, mochila, estudiante(5L, "A2024015"), TipoPeticion.RECLAMO);
        when(usuarioService.obtenerActivo(1L, RolUsuario.ADMINISTRADOR)).thenReturn(administrador());
        when(objetoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(mochila));
        when(peticionRepository.findByObjetoIdAndEstadoAndEstatusTrue(1L, EstadoPeticion.PENDIENTE))
                .thenReturn(List.of(pendiente));

        service.eliminar(1L, 1L);

        assertThat(mochila.getEstatus()).isFalse();
        assertThat(pendiente.getEstatus()).isFalse();
        verify(objetoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Un objeto eliminado ya no se encuentra")
    void obtenerEliminado() {
        when(objetoRepository.findByIdAndEstatusTrue(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(4L)).hasMessage(Mensajes.OBJETO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("Editar etiquetas desactiva las quitadas, conserva las repetidas y agrega las nuevas")
    void sincronizarEtiquetas() {
        Objeto o = objeto(1L, EstadoObjeto.PERDIDO);
        Etiqueta llavero = etiqueta(2L, "Con llavero");
        Etiqueta estampas = etiqueta(4L, "Con estampas");
        Etiqueta danado = etiqueta(5L, "Dañado");
        ObjetoEtiqueta relLlavero = new ObjetoEtiqueta(o, llavero);
        ObjetoEtiqueta relEstampas = new ObjetoEtiqueta(o, estampas);
        o.getEtiquetas().addAll(List.of(relLlavero, relEstampas));

        ObjetoService.sincronizarEtiquetas(o, List.of(estampas, danado));

        assertThat(relLlavero.getEstatus()).isFalse();
        assertThat(relEstampas.getEstatus()).isTrue();
        assertThat(o.getEtiquetas()).hasSize(3);
        assertThat(ObjetoDTO.de(o).otrasEtiquetas()).extracting("nombre").containsExactly("Con estampas", "Dañado");
    }
}
