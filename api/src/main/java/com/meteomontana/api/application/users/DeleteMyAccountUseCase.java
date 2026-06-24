package com.meteomontana.api.application.users;

import com.google.firebase.auth.FirebaseAuth;
import com.meteomontana.api.domain.port.ChatRepository;
import com.meteomontana.api.infrastructure.storage.StorageService;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFavoriteRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFollowRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataJournalRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataNoteRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataNotificationRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolSubmissionRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.UserJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataWeekendAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Borrado de cuenta (requisito de Google Play y App Store cuando hay login):
 * elimina los datos personales del usuario y su cuenta de Firebase Auth.
 *
 * Borra: perfil, favoritas, seguimientos (en ambos sentidos), diario,
 * notificaciones, alerta de tiempo, propuestas de escuela, notas. Además borra
 * las conversaciones de chat del usuario (Firestore) y su foto de perfil
 * (Storage). Las contribuciones aprobadas ya se materializaron como contenido
 * de la escuela (no son datos personales) y se conservan, igual que las fotos
 * de vías/notas ya publicadas como contenido comunitario.
 */
@Service
public class DeleteMyAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteMyAccountUseCase.class);

    private final SpringDataUserRepository users;
    private final SpringDataFavoriteRepository favorites;
    private final SpringDataFollowRepository follows;
    private final SpringDataJournalRepository journal;
    private final SpringDataNotificationRepository notifications;
    private final SpringDataSchoolSubmissionRepository submissions;
    private final SpringDataNoteRepository notes;
    private final SpringDataWeekendAlertRepository weekendAlerts;
    private final ChatRepository chat;
    private final StorageService storage;

    public DeleteMyAccountUseCase(SpringDataUserRepository users,
                                  SpringDataFavoriteRepository favorites,
                                  SpringDataFollowRepository follows,
                                  SpringDataJournalRepository journal,
                                  SpringDataNotificationRepository notifications,
                                  SpringDataSchoolSubmissionRepository submissions,
                                  SpringDataNoteRepository notes,
                                  SpringDataWeekendAlertRepository weekendAlerts,
                                  ChatRepository chat,
                                  StorageService storage) {
        this.users = users;
        this.favorites = favorites;
        this.follows = follows;
        this.journal = journal;
        this.notifications = notifications;
        this.submissions = submissions;
        this.notes = notes;
        this.weekendAlerts = weekendAlerts;
        this.chat = chat;
        this.storage = storage;
    }

    @Transactional
    public void execute(String uid) {
        if (uid == null || uid.isBlank()) return;

        // Ruta de la foto de perfil (Storage) ANTES de borrar la fila del usuario.
        String photoPath = users.findById(uid).map(UserJpaEntity::getPhotoPath).orElse(null);

        favorites.deleteByUid(uid);
        follows.deleteAllForUid(uid);
        journal.deleteByUid(uid);
        notifications.deleteByUid(uid);
        notes.deleteByUid(uid);
        submissions.deleteBySubmittedByUid(uid);
        if (weekendAlerts.existsById(uid)) weekendAlerts.deleteById(uid);
        users.deleteById(uid);

        // Chat (Firestore): conversaciones 1-a-1 + salir de grupos. Best-effort.
        chat.deleteUserData(uid);

        // Foto de perfil (Storage). Best-effort. Las fotos de vías/notas ya
        // publicadas son contenido comunitario y no se borran aquí.
        if (photoPath != null && !photoPath.isBlank()) {
            try { storage.delete(photoPath); }
            catch (Exception e) { log.warn("delete account: foto {} no borrada: {}", photoPath, e.getMessage()); }
        }

        // Cuenta de Firebase Auth: best-effort. Si falla, los datos ya se
        // borraron; al volver a entrar con Google se crearía un perfil nuevo.
        try {
            FirebaseAuth.getInstance().deleteUser(uid);
        } catch (Exception e) {
            log.warn("delete account: no se pudo borrar el usuario Firebase {}: {}", uid, e.getMessage());
        }
        log.info("Cuenta borrada: {}", uid);
    }
}
