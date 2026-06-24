package com.meteomontana.api.application.admin;

import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.port.SchoolSubmissionRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataNoteRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminStatsUseCase {

    public record AdminStats(
            long totalUsers,
            long totalAdmins,
            long totalSchools,
            long totalNotes,
            long submissionsPending,
            long submissionsApproved,
            long submissionsRejected
    ) {}

    private final SpringDataUserRepository users;
    private final SpringDataSchoolRepository schools;
    private final SpringDataNoteRepository notes;
    private final SchoolSubmissionRepository submissions;
    private final AdminGuard adminGuard;

    public AdminStatsUseCase(SpringDataUserRepository users,
                             SpringDataSchoolRepository schools,
                             SpringDataNoteRepository notes,
                             SchoolSubmissionRepository submissions,
                             AdminGuard adminGuard) {
        this.users = users;
        this.schools = schools;
        this.notes = notes;
        this.submissions = submissions;
        this.adminGuard = adminGuard;
    }

    public AdminStats compute(String adminUid) {
        adminGuard.ensureAdmin(adminUid);
        long totalUsers = users.count();
        long totalAdmins = users.countAdmins();
        long totalSchools = schools.count();
        long totalNotes = notes.count();
        long pending = submissions.findByStatus(SubmissionStatus.PENDING).size();
        long approved = submissions.findByStatus(SubmissionStatus.APPROVED).size();
        long rejected = submissions.findByStatus(SubmissionStatus.REJECTED).size();
        return new AdminStats(totalUsers, totalAdmins, totalSchools, totalNotes,
                pending, approved, rejected);
    }
}
