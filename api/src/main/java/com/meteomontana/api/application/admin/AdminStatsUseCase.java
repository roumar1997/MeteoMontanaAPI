package com.meteomontana.api.application.admin;

import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.port.SchoolSubmissionRepository;
import com.meteomontana.api.domain.port.NoteRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
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

    private final UserRepository users;
    private final SchoolRepository schools;
    private final NoteRepository notes;
    private final SchoolSubmissionRepository submissions;
    private final AdminGuard adminGuard;

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
