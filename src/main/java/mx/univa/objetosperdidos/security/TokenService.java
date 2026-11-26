package mx.univa.objetosperdidos.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import mx.univa.objetosperdidos.config.AppProperties;
import mx.univa.objetosperdidos.entity.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Genera el JWT que se entrega tras la identificación por correo. */
@Service
public class TokenService {

    public static final String CLAIM_ROL = "rol";

    private final JwtEncoder encoder;
    private final Duration expiracion;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, AppProperties properties, Clock clock) {
        this.encoder = encoder;
        this.expiracion = properties.jwt().expiracion();
        this.clock = clock;
    }

    public String generar(Usuario usuario) {
        Instant ahora = clock.instant();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.EMISOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(ahora)
                .expiresAt(ahora.plus(expiracion))
                .claim(CLAIM_ROL, usuario.getRol().name())
                .claim("nombre", usuario.getNombre())
                .claim("correo", usuario.getCorreo());
        if (usuario.getMatricula() != null) {
            claims.claim("matricula", usuario.getMatricula());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    public long segundosDeVigencia() {
        return expiracion.toSeconds();
    }
}
