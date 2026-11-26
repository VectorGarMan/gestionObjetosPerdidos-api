package mx.univa.objetosperdidos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;

import mx.univa.objetosperdidos.Fabrica;
import mx.univa.objetosperdidos.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.unit.DataSize;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService(JwtConfig.crearEncoder(Fabrica.SECRETO),
            Fabrica.propiedades("uploads", DataSize.ofMegabytes(5)), Clock.systemUTC());
    private final JwtDecoder decoder = JwtConfig.crearDecoder(Fabrica.SECRETO);

    @Test
    @DisplayName("El token generado se valida y contiene el id y el rol del usuario")
    void tokenValido() {
        Usuario ana = Fabrica.estudiante(3L, "A2023001");

        Jwt jwt = decoder.decode(tokenService.generar(ana));

        assertThat(jwt.getSubject()).isEqualTo("3");
        assertThat(jwt.getClaimAsString(TokenService.CLAIM_ROL)).isEqualTo("ESTUDIANTE");
        assertThat(jwt.getClaimAsString("matricula")).isEqualTo("A2023001");
        assertThat(UsuarioActual.id(jwt)).isEqualTo(3L);
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    @DisplayName("El rol del token se convierte en ROLE_ADMINISTRADOR")
    void rolComoAutoridad() {
        Jwt jwt = decoder.decode(tokenService.generar(Fabrica.administrador()));

        AbstractAuthenticationToken autenticacion = SecurityConfig.convertidorRoles().convert(jwt);

        // Spring Security 7.1 agrega además FACTOR_BEARER (indica que entró con token)
        assertThat(autenticacion.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMINISTRADOR")
                .doesNotContain("ROLE_ESTUDIANTE");
    }

    @Test
    @DisplayName("Un token firmado con otro secreto se rechaza")
    void otroSecreto() {
        String token = tokenService.generar(Fabrica.administrador());
        JwtDecoder otro = JwtConfig.crearDecoder("otro-secreto-distinto-tambien-de-32-caracteres");

        assertThatThrownBy(() -> otro.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Un secreto de menos de 32 caracteres impide arrancar la API")
    void secretoCorto() {
        assertThatThrownBy(() -> new mx.univa.objetosperdidos.config.AppProperties.Jwt("corto", null))
                .isInstanceOf(IllegalStateException.class);
    }
}
