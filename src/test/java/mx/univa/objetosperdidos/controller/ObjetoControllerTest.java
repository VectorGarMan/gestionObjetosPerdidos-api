package mx.univa.objetosperdidos.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import mx.univa.objetosperdidos.Fabrica;
import mx.univa.objetosperdidos.dto.ObjetoDTO;
import mx.univa.objetosperdidos.entity.EstadoObjeto;
import mx.univa.objetosperdidos.entity.RolUsuario;
import mx.univa.objetosperdidos.exception.ApiException;
import mx.univa.objetosperdidos.exception.Mensajes;
import mx.univa.objetosperdidos.service.ObjetoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class ObjetoControllerTest {

    @Mock
    private ObjetoService objetoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControladorTestSupport.mockMvc(new ObjetoController(objetoService));
    }

    @AfterEach
    void tearDown() {
        ControladorTestSupport.limpiar();
    }

    @Test
    @DisplayName("Los filtros llegan al servicio, incluidas varias etiquetas separadas por coma")
    void buscarConFiltros() throws Exception {
        ControladorTestSupport.identificarComo(3L, "ESTUDIANTE");
        when(objetoService.buscar(EstadoObjeto.PERDIDO, 1L, null, List.of(2L, 4L), RolUsuario.ESTUDIANTE))
                .thenReturn(List.of(ObjetoDTO.de(Fabrica.objeto(1L, EstadoObjeto.PERDIDO))));

        mockMvc.perform(get("/api/objetos").param("estado", "perdido").param("tipoObjetoId", "1")
                        .param("otrasEtiquetaIds", "2,4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].estado").value("PERDIDO"))
                .andExpect(jsonPath("$[0].modulo.nombre").value("Módulo A"));
    }

    @Test
    @DisplayName("Un estado desconocido responde 400")
    void estadoDesconocido() throws Exception {
        ControladorTestSupport.identificarComo(3L, "ESTUDIANTE");

        mockMvc.perform(get("/api/objetos").param("estado", "ROBADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El estado debe ser PERDIDO o RECLAMADO."));
    }

    @Test
    @DisplayName("Sin estado, el estudiante recibe la advertencia de F-02")
    void estudianteSinEstado() throws Exception {
        ControladorTestSupport.identificarComo(3L, "ESTUDIANTE");
        when(objetoService.buscar(isNull(), isNull(), isNull(), isNull(), eq(RolUsuario.ESTUDIANTE)))
                .thenThrow(ApiException.solicitudInvalida(Mensajes.ESTADO_OBLIGATORIO));

        mockMvc.perform(get("/api/objetos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(Mensajes.ESTADO_OBLIGATORIO));
    }

    @Test
    @DisplayName("Alta con campos inválidos: mensaje de obligatorios y detalle por campo")
    void altaInvalida() throws Exception {
        ControladorTestSupport.identificarComo(1L, "ADMINISTRADOR");

        mockMvc.perform(multipart("/api/objetos")
                        .file(new MockMultipartFile("fotografia", "f.png", "image/png", new byte[]{1}))
                        .param("nombre", "ab")
                        .param("descripcion", "<script>alert(1)</script>")
                        .param("lugarPerdida", "Cafetería"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(Mensajes.CAMPOS_OBLIGATORIOS))
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.descripcion").value("La descripción no admite etiquetas ni código."))
                .andExpect(jsonPath("$.campos.tipoObjetoId").exists())
                .andExpect(jsonPath("$.campos.moduloId").exists());
        verify(objetoService, never()).crear(any(), anyLong());
    }

    @Test
    @DisplayName("Alta válida responde 201 con Location y usa el id del token")
    void altaValida() throws Exception {
        ControladorTestSupport.identificarComo(1L, "ADMINISTRADOR");
        when(objetoService.crear(any(), eq(1L))).thenReturn(ObjetoDTO.de(Fabrica.objeto(7L, EstadoObjeto.PERDIDO)));

        mockMvc.perform(multipart("/api/objetos")
                        .file(new MockMultipartFile("fotografia", "f.png", "image/png", new byte[]{1}))
                        .param("nombre", "Mochila negra")
                        .param("descripcion", "Mochila de dos compartimentos.")
                        .param("lugarPerdida", "Cafetería central")
                        .param("tipoObjetoId", "1")
                        .param("colorId", "1")
                        .param("moduloId", "1")
                        .param("otrasEtiquetaIds", "2", "4"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/objetos/7")))
                .andExpect(jsonPath("$.rutaImagen").value("/uploads/objetos/objeto_01.jpg"));
    }

    @Test
    @DisplayName("Eliminar responde 204")
    void eliminar() throws Exception {
        ControladorTestSupport.identificarComo(1L, "ADMINISTRADOR");

        mockMvc.perform(delete("/api/objetos/4")).andExpect(status().isNoContent());
        verify(objetoService).eliminar(4L, 1L);
    }
}
