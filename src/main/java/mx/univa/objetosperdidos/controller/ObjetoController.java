package mx.univa.objetosperdidos.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import mx.univa.objetosperdidos.dto.ObjetoDTO;
import mx.univa.objetosperdidos.dto.ObjetoForm;
import mx.univa.objetosperdidos.entity.EstadoObjeto;
import mx.univa.objetosperdidos.security.UsuarioActual;
import mx.univa.objetosperdidos.service.ObjetoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/objetos")
public class ObjetoController {

    private final ObjetoService objetoService;

    public ObjetoController(ObjetoService objetoService) {
        this.objetoService = objetoService;
    }

    /**
     * PNT-03 y PNT-05 (estado=PERDIDO) y PNT-09 (estado=RECLAMADO).
     * otrasEtiquetaIds se repite o se separa por comas: ?otrasEtiquetaIds=1&otrasEtiquetaIds=4 o ?otrasEtiquetaIds=1,4
     */
    @GetMapping
    public List<ObjetoDTO> buscar(@RequestParam(name = "estado", required = false) String estado,
                                  @RequestParam(name = "tipoObjetoId", required = false) Long tipoObjetoId,
                                  @RequestParam(name = "colorId", required = false) Long colorId,
                                  @RequestParam(name = "otrasEtiquetaIds", required = false) List<Long> otrasEtiquetaIds,
                                  @AuthenticationPrincipal Jwt jwt) {
        return objetoService.buscar(EstadoObjeto.desde(estado), tipoObjetoId, colorId, otrasEtiquetaIds,
                UsuarioActual.rol(jwt));
    }

    /** PNT-04, PNT-06 y PNT-09: detalle. */
    @GetMapping("/{id}")
    public ObjetoDTO obtener(@PathVariable("id") Long id) {
        return objetoService.obtener(id);
    }

    /** PNT-07: alta (multipart/form-data con la parte «fotografia»). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjetoDTO> crear(@Valid @ModelAttribute ObjetoForm form, @AuthenticationPrincipal Jwt jwt) {
        ObjetoDTO creado = objetoService.crear(form, UsuarioActual.id(jwt));
        return ResponseEntity.created(URI.create("/api/objetos/" + creado.id())).body(creado);
    }

    /** PNT-06: edición (mismo formulario; «fotografia» opcional). */
    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ObjetoDTO actualizar(@PathVariable("id") Long id, @Valid @ModelAttribute ObjetoForm form,
                                @AuthenticationPrincipal Jwt jwt) {
        return objetoService.actualizar(id, form, UsuarioActual.id(jwt));
    }

    /** PNT-06: eliminación lógica. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
        objetoService.eliminar(id, UsuarioActual.id(jwt));
        return ResponseEntity.noContent().build();
    }
}
