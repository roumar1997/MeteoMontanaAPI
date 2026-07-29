package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.social.SearchUsersUseCase;
import com.meteomontana.api.application.users.PublicProfileDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserSearchController {

    private final SearchUsersUseCase useCase;

    @GetMapping("/search")
    public List<PublicProfileDto> search(@RequestParam("q") String query,
                                         @RequestParam(value = "limit", defaultValue = "20") int limit) {
        // Cap del lado servidor: el cliente no puede pedir un límite arbitrario.
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return useCase.search(query, safeLimit);
    }
}
