package mx.univa.objetosperdidos.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import mx.univa.objetosperdidos.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Convierte cualquier error en ErrorResponse con un mensaje comprensible y sin detalles técnicos
 * (regla de calidad de la sección 3 de la especificación).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> negocio(ApiException ex) {
        return respuesta(ex.getStatus(), ex.getMessage(), ex.getCampos());
    }

    /** Errores de @Valid, tanto en JSON (MethodArgumentNotValidException) como en multipart. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacionCuerpo(MethodArgumentNotValidException ex, HttpServletRequest request) {
        return validacion(ex.getBindingResult(), request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> validacionFormulario(BindException ex, HttpServletRequest request) {
        return validacion(ex.getBindingResult(), request);
    }

    private static ResponseEntity<ErrorResponse> validacion(BindingResult resultado, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : resultado.getFieldErrors()) {
            String mensaje = error.isBindingFailure()
                    ? "El valor de «" + error.getField() + "» no es válido."
                    : error.getDefaultMessage();
            campos.putIfAbsent(error.getField(), mensaje);
        }
        String mensaje;
        if (request.getRequestURI().startsWith("/api/objetos")) {
            mensaje = Mensajes.CAMPOS_OBLIGATORIOS;
        } else if (campos.size() == 1) {
            mensaje = campos.values().iterator().next();
        } else {
            mensaje = "Revisa los campos marcados.";
        }
        return respuesta(HttpStatus.BAD_REQUEST, mensaje, campos.isEmpty() ? null : campos);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> archivoGrande(MaxUploadSizeExceededException ex) {
        return respuesta(HttpStatus.PAYLOAD_TOO_LARGE, Mensajes.IMAGEN_INVALIDA, Map.of("fotografia", Mensajes.IMAGEN_INVALIDA));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> multipart(MultipartException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Envía los datos del objeto como multipart/form-data.", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "El valor de «" + ex.getName() + "» no es válido.", null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> parametroFaltante(MissingServletRequestParameterException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Falta el parámetro «" + ex.getParameterName() + "».", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoInvalido(HttpMessageNotReadableException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodo(HttpRequestMethodNotSupportedException ex) {
        return respuesta(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido en esta ruta.", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> mediaType(HttpMediaTypeNotSupportedException ex) {
        return respuesta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado.", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noExiste(NoResourceFoundException ex) {
        return respuesta(HttpStatus.NOT_FOUND, "El recurso solicitado no existe.", null);
    }

    /** Última defensa: las restricciones de la base (por ejemplo, dos peticiones simultáneas). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex) {
        String detalle = String.valueOf(ex.getMostSpecificCause().getMessage());
        if (detalle.contains("uq_peticion_pendiente")) {
            return respuesta(HttpStatus.CONFLICT, Mensajes.PETICION_DUPLICADA, null);
        }
        log.warn("Violación de integridad: {}", detalle);
        return respuesta(HttpStatus.CONFLICT, "La operación no cumple con las reglas de los datos.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, Mensajes.ERROR_INTERNO, null);
    }

    private static ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String mensaje, Map<String, String> campos) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), mensaje, campos, LocalDateTime.now()));
    }
}
