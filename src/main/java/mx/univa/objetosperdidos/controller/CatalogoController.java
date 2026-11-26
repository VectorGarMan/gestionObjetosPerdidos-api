package mx.univa.objetosperdidos.controller;

import java.util.List;

import mx.univa.objetosperdidos.dto.CatalogoDTO;
import mx.univa.objetosperdidos.service.CatalogoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /** PNT-03, PNT-05 y PNT-07. tipo: tipos-objeto | colores | etiquetas | modulos. */
    @GetMapping("/{tipo}")
    public List<CatalogoDTO> listar(@PathVariable("tipo") String tipo) {
        return catalogoService.listar(tipo);
    }
}
