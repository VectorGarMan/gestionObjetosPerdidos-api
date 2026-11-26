package mx.univa.objetosperdidos.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import mx.univa.objetosperdidos.dto.ContactoDTO;
import mx.univa.objetosperdidos.dto.PeticionDTO;
import mx.univa.objetosperdidos.dto.PeticionDetalleDTO;
import mx.univa.objetosperdidos.dto.PeticionRequest;
import mx.univa.objetosperdidos.security.UsuarioActual;
import mx.univa.objetosperdidos.service.PeticionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/peticiones")
public class PeticionController {

    private final PeticionService peticionService;

    public PeticionController(PeticionService peticionService) {
        this.peticionService = peticionService;
    }

    /** PNT-04 (estudiante): reclamo o reclamo de error. */
    @PostMapping
    public ResponseEntity<PeticionDTO> crear(@Valid @RequestBody PeticionRequest request,
                                             @AuthenticationPrincipal Jwt jwt) {
        PeticionDTO creada = peticionService.crear(request, UsuarioActual.id(jwt));
        return ResponseEntity.created(URI.create("/api/peticiones/" + creada.id())).body(creada);
    }

    /** PNT-08: lista, con filtro opcional por matrícula. */
    @GetMapping
    public List<PeticionDTO> listar(@RequestParam(name = "matricula", required = false) String matricula) {
        return peticionService.listar(matricula);
    }

    @GetMapping("/{id}")
    public PeticionDetalleDTO obtener(@PathVariable("id") Long id) {
        return peticionService.obtener(id);
    }

    /** PNT-08 «Contactar estudiante» (solo reclamo de error). */
    @GetMapping("/{id}/contacto-previo")
    public ContactoDTO contactoPrevio(@PathVariable("id") Long id) {
        return peticionService.contactoPrevio(id);
    }

    /** PNT-08 «Liberar». */
    @PutMapping("/{id}/liberacion")
    public PeticionDetalleDTO liberar(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
        return peticionService.liberar(id, UsuarioActual.id(jwt));
    }
}
