package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.port.MeetupReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListReportsUseCase {

    private final MeetupReportRepository reportRepository;

    public ListReportsUseCase(MeetupReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public List<ReportDto> execute() {
        return reportRepository.findPending()
                .stream().map(ReportDto::from).toList();
    }
}
