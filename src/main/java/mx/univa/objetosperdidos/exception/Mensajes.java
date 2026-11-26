package mx.univa.objetosperdidos.exception;

/**
 * Textos de la especificación (sección 3, «Prevención de errores y mensajes de advertencia»).
 * Se centralizan para que el cliente pueda mostrarlos sin traducirlos.
 */
public final class Mensajes {

    private Mensajes() {
    }

    // F-01 Identificación (PNT-01)
    public static final String CORREO_VACIO = "Ingresa tu correo para continuar.";
    public static final String CORREO_INVALIDO = "Escribe un correo con formato válido.";
    public static final String CORREO_NO_ENCONTRADO =
            "No encontramos una cuenta con ese correo. Verifica que sea el correo registrado en la universidad.";
    public static final String USUARIO_SIN_ROL =
            "Tu cuenta no tiene un rol asignado para usar la plataforma. Acude con el personal administrador.";

    // F-02 Filtros (PNT-03 y PNT-05)
    public static final String ESTADO_OBLIGATORIO = "Selecciona «Perdidos» o «Reclamados» para ver los objetos.";
    public static final String ERROR_CARGA = "No pudimos cargar los objetos. Inténtalo de nuevo en unos momentos.";

    // F-03 Peticiones (PNT-04)
    public static final String PETICION_DUPLICADA = "Ya tienes una petición pendiente para este objeto.";
    public static final String OBJETO_CAMBIO_ESTADO =
            "Este objeto cambió de estado. Actualiza la pantalla para ver su situación actual.";
    public static final String RECLAMO_ERROR_PROPIO =
            "Este objeto ya aparece como entregado a ti; no puedes reportar un reclamo de error sobre él.";

    // F-04 Alta y edición (PNT-07 y PNT-06)
    public static final String IMAGEN_INVALIDA = "Formato o tamaño no permitido. Usa una imagen JPG o PNG de máximo 5 MB.";
    public static final String CAMPOS_OBLIGATORIOS =
            "Completa los campos obligatorios marcados con * para registrar el objeto.";
    public static final String OBJETO_NO_ENCONTRADO = "El objeto no existe o fue eliminado.";

    // F-05 Peticiones del administrador (PNT-08)
    public static final String MATRICULA_INVALIDA = "Escribe una matrícula válida (solo letras y números).";
    public static final String PETICION_YA_LIBERADA = "Esta petición ya fue liberada. Actualiza la lista para ver su estado.";
    public static final String PETICION_NO_ENCONTRADA = "La petición no existe.";
    public static final String CONTACTO_SOLO_RECLAMO_ERROR =
            "Los datos de contacto solo están disponibles en los reclamos de error.";
    public static final String CONTACTO_AVISO = "Estos datos de contacto son solo para resolver este caso.";

    // Seguridad
    public static final String NO_AUTENTICADO = "Tu sesión no es válida o expiró. Identifícate de nuevo.";
    public static final String SIN_PERMISO = "No tienes permiso para realizar esta acción.";
    public static final String ERROR_INTERNO = "No pudimos procesar la solicitud. Inténtalo de nuevo en unos momentos.";
}
