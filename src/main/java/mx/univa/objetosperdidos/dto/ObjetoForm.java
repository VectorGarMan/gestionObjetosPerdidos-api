package mx.univa.objetosperdidos.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * ObjetoDTO de entrada (F-04). Se recibe como multipart/form-data en
 * POST /api/objetos y PUT /api/objetos/{id}. La fecha de registro la asigna la base.
 */
@Getter
@Setter
public class ObjetoForm {

    /** Obligatoria en el alta; opcional en la edición (si no se envía se conserva la actual). */
    private MultipartFile fotografia;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres.")
    @Pattern(regexp = "^\\s*[\\p{L}\\p{N}]([\\p{L}\\p{N} ]*[\\p{L}\\p{N}])?\\s*$",
            message = "El nombre solo admite letras, números y espacios.")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(min = 10, max = 250, message = "La descripción debe tener entre 10 y 250 caracteres.")
    @Pattern(regexp = "^[^<>]*$", message = "La descripción no admite etiquetas ni código.")
    private String descripcion;

    @Size(max = 40, message = "La marca admite máximo 40 caracteres.")
    private String marca;

    @NotNull(message = "Selecciona el tipo de objeto.")
    private Long tipoObjetoId;

    @NotNull(message = "Selecciona el color.")
    private Long colorId;

    private List<Long> otrasEtiquetaIds = new ArrayList<>();

    @NotBlank(message = "El lugar de pérdida es obligatorio.")
    @Size(min = 3, max = 80, message = "El lugar de pérdida debe tener entre 3 y 80 caracteres.")
    private String lugarPerdida;

    @NotNull(message = "Selecciona el módulo de resguardo.")
    private Long moduloId;
}
