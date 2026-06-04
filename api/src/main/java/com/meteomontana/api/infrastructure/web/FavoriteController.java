package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.favorites.FavoriteUseCase;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/favorites")
public class FavoriteController {

    private final FavoriteUseCase useCase;

    public FavoriteController(FavoriteUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<FavoriteUseCase.FavoriteSchoolDto> list(@AuthenticationPrincipal FirebaseUser user) {
        return useCase.listMine(user.uid());
    }

    @PostMapping("/{schoolId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void add(@AuthenticationPrincipal FirebaseUser user,
                    @PathVariable String schoolId) {
        useCase.add(user.uid(), schoolId);
    }

    @DeleteMapping("/{schoolId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal FirebaseUser user,
                       @PathVariable String schoolId) {
        useCase.remove(user.uid(), schoolId);
    }

    @GetMapping("/grid")
    public FavoriteUseCase.FavoritesGridDto grid(@AuthenticationPrincipal FirebaseUser user) {
        return useCase.grid7Days(user.uid());
    }
}
