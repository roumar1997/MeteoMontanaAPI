package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.admin.ApproveSubmissionUseCase;
import com.meteomontana.api.application.admin.ListAdminLogsUseCase;
import com.meteomontana.api.application.admin.ListPendingSubmissionsUseCase;
import com.meteomontana.api.application.admin.RejectSubmissionUseCase;
import com.meteomontana.api.application.submissions.SubmissionDto;
import com.meteomontana.api.domain.model.AdminLog;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ListPendingSubmissionsUseCase listPending;
    private final ApproveSubmissionUseCase approveUseCase;
    private final RejectSubmissionUseCase rejectUseCase;
    private final ListAdminLogsUseCase listLogs;

    public AdminController(ListPendingSubmissionsUseCase listPending,
                           ApproveSubmissionUseCase approveUseCase,
                           RejectSubmissionUseCase rejectUseCase,
                           ListAdminLogsUseCase listLogs) {
        this.listPending = listPending;
        this.approveUseCase = approveUseCase;
        this.rejectUseCase = rejectUseCase;
        this.listLogs = listLogs;
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
}
