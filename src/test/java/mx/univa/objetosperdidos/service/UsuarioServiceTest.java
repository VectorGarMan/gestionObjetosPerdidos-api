package mx.univa.objetosperdidos.service;

import static mx.univa.objetosperdidos.Fabrica.administrador;
import static mx.univa.objetosperdidos.Fabrica.estudiante;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import mx.univa.objetosperdidos.dto.IdentificacionResponse;
import mx.univa.objetosperdidos.entity.RolUsuario;
import mx.univa.objetosperdidos.entity.Usuario;
import mx.univa.objetosperdidos.exception.Mensajes;
import mx.univa.objetosperdidos.repository.UsuarioRepository;
import mx.univa.objetosperdidos.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TokenService tokenService;

    @InjectMocks
    private UsuarioService service;

    @Test
    @DisplayName("Identifica por correo sin distinguir mayúsculas ni espacios y devuelve el token")
    void identificaYDevuelveToken() {
        Usuario ana = estudiante(3L, "A2023001");
        when(usuarioRepository.findByCorreoIgnoreCase("ana.torres@alumnos.univa.mx")).thenReturn(Optional.of(ana));
        when(tokenService.generar(ana)).thenReturn("token-de-prueba");
        when(tokenService.segundosDeVigencia()).thenReturn(28800L);

        IdentificacionResponse respuesta = service.identificar("  Ana.Torres@Alumnos.UNIVA.mx ");

        assertThat(respuesta.usuario().matricula()).isEqualTo("A2023001");
        assertThat(respuesta.usuario().rol()).isEqualTo(RolUsuario.ESTUDIANTE);
        assertThat(respuesta.token()).isEqualTo("token-de-prueba");
        assertThat(respuesta.tipoToken()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Un correo que no existe responde 404 sin revelar datos de otras cuentas")
    void correoNoEncontrado() {
        when(usuarioRepository.findByCorreoIgnoreCase(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.identificar("nadie@univa.mx"))
                .hasMessage(Mensajes.CORREO_NO_ENCONTRADO)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        verify(tokenService, never()).generar(any());
    }

    @Test
    @DisplayName("Una cuenta desactivada no puede entrar")
    void cuentaInactiva() {
        Usuario inactivo = administrador();
        inactivo.setEstatus(false);
        when(usuarioRepository.findByCorreoIgnoreCase(any())).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> service.identificar("laura.mendez@univa.mx"))
                .hasMessage(Mensajes.USUARIO_SIN_ROL)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("obtenerActivo rechaza un rol distinto al esperado")
    void rolDistinto() {
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(estudiante(3L, "A2023001")));

        assertThatThrownBy(() -> service.obtenerActivo(3L, RolUsuario.ADMINISTRADOR))
                .hasMessage(Mensajes.SIN_PERMISO);
    }
}
