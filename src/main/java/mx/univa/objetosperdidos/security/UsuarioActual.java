package mx.univa.objetosperdidos.security;

import mx.univa.objetosperdidos.entity.RolUsuario;
import org.springframework.security.oauth2.jwt.Jwt;

/** Lectura de los datos del usuario identificado desde su token. */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    public static RolUsuario rol(Jwt jwt) {
        return RolUsuario.valueOf(jwt.getClaimAsString(TokenService.CLAIM_ROL));
    }
}
