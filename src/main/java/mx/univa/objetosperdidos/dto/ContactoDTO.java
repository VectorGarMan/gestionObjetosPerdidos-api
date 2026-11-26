package mx.univa.objetosperdidos.dto;

import mx.univa.objetosperdidos.entity.Usuario;

/** Datos del estudiante que reclamó antes el objeto (botón «Contactar estudiante», PNT-08). */
public record ContactoDTO(String nombre, String matricula, String correo, String telefono, String aviso) {

    public static ContactoDTO de(Usuario u, String aviso) {
        return new ContactoDTO(u.getNombre(), u.getMatricula(), u.getCorreo(), u.getTelefono(), aviso);
    }
}
