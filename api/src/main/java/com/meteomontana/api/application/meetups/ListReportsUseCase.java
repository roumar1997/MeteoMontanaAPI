package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.port.MeetupReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListReportsUseCase {

    private final MeetupReportRepository reportRepository;

    public List<ReportDto> execute() {
        return reportRepository.findPending()
                .stream().map(ReportDto::from).toList();
    }
}
