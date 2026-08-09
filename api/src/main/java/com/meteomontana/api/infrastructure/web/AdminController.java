package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.admin.AdminGuard;
import com.meteomontana.api.application.admin.AdminStatsUseCase;
import com.meteomontana.api.application.admin.ApproveSubmissionUseCase;
import com.meteomontana.api.application.admin.ListAdminLogsUseCase;
import com.meteomontana.api.application.admin.ListPendingSubmissionsUseCase;
import com.meteomontana.api.application.admin.RejectSubmissionUseCase;
import com.meteomontana.api.application.admin.SendAdminPushUseCase;
import com.meteomontana.api.application.meetups.ListReportsUseCase;
import com.meteomontana.api.application.meetups.ReportDto;
import com.meteomontana.api.application.meetups.ResolveReportUseCase;
import com.meteomontana.api.application.submissions.SubmissionDto;
import com.meteomontana.api.domain.model.AdminLog;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ListPendingSubmissionsUseCase listPending;
    private final ApproveSubmissionUseCase approveUseCase;
    private final RejectSubmissionUseCase rejectUseCase;
    private final ListAdminLogsUseCase listLogs;
    private final SendAdminPushUseCase sendPushUseCase;
    private final AdminStatsUseCase statsUseCase;
    private final ListReportsUseCase listReports;
    private final ResolveReportUseCase resolveReport;
    private final SchoolRepository schoolRepository;
    private final AdminGuard adminGuard;
    private final com.meteomontana.api.infrastructure.storage.StorageMigrationService storageMigration;

    /** Mover una escuela directamente (admin). Body: {"lat": ..., "lon": ...}. */
    public record MoveSchoolRequest(double lat, double lon) {}

    @PutMapping("/schools/{id}/position")
    public School moveSchool(
            @AuthenticationPrincipal FirebaseUser user,
            @PathVariable String id,
            @RequestBody MoveSchoolRequest req) {
        // Este endpoint no pasa por un use case admin — el guard va aquí.
        // Sin esto, cualquier usuario autenticado podía mover escuelas.
        adminGuard.ensureAdmin(user.uid());
        School current = schoolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("School not found: " + id));
        School moved = new School(
                current.getId(), current.getName(), current.getLocation(), current.getRegion(),
                current.getStyle(), current.getRockType(),
                req.lat(), req.lon(),
                current.getSource(),
                current.getCountry()
        );
        return schoolRepository.save(moved);
    }

    @GetMapping("/stats")
    public AdminStatsUseCase.AdminStats stats(@AuthenticationPrincipal FirebaseUser user) {
        return statsUseCase.compute(user.uid());
    }

    /**
     * Copia las fotos de Firebase → R2 (migración manual, admin). ?dryRun=true
     * solo cuenta cuántas faltan en R2 sin copiar. Idempotente y no destructivo
     * (nunca borra de Firebase). Devuelve total/copiadas/ya-estaban/fallos.
     */
    @PostMapping("/storage/migrate")
    public com.meteomontana.api.infrastructure.storage.StorageMigrationService.Result migrateStorage(
            @AuthenticationPrincipal FirebaseUser user,
            @RequestParam(defaultValue = "true") boolean dryRun) {
        adminGuard.ensureAdmin(user.uid());
        return storageMigration.migrate(dryRun);
    }

    @PostMapping("/push")
    public SendAdminPushUseCase.AdminPushResponse sendPush(
            @AuthenticationPrincipal FirebaseUser user,
            @RequestBody SendAdminPushUseCase.AdminPushRequest req) {
        return sendPushUseCase.execute(user.uid(), req);
    }

    @GetMapping("/submissions")
    public List<SubmissionDto> pending(@AuthenticationPrincipal FirebaseUser user) {
        return listPending.execute(user.uid());
    }

    public record ApproveBody(String schoolId) {}

    @PostMapping("/submissions/{id}/approve")
    public SubmissionDto approve(@AuthenticationPrincipal FirebaseUser user,
                                 @PathVariable String id,
                                 @RequestBody(required = false) ApproveBody body) {
        String override = body != null ? body.schoolId() : null;
        return approveUseCase.execute(user.uid(), id, override);
    }

    public record RejectBody(String reason) {}

    @PostMapping("/submissions/{id}/reject")
    public SubmissionDto reject(@AuthenticationPrincipal FirebaseUser user,
                                @PathVariable String id,
                                @RequestBody(required = false) RejectBody body) {
        String reason = body != null ? body.reason() : null;
        return rejectUseCase.execute(user.uid(), id, reason);
    }

    @GetMapping("/logs")
    public List<AdminLog> logs(@AuthenticationPrincipal FirebaseUser user,
                               @RequestParam(defaultValue = "100") int limit) {
        return listLogs.execute(user.uid(), limit);
    }

    @GetMapping("/reports")
    public List<ReportDto> reports(@AuthenticationPrincipal FirebaseUser user) {
        adminGuard.ensureAdmin(user.uid());
        return listReports.execute();
    }

    @PostMapping("/reports/{id}/resolve")
    public ReportDto resolveReport(@AuthenticationPrincipal FirebaseUser user,
                                   @PathVariable String id,
                                   @RequestBody(required = false) Map<String, String> body) {
        adminGuard.ensureAdmin(user.uid());
        String action = body != null ? body.getOrDefault("action", "resolve") : "resolve";
        return resolveReport.execute(user.uid(), id, action);
    }
}
