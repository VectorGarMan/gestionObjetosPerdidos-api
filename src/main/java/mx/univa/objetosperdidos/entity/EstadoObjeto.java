package mx.univa.objetosperdidos.entity;

import java.util.Locale;

import mx.univa.objetosperdidos.exception.ApiException;

/** Estado de negocio del objeto (columna objeto.estado). */
public enum EstadoObjeto {
    PERDIDO, RECLAMADO;

    /** Convierte el parámetro de consulta sin distinguir mayúsculas; null o vacío devuelve null. */
    public static EstadoObjeto desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.solicitudInvalida("El estado debe ser PERDIDO o RECLAMADO.");
        }
    }
}
