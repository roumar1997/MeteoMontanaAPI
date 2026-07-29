package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.port.AccountDataPurger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Implementación del borrado de cuenta: sabe qué tablas hay y en qué orden. */
@Repository
public class JpaAccountDataPurger implements AccountDataPurger {

    private final SpringDataUserRepository users;
    private final SpringDataFavoriteRepository favorites;
    private final SpringDataFollowRepository follows;
    private final SpringDataJournalRepository journal;
    private final SpringDataNotificationRepository notifications;
    private final SpringDataSchoolSubmissionRepository submissions;
    private final SpringDataNoteRepository notes;
    private final SpringDataWeekendAlertRepository weekendAlerts;

    public JpaAccountDataPurger(SpringDataUserRepository users,
                                SpringDataFavoriteRepository favorites,
                                SpringDataFollowRepository follows,
                                SpringDataJournalRepository journal,
                                SpringDataNotificationRepository notifications,
                                SpringDataSchoolSubmissionRepository submissions,
                                SpringDataNoteRepository notes,
                                SpringDataWeekendAlertRepository weekendAlerts) {
        this.users = users;
        this.favorites = favorites;
        this.follows = follows;
        this.journal = journal;
        this.notifications = notifications;
        this.submissions = submissions;
        this.notes = notes;
        this.weekendAlerts = weekendAlerts;
    }

    @Override
    public String photoPathOf(String uid) {
        return users.findById(uid).map(UserJpaEntity::getPhotoPath).orElse(null);
    }

    @Override
    @Transactional
    public void purgeAllDataOf(String uid) {
        favorites.deleteByUid(uid);
        follows.deleteAllForUid(uid);
        journal.deleteByUid(uid);
        notifications.deleteByUid(uid);
        notes.deleteByUid(uid);
        submissions.deleteBySubmittedByUid(uid);
        if (weekendAlerts.existsById(uid)) weekendAlerts.deleteById(uid);
        users.deleteById(uid);
    }
}
