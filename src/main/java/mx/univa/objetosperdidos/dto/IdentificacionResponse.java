package mx.univa.objetosperdidos.dto;

/** Respuesta de POST /api/usuarios/identificacion: datos de PNT-02 y el token para las demás llamadas. */
public record IdentificacionResponse(UsuarioDTO usuario, String token, String tipoToken, long expiraEnSegundos) {
}
