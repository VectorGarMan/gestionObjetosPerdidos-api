package mx.univa.objetosperdidos.controller;

import jakarta.validation.Valid;
import mx.univa.objetosperdidos.dto.IdentificacionRequest;
import mx.univa.objetosperdidos.dto.IdentificacionResponse;
import mx.univa.objetosperdidos.service.UsuarioService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** PNT-01. Público: devuelve los datos de PNT-02 y el token. */
    @PostMapping("/identificacion")
    public IdentificacionResponse identificar(@Valid @RequestBody IdentificacionRequest request) {
        return usuarioService.identificar(request.correo());
    }
}
