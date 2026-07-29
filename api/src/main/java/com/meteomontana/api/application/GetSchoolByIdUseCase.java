package com.meteomontana.api.application;

import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetSchoolByIdUseCase {

    private final SchoolRepository repository;

    public Optional<School> execute(String id) {
        return repository.findById(id);
    }
}
