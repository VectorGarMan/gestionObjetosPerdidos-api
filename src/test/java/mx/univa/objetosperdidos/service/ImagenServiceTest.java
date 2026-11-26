package mx.univa.objetosperdidos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import mx.univa.objetosperdidos.Fabrica;
import mx.univa.objetosperdidos.exception.Mensajes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class ImagenServiceTest {

    /** Misma expresión que el CHECK ck_objeto_ruta_imagen de la base. */
    private static final String PATRON_BD = "(?i)^/uploads/objetos/[A-Za-z0-9_\\-]+\\.(jpg|jpeg|png)$";

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 0, 0};

    @TempDir
    Path carpeta;

    private ImagenService servicio(DataSize maximo) {
        return new ImagenService(Fabrica.propiedades(carpeta.toString(), maximo));
    }

    @Test
    @DisplayName("Guarda un PNG y devuelve una ruta que acepta la base de datos")
    void guardaPng() {
        String ruta = servicio(DataSize.ofMegabytes(5))
                .guardar(new MockMultipartFile("fotografia", "foto.PNG", "image/png", PNG));

        assertThat(ruta).matches(PATRON_BD).endsWith(".png");
        assertThat(Files.exists(carpeta.resolve("objetos").resolve(ruta.substring("/uploads/objetos/".length()))))
                .isTrue();
    }

    @Test
    @DisplayName("Un .jpeg se guarda con extensión .jpg")
    void guardaJpeg() {
        String ruta = servicio(DataSize.ofMegabytes(5))
                .guardar(new MockMultipartFile("fotografia", "foto.jpeg", "image/jpeg", JPG));

        assertThat(ruta).matches(PATRON_BD).endsWith(".jpg");
    }

    @Test
    @DisplayName("Rechaza un archivo cuyo contenido no es la imagen que dice su extensión")
    void rechazaContenidoFalso() {
        assertThatThrownBy(() -> servicio(DataSize.ofMegabytes(5))
                .guardar(new MockMultipartFile("fotografia", "foto.png", "image/png", GIF)))
                .hasMessage(Mensajes.IMAGEN_INVALIDA);
    }

    @Test
    @DisplayName("Rechaza formatos distintos de JPG o PNG")
    void rechazaGif() {
        assertThatThrownBy(() -> servicio(DataSize.ofMegabytes(5))
                .guardar(new MockMultipartFile("fotografia", "foto.gif", "image/gif", GIF)))
                .hasMessage(Mensajes.IMAGEN_INVALIDA);
    }

    @Test
    @DisplayName("Rechaza imágenes que superan el tamaño máximo")
    void rechazaTamano() {
        assertThatThrownBy(() -> servicio(DataSize.ofBytes(5))
                .guardar(new MockMultipartFile("fotografia", "foto.png", "image/png", PNG)))
                .hasMessage(Mensajes.IMAGEN_INVALIDA);
    }

    @Test
    @DisplayName("Rechaza un archivo vacío")
    void rechazaVacio() {
        assertThatThrownBy(() -> servicio(DataSize.ofMegabytes(5))
                .guardar(new MockMultipartFile("fotografia", "foto.png", "image/png", new byte[0])))
                .hasMessage(Mensajes.IMAGEN_INVALIDA);
    }

    @Test
    @DisplayName("eliminar no borra archivos fuera de la carpeta de fotografías")
    void eliminarNoSaleDeLaCarpeta() throws Exception {
        Path ajeno = Files.writeString(carpeta.resolve("ajeno.txt"), "no borrar");

        servicio(DataSize.ofMegabytes(5)).eliminar("/uploads/objetos/../ajeno.txt");

        assertThat(Files.exists(ajeno)).isTrue();
    }

    @Test
    @DisplayName("eliminar borra la fotografía guardada")
    void eliminarBorra() {
        ImagenService servicio = servicio(DataSize.ofMegabytes(5));
        String ruta = servicio.guardar(new MockMultipartFile("fotografia", "foto.png", "image/png", PNG));

        servicio.eliminar(ruta);

        assertThat(carpeta.resolve("objetos")).isEmptyDirectory();
    }
}
