package mx.univa.objetosperdidos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import mx.univa.objetosperdidos.dto.PeticionDTO;
import mx.univa.objetosperdidos.dto.PeticionRequest;
import mx.univa.objetosperdidos.entity.EstadoPeticion;
import mx.univa.objetosperdidos.entity.TipoPeticion;
import mx.univa.objetosperdidos.exception.ApiException;
import mx.univa.objetosperdidos.exception.Mensajes;
import mx.univa.objetosperdidos.service.PeticionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class PeticionControllerTest {

    @Mock
    private PeticionService peticionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControladorTestSupport.mockMvc(new PeticionController(peticionService));
    }

    @AfterEach
    void tearDown() {
        ControladorTestSupport.limpiar();
    }

    @Test
    @DisplayName("Crear petición responde 201 con la matrícula del estudiante identificado")
    void crear() throws Exception {
        ControladorTestSupport.identificarComo(5L, "ESTUDIANTE");
        when(peticionService.crear(new PeticionRequest(1L, null), 5L)).thenReturn(new PeticionDTO(9L,
                TipoPeticion.RECLAMO, EstadoPeticion.PENDIENTE, 1L, "Mochila negra", "A2024015",
                "María Fernanda Ibarra", LocalDateTime.of(2026, 11, 26, 10, 0), null));

        mockMvc.perform(post("/api/peticiones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objetoId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/peticiones/9"))
                .andExpect(jsonPath("$.tipo").value("RECLAMO"))
                .andExpect(jsonPath("$.matricula").value("A2024015"));
    }

    @Test
    @DisplayName("Petición duplicada responde 409 con el mensaje de F-03")
    void duplicada() throws Exception {
        ControladorTestSupport.identificarComo(5L, "ESTUDIANTE");
        when(peticionService.crear(any(), eq(5L))).thenThrow(ApiException.conflicto(Mensajes.PETICION_DUPLICADA));

        mockMvc.perform(post("/api/peticiones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objetoId\":1,\"tipo\":\"RECLAMO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(Mensajes.PETICION_DUPLICADA));
    }

    @Test
    @DisplayName("Sin objetoId responde 400")
    void sinObjeto() throws Exception {
        ControladorTestSupport.identificarComo(5L, "ESTUDIANTE");

        mockMvc.perform(post("/api/peticiones").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.objetoId").exists());
    }

    @Test
    @DisplayName("Buscar por matrícula sin resultados devuelve una lista vacía")
    void listarVacio() throws Exception {
        when(peticionService.listar("A2099999")).thenReturn(List.of());

        mockMvc.perform(get("/api/peticiones").param("matricula", "A2099999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Liberar una petición ya liberada responde 409")
    void liberarYaLiberada() throws Exception {
        ControladorTestSupport.identificarComo(1L, "ADMINISTRADOR");
        when(peticionService.liberar(1L, 1L)).thenThrow(ApiException.conflicto(Mensajes.PETICION_YA_LIBERADA));

        mockMvc.perform(put("/api/peticiones/1/liberacion"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(Mensajes.PETICION_YA_LIBERADA));
    }
}
