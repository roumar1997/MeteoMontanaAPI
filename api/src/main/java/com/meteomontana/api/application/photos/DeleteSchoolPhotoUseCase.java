package com.meteomontana.api.application.photos;

import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.PhotoNotFoundException;
import com.meteomontana.api.domain.model.SchoolPhoto;
import com.meteomontana.api.domain.port.SchoolPhotoRepository;
import com.meteomontana.api.infrastructure.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteSchoolPhotoUseCase {

    private final SchoolPhotoRepository photoRepository;
    private final StorageService storageService;

    @Transactional
    public void execute(String photoId, String requesterUid) {
        SchoolPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new PhotoNotFoundException(photoId));

        // Solo el que subió la foto puede borrarla (los admins se añadirán en Fase 9).
        if (!photo.getUploadedByUid().equals(requesterUid)) {
            throw new ForbiddenException("Only the uploader can delete this photo");
        }

        photoRepository.deleteById(photoId);
        // El archivo en Storage también se borra (si esto falla, queda huérfano pero
        // la BD ya no lo referencia).
        try { storageService.delete(photo.getStoragePath()); } catch (Exception ignored) {}
    }
}
