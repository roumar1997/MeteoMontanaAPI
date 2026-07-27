package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUid(String uid);
    Optional<User> findByUsername(String username);
    /** Varios usuarios de una vez (una sola query — evita N+1 en rankings/listas). */
    List<User> findByUids(java.util.Collection<String> uids);
    User save(User user);
    boolean usernameTakenByOtherUser(String username, String currentUid);
    List<User> findAllWithFcmToken();
    /** Buscador de usuarios: username O displayName contienen la query
     *  (case-insensitive), acotado en BD para no cargar toda la tabla. */
    List<User> searchByUsernameOrDisplayName(String query);
    /** Nº total de usuarios registrados (panel de admin). */
    long count();
    /** Nº de administradores (panel de admin). */
    long countAdmins();
    /** Todos los administradores (para avisarles de propuestas nuevas). */
    List<User> findAdmins();
    /** Últimos usuarios registrados, más recientes primero (panel de admin). */
    List<User> findRecent(int limit);
}
