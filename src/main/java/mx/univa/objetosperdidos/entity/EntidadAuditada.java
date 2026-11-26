package mx.univa.objetosperdidos.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * Columnas de control presentes en todas las tablas.
 * fecha_registro la asigna el DEFAULT de la base y fecha_actualizacion el trigger
 * fn_actualizar_fecha_actualizacion(); Hibernate solo las lee.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class EntidadAuditada {

    /** Borrado lógico: TRUE = activo, FALSE = eliminado. */
    @Column(name = "estatus", nullable = false)
    private Boolean estatus = Boolean.TRUE;

    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_registro", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "fecha_actualizacion", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    public boolean isActivo() {
        return Boolean.TRUE.equals(estatus);
    }
}
