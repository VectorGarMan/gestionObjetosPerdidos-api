package mx.univa.objetosperdidos.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/** Propiedades «app.*» de application.properties. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Uploads uploads, Cors cors) {

    public record Jwt(String secret, Duration expiracion) {
        public Jwt {
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException(
                        "Define la variable de entorno APP_JWT_SECRET con al menos 32 caracteres.");
            }
            if (expiracion == null) {
                expiracion = Duration.ofHours(8);
            }
        }
    }

    public record Uploads(String directorio, DataSize tamanoMaximo) {
        public Uploads {
            if (directorio == null || directorio.isBlank()) {
                directorio = "uploads";
            }
            if (tamanoMaximo == null) {
                tamanoMaximo = DataSize.ofMegabytes(5);
            }
        }
    }

    public record Cors(List<String> origenesPermitidos) {
        public Cors {
            origenesPermitidos = origenesPermitidos == null ? List.of() : List.copyOf(origenesPermitidos);
        }
    }
}
