package mx.univa.objetosperdidos.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Publica las fotografías guardadas en disco en la ruta /uploads/** (la misma que guarda ruta_imagen). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path directorio;

    public WebConfig(AppProperties properties) {
        this.directorio = Paths.get(properties.uploads().directorio()).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = directorio.toUri().toString();
        if (!ubicacion.endsWith("/")) {
            ubicacion = ubicacion + "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(ubicacion);
    }
}
