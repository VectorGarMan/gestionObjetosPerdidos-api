package mx.univa.objetosperdidos.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mx.univa.objetosperdidos.dto.IdentificacionResponse;
import mx.univa.objetosperdidos.dto.UsuarioDTO;
import mx.univa.objetosperdidos.entity.RolUsuario;
import mx.univa.objetosperdidos.exception.ApiException;
import mx.univa.objetosperdidos.exception.Mensajes;
import mx.univa.objetosperdidos.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControladorTestSupport.mockMvc(new UsuarioController(usuarioService));
    }

    private org.springframework.test.web.servlet.ResultActions enviar(String json) throws Exception {
        return mockMvc.perform(post("/api/usuarios/identificacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    @DisplayName("Correo vacío: «Ingresa tu correo para continuar.»")
    void correoVacio() throws Exception {
        enviar("{\"correo\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(Mensajes.CORREO_VACIO));
        verify(usuarioService, never()).identificar(anyString());
    }

    @Test
    @DisplayName("Correo con formato inválido: «Escribe un correo con formato válido.»")
    void correoInvalido() throws Exception {
        enviar("{\"correo\":\"ana.torres\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(Mensajes.CORREO_INVALIDO))
                .andExpect(jsonPath("$.campos.correo").value(Mensajes.CORREO_INVALIDO));
    }

    @Test
    @DisplayName("Correo válido devuelve los datos de PNT-02 y el token")
    void correoValido() throws Exception {
        when(usuarioService.identificar("ana.torres@alumnos.univa.mx")).thenReturn(new IdentificacionResponse(
                new UsuarioDTO(3L, "Ana Torres Gómez", "ana.torres@alumnos.univa.mx", "A2023001",
                        RolUsuario.ESTUDIANTE), "abc.def.ghi", "Bearer", 28800));

        enviar("{\"correo\":\"ana.torres@alumnos.univa.mx\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.usuario.matricula").value("A2023001"))
                .andExpect(jsonPath("$.token").value("abc.def.ghi"));
    }

    @Test
    @DisplayName("Correo no registrado responde 404 con el mensaje de la especificación")
    void correoNoRegistrado() throws Exception {
        when(usuarioService.identificar("nadie@univa.mx"))
                .thenThrow(ApiException.noEncontrado(Mensajes.CORREO_NO_ENCONTRADO));

        enviar("{\"correo\":\"nadie@univa.mx\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value(Mensajes.CORREO_NO_ENCONTRADO));
    }

    @Test
    @DisplayName("Un cuerpo que no es JSON responde 400 sin detalles técnicos")
    void cuerpoInvalido() throws Exception {
        enviar("esto no es json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El cuerpo de la solicitud no es un JSON válido."));
    }
}
