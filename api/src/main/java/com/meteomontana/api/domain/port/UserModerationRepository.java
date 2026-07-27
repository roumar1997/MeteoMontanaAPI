package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.ModerationAction;
import com.meteomontana.api.domain.model.UserModerationState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Estado de moderación de los usuarios y auditoría de acciones (puerto). */
public interface UserModerationRepository {

    Optional<UserModerationState> findState(String uid);

    /** Suma un aviso y devuelve el estado resultante. */
    UserModerationState addWarning(String uid);

    /** Fija (o retira, con null) la suspensión. */
    UserModerationState setSuspendedUntil(String uid, LocalDateTime until);

    /** Banea o desbanea (al desbanear se retira también la suspensión). */
    UserModerationState setBanned(String uid, boolean banned);

    /** Registro auditable de una acción de moderación. */
    void recordAction(String adminUid, String targetUid, String action,
                      String reason, String snapshot);

    List<ModerationAction> actionsOf(String targetUid);
}
