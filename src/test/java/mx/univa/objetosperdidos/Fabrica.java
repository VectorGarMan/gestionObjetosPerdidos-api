package mx.univa.objetosperdidos;

import java.time.LocalDateTime;

import mx.univa.objetosperdidos.config.AppProperties;
import mx.univa.objetosperdidos.entity.Color;
import mx.univa.objetosperdidos.entity.EstadoObjeto;
import mx.univa.objetosperdidos.entity.EstadoPeticion;
import mx.univa.objetosperdidos.entity.Etiqueta;
import mx.univa.objetosperdidos.entity.Modulo;
import mx.univa.objetosperdidos.entity.Objeto;
import mx.univa.objetosperdidos.entity.Peticion;
import mx.univa.objetosperdidos.entity.RolUsuario;
import mx.univa.objetosperdidos.entity.TipoObjeto;
import mx.univa.objetosperdidos.entity.TipoPeticion;
import mx.univa.objetosperdidos.entity.Usuario;
import org.springframework.util.unit.DataSize;

/** Datos de prueba basados en los registros de database/base_datos_proyecto.sql. */
public final class Fabrica {

    public static final String SECRETO = "secreto-de-pruebas-con-mas-de-32-caracteres";

    private Fabrica() {
    }

    public static AppProperties propiedades(String directorio, DataSize tamanoMaximo) {
        return new AppProperties(new AppProperties.Jwt(SECRETO, null),
                new AppProperties.Uploads(directorio, tamanoMaximo), new AppProperties.Cors(null));
    }

    public static Usuario administrador() {
        return usuario(1L, "Laura Méndez Ruiz", "laura.mendez@univa.mx", null, RolUsuario.ADMINISTRADOR);
    }

    public static Usuario estudiante(Long id, String matricula) {
        return usuario(id, "Estudiante " + id, "estudiante" + id + "@alumnos.univa.mx", matricula, RolUsuario.ESTUDIANTE);
    }

    public static Usuario usuario(Long id, String nombre, String correo, String matricula, RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setMatricula(matricula);
        u.setTelefono("3312345600");
        u.setRol(rol);
        return u;
    }

    public static TipoObjeto tipo(Long id) {
        TipoObjeto t = new TipoObjeto();
        t.setId(id);
        t.setNombre("Mochila");
        return t;
    }

    public static Color color(Long id) {
        Color c = new Color();
        c.setId(id);
        c.setNombre("Negro");
        c.setCodigoHex("#000000");
        return c;
    }

    public static Modulo modulo(Long id) {
        Modulo m = new Modulo();
        m.setId(id);
        m.setNombre("Módulo A");
        m.setUbicacion("Edificio A");
        return m;
    }

    public static Etiqueta etiqueta(Long id, String nombre) {
        Etiqueta e = new Etiqueta();
        e.setId(id);
        e.setNombre(nombre);
        return e;
    }

    public static Objeto objeto(Long id, EstadoObjeto estado) {
        Objeto o = new Objeto();
        o.setId(id);
        o.setNombre("Mochila negra");
        o.setDescripcion("Mochila de dos compartimentos.");
        o.setLugarPerdida("Cafetería central");
        o.setRutaImagen("/uploads/objetos/objeto_01.jpg");
        o.setEstado(estado);
        o.setTipoObjeto(tipo(1L));
        o.setColor(color(1L));
        o.setModulo(modulo(1L));
        o.setRegistradoPor(administrador());
        return o;
    }

    public static Objeto objetoReclamadoPor(Long id, Usuario reclamadoPor) {
        Objeto o = objeto(id, EstadoObjeto.RECLAMADO);
        o.setReclamadoPor(reclamadoPor);
        o.setFechaReclamo(LocalDateTime.of(2026, 11, 20, 10, 0));
        return o;
    }

    public static Peticion peticion(Long id, Objeto objeto, Usuario solicitante, TipoPeticion tipo) {
        Peticion p = new Peticion();
        p.setId(id);
        p.setObjeto(objeto);
        p.setSolicitante(solicitante);
        p.setTipo(tipo);
        p.setEstado(EstadoPeticion.PENDIENTE);
        return p;
    }
}
