package mx.univa.objetosperdidos.exception;

import java.util.Map;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Error de negocio con un mensaje pensado para mostrarse tal cual al usuario. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> campos;

    public ApiException(HttpStatus status, String mensaje, Map<String, String> campos) {
        super(mensaje);
        this.status = status;
        this.campos = campos;
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, mensaje, null);
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, mensaje, null);
    }

    public static ApiException solicitudInvalida(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje, null);
    }

    public static ApiException solicitudInvalida(String mensaje, Map<String, String> campos) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje, campos);
    }

    public static ApiException prohibido(String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, mensaje, null);
    }
}
