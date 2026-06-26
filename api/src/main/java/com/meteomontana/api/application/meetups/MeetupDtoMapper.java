package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.model.Meetup;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MeetupDtoMapper {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    public MeetupDtoMapper(SchoolRepository schoolRepository, UserRepository userRepository) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
    }

    public MeetupDto toDto(Meetup m, String requesterUid) {
        String schoolName = schoolRepository.findById(m.getSchoolId())
                .map(s -> s.getName()).orElse(m.getSchoolId());

        var creator = userRepository.findByUid(m.getCreatorUid());
        String creatorUsername  = creator.map(u -> u.getUsername()).orElse(null);
        String creatorPhotoUrl  = creator.map(u -> u.getPhotoPath()).orElse(null);

        List<MeetupDto.MemberDto> memberDtos = m.getMembers() == null ? List.of() :
                m.getMembers().stream()
                        .map(mm -> new MeetupDto.MemberDto(
                                mm.uid(), mm.username(), mm.displayName(), mm.photoUrl()))
                        .toList();

        boolean joined = requesterUid != null && m.getMembers() != null &&
                m.getMembers().stream().anyMatch(mm -> mm.uid().equals(requesterUid));

        return new MeetupDto(
                m.getId(), m.getSchoolId(), schoolName, m.getName(), m.getDiscipline(),
                m.getPrivacy(), m.getMemberLimit(),
                m.getMembers() == null ? 0 : m.getMembers().size(),
                m.getPhotoUrl(), m.getCreatorUid(), creatorUsername, creatorPhotoUrl,
                m.getConversationId(), m.getDays(), m.getLastDay(),
                m.getExpiresAt(), m.getCreatedAt(), memberDtos, joined
        );
    }
}
