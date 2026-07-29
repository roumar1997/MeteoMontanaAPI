package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.photos.DeleteSchoolPhotoUseCase;
import com.meteomontana.api.application.photos.GetSchoolPhotosUseCase;
import com.meteomontana.api.application.photos.SchoolPhotoDto;
import com.meteomontana.api.application.photos.UploadSchoolPhotoUseCase;
import com.meteomontana.api.domain.model.SchoolPhoto;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SchoolPhotoController {

    private final UploadSchoolPhotoUseCase uploadUseCase;
    private final GetSchoolPhotosUseCase getUseCase;
    private final DeleteSchoolPhotoUseCase deleteUseCase;

    /** Público: listar fotos de una escuela. */
    @GetMapping("/schools/{id}/photos")
    public List<SchoolPhotoDto> getPhotos(@PathVariable String id) {
        return getUseCase.execute(id);
    }

    /** Autenticado: subir foto (multipart/form-data con campo 'file'). */
    @PostMapping("/schools/{id}/photos")
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolPhoto upload(@PathVariable String id,
                              @RequestParam("file") MultipartFile file,
                              @RequestParam(value = "caption", required = false) String caption,
                              @AuthenticationPrincipal FirebaseUser user) throws IOException {
        return uploadUseCase.execute(id, user.uid(), file, caption);
    }

    /** Autenticado: borrar tu propia foto. */
    @DeleteMapping("/photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String photoId,
                       @AuthenticationPrincipal FirebaseUser user) {
        deleteUseCase.execute(photoId, user.uid());
    }
}
