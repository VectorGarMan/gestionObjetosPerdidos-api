package mx.univa.objetosperdidos.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletResponse;
import mx.univa.objetosperdidos.exception.Mensajes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Responde 401/403 con el mismo formato JSON que ErrorResponse. */
@Component
public class RespuestaJsonSeguridad {

    public AuthenticationEntryPoint noAutenticado() {
        return (request, response, ex) -> escribir(response, HttpStatus.UNAUTHORIZED, Mensajes.NO_AUTENTICADO);
    }

    public AccessDeniedHandler sinPermiso() {
        return (request, response, ex) -> escribir(response, HttpStatus.FORBIDDEN, Mensajes.SIN_PERMISO);
    }

    private static void escribir(HttpServletResponse response, HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        response.getWriter().write("{\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"mensaje\":\"" + mensaje
                + "\",\"timestamp\":\"" + LocalDateTime.now() + "\"}");
    }
}
