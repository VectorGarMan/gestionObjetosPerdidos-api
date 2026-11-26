package mx.univa.objetosperdidos.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import mx.univa.objetosperdidos.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Firma y verificación de tokens con HS256 y un secreto compartido (APP_JWT_SECRET). */
@Configuration
public class JwtConfig {

    public static final String EMISOR = "objetos-perdidos-api";

    @Bean
    public JwtEncoder jwtEncoder(AppProperties properties) {
        return crearEncoder(properties.jwt().secret());
    }

    @Bean
    public JwtDecoder jwtDecoder(AppProperties properties) {
        return crearDecoder(properties.jwt().secret());
    }

    public static JwtEncoder crearEncoder(String secreto) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave(secreto)));
    }

    public static JwtDecoder crearDecoder(String secreto) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave(secreto))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR));
        return decoder;
    }

    private static SecretKey clave(String secreto) {
        return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
