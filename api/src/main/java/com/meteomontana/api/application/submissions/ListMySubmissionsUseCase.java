package com.meteomontana.api.application.submissions;

import com.meteomontana.api.domain.port.SchoolSubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListMySubmissionsUseCase {

    private final SchoolSubmissionRepository repository;

    public List<SubmissionDto> execute(String uid) {
        return repository.findBySubmittedByUid(uid).stream()
                .map(SubmissionDto::from)
                .toList();
    }
}
