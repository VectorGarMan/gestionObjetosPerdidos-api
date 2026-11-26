package mx.univa.objetosperdidos.repository;

import java.util.Optional;

import mx.univa.objetosperdidos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);
}
