package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.submissions.ListMySubmissionsUseCase;
import com.meteomontana.api.application.submissions.SubmissionDto;
import com.meteomontana.api.application.submissions.SubmitSchoolRequest;
import com.meteomontana.api.application.submissions.SubmitSchoolUseCase;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SubmissionController {

    private final SubmitSchoolUseCase submitUseCase;
    private final ListMySubmissionsUseCase listMyUseCase;

    public SubmissionController(SubmitSchoolUseCase submitUseCase,
                                ListMySubmissionsUseCase listMyUseCase) {
        this.submitUseCase = submitUseCase;
        this.listMyUseCase = listMyUseCase;
    }

    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionDto submit(@AuthenticationPrincipal FirebaseUser user,
                                @RequestBody SubmitSchoolRequest request) {
        return submitUseCase.execute(user.uid(), request);
    }

    @GetMapping("/submissions/me")
    public List<SubmissionDto> listMine(@AuthenticationPrincipal FirebaseUser user) {
        return listMyUseCase.execute(user.uid());
    }
}
