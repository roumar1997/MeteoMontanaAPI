package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.social.SearchUsersUseCase;
import com.meteomontana.api.application.users.PublicProfileDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserSearchController {

    private final SearchUsersUseCase useCase;

    public UserSearchController(SearchUsersUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/search")
    public List<PublicProfileDto> search(@RequestParam("q") String query,
                                         @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return useCase.search(query, limit);
    }
}
