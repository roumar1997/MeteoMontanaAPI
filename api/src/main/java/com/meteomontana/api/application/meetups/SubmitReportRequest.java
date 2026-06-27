package com.meteomontana.api.application.meetups;

public class SubmitReportRequest {
    private String reportedUid;   // null si se denuncia la quedada entera
    private String reason;        // SPAM | INAPPROPRIATE | HARASSMENT | OTHER
    private String context;

    public String getReportedUid() { return reportedUid; }
    public String getReason()      { return reason; }
    public String getContext()     { return context; }
}
