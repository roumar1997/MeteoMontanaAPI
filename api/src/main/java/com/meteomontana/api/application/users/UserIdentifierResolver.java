package com.meteomontana.api.application.users;

import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Traduce el identificador que llega por la API — que puede ser el uid o el
 * username — al usuario real.
 *
 * <p>Existe porque las apps no siempre conocen el uid: al tocar una mención
 * {@code @usuario} en el feed solo tienen el username, y con él abren el
 * perfil entero (seguir, ver publicaciones, bloquear). Antes esta resolución
 * estaba copiada a mano en un par de sitios, así que los endpoints escritos
 * después nacían aceptando únicamente uid y fallaban en silencio. Con un
 * único punto, todo endpoint que lo use acepta las dos formas.
 */
@Service
@RequiredArgsConstructor
public class UserIdentifierResolver {

    private final UserRepository users;

    /** El usuario, buscando primero por uid y luego por username. */
    public Optional<User> find(String identifier) {
        if (identifier == null || identifier.isBlank()) return Optional.empty();
        return users.findByUid(identifier).or(() -> users.findByUsername(identifier));
    }

    /** Como {@link #find}, pero 404 si no existe. */
    public User require(String identifier) {
        return find(identifier).orElseThrow(() -> new UserNotFoundException(identifier));
    }

    /** El uid del usuario; 404 si el identificador no corresponde a nadie. */
    public String requireUid(String identifier) {
        return require(identifier).getUid();
    }

    /**
     * El uid si el identificador existe; si no, el identificador tal cual.
     * Para endpoints que ante un usuario desconocido devuelven vacío en vez
     * de 404 (el feed de un perfil) y no deben cambiar ese comportamiento.
     */
    public String uidOrSame(String identifier) {
        return find(identifier).map(User::getUid).orElse(identifier);
    }
}
