package com.meteomontana.api.application;

import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetSchoolByIdUseCase {

    private final SchoolRepository repository;

    public GetSchoolByIdUseCase(SchoolRepository repository) {
        this.repository = repository;
    }

    public Optional<School> execute(String id) {
        return repository.findById(id);
    }
}
