package mx.univa.objetosperdidos.dto;

import mx.univa.objetosperdidos.entity.RolUsuario;
import mx.univa.objetosperdidos.entity.Usuario;

/** Datos esenciales que se muestran en PNT-02. */
public record UsuarioDTO(Long id, String nombre, String correo, String matricula, RolUsuario rol) {

    public static UsuarioDTO de(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getNombre(), u.getCorreo(), u.getMatricula(), u.getRol());
    }
}
