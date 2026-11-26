package mx.univa.objetosperdidos.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import mx.univa.objetosperdidos.config.AppProperties;
import mx.univa.objetosperdidos.exception.ApiException;
import mx.univa.objetosperdidos.exception.Mensajes;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda las fotografías en disco. En la base solo se guarda la ruta pública
 * (/uploads/objetos/{uuid}.jpg|png), que cumple el CHECK ck_objeto_ruta_imagen.
 */
@Slf4j
@Service
public class ImagenService {

    public static final String PREFIJO_RUTA = "/uploads/objetos/";

    private final Path carpetaObjetos;
    private final long tamanoMaximo;

    public ImagenService(AppProperties properties) {
        this.carpetaObjetos = Paths.get(properties.uploads().directorio()).toAbsolutePath().normalize()
                .resolve("objetos");
        this.tamanoMaximo = properties.uploads().tamanoMaximo().toBytes();
    }

    /** Valida formato real (no solo la extensión) y tamaño; devuelve la ruta para ruta_imagen. */
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > tamanoMaximo) {
            throw invalida();
        }
        String extension = extensionPermitida(archivo.getOriginalFilename());
        byte[] cabecera = leerCabecera(archivo);
        String formatoReal = esJpg(cabecera) ? "jpg" : esPng(cabecera) ? "png" : null;
        if (formatoReal == null || !formatoReal.equals(extension)) {
            throw invalida();
        }
        String nombre = UUID.randomUUID() + "." + formatoReal;
        try (InputStream entrada = archivo.getInputStream()) {
            Files.createDirectories(carpetaObjetos);
            Files.copy(entrada, carpetaObjetos.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la fotografía", e);
        }
        return PREFIJO_RUTA + nombre;
    }

    /** Borra un archivo guardado por este servicio (se usa si la transacción falla). */
    public void eliminar(String ruta) {
        if (ruta == null || !ruta.startsWith(PREFIJO_RUTA)) {
            return;
        }
        Path archivo = carpetaObjetos.resolve(ruta.substring(PREFIJO_RUTA.length())).normalize();
        if (!archivo.startsWith(carpetaObjetos)) {
            return;
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            log.warn("No se pudo borrar la fotografía {}", archivo, e);
        }
    }

    private static String extensionPermitida(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            throw invalida();
        }
        String ext = nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "jpg", "jpeg" -> "jpg";
            case "png" -> "png";
            default -> throw invalida();
        };
    }

    private static byte[] leerCabecera(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream()) {
            return entrada.readNBytes(8);
        } catch (IOException e) {
            throw invalida();
        }
    }

    private static boolean esJpg(byte[] b) {
        return b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean esPng(byte[] b) {
        return b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A;
    }

    private static ApiException invalida() {
        return ApiException.solicitudInvalida(Mensajes.IMAGEN_INVALIDA);
    }
}
