package mx.univa.objetosperdidos.service;

import java.util.Locale;

import mx.univa.objetosperdidos.dto.IdentificacionResponse;
import mx.univa.objetosperdidos.dto.UsuarioDTO;
import mx.univa.objetosperdidos.entity.RolUsuario;
import mx.univa.objetosperdidos.entity.Usuario;
import mx.univa.objetosperdidos.exception.ApiException;
import mx.univa.objetosperdidos.exception.Mensajes;
import mx.univa.objetosperdidos.repository.UsuarioRepository;
import mx.univa.objetosperdidos.security.TokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;

    public UsuarioService(UsuarioRepository usuarioRepository, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.tokenService = tokenService;
    }

    /** PNT-01: busca por correo (sin distinguir mayúsculas) y devuelve sus datos y el token. */
    @Transactional(readOnly = true)
    public IdentificacionResponse identificar(String correo) {
        String normalizado = correo.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(normalizado)
                .orElseThrow(() -> ApiException.noEncontrado(Mensajes.CORREO_NO_ENCONTRADO));
        if (!usuario.isActivo() || usuario.getRol() == null) {
            throw ApiException.prohibido(Mensajes.USUARIO_SIN_ROL);
        }
        return new IdentificacionResponse(UsuarioDTO.de(usuario), tokenService.generar(usuario), "Bearer",
                tokenService.segundosDeVigencia());
    }

    /**
     * Vuelve a comprobar en la base que quien trae el token sigue activo y con el rol esperado,
     * por si su cuenta cambió después de emitirse el token.
     */
    @Transactional(readOnly = true)
    public Usuario obtenerActivo(Long id, RolUsuario rolEsperado) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> ApiException.prohibido(Mensajes.USUARIO_SIN_ROL));
        if (usuario.getRol() != rolEsperado) {
            throw ApiException.prohibido(Mensajes.SIN_PERMISO);
        }
        return usuario;
    }
}
