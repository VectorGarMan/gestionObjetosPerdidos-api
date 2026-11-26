package mx.univa.objetosperdidos.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/** Objeto perdido. "estado" es el estado de negocio; "estatus" el borrado lógico. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "objeto")
public class Objeto extends EntidadAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "descripcion", nullable = false, length = 250)
    private String descripcion;

    @Column(name = "marca", length = 40)
    private String marca;

    @Column(name = "lugar_perdida", nullable = false, length = 80)
    private String lugarPerdida;

    /** Solo la ruta pública de la fotografía, nunca el binario. */
    @Column(name = "ruta_imagen", nullable = false, length = 255)
    private String rutaImagen;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoObjeto estado = EstadoObjeto.PERDIDO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_objeto_id", nullable = false)
    private TipoObjeto tipoObjeto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "color_id", nullable = false)
    private Color color;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modulo_id", nullable = false)
    private Modulo modulo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reclamado_por_id")
    private Usuario reclamadoPor;

    @Column(name = "fecha_reclamo")
    private LocalDateTime fechaReclamo;

    /** Relación N:M con su propia tabla (objeto_etiqueta), que también usa borrado lógico. */
    @BatchSize(size = 50)
    @OneToMany(mappedBy = "objeto", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<ObjetoEtiqueta> etiquetas = new ArrayList<>();
}
