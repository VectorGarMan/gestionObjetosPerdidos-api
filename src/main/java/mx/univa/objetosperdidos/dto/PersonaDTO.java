package mx.univa.objetosperdidos.dto;

import mx.univa.objetosperdidos.entity.Usuario;

/** Datos de una persona dentro del detalle de una petición (solo para el administrador). */
public record PersonaDTO(Long id, String nombre, String matricula, String correo) {

    public static PersonaDTO de(Usuario u) {
        return new PersonaDTO(u.getId(), u.getNombre(), u.getMatricula(), u.getCorreo());
    }
}
