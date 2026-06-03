package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.JournalSession;

import java.util.List;
import java.util.Optional;

public interface JournalRepository {
    JournalSession save(JournalSession session);
    Optional<JournalSession> findById(String id);
    List<JournalSession> findByUid(String uid);
    void deleteById(String id);
}
