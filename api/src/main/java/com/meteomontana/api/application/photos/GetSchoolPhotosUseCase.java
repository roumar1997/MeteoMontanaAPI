package com.meteomontana.api.application.photos;

import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.SchoolPhoto;
import com.meteomontana.api.domain.port.SchoolPhotoRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.infrastructure.storage.StorageService;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetSchoolPhotosUseCase {

    // URL firmada válida durante 60 minutos.
    private static final int URL_TTL_MINUTES = 60;

    private final SchoolRepository schoolRepository;
    private final SchoolPhotoRepository photoRepository;
    private final StorageService storageService;

    public List<SchoolPhotoDto> execute(String schoolId) {
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));

        return photoRepository.findBySchoolId(schoolId).stream()
                .map(this::toDto)
                .toList();
    }

    private SchoolPhotoDto toDto(SchoolPhoto p) {
        String url = storageService.signedReadUrl(p.getStoragePath(), URL_TTL_MINUTES).toString();
        return new SchoolPhotoDto(
                p.getId(), p.getSchoolId(), url, p.getUploadedByUid(),
                p.getCaption(), p.getWidth(), p.getHeight(),
                p.getSizeBytes(), p.getContentType(), p.getCreatedAt()
        );
    }
}
