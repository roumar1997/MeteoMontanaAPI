CREATE TABLE meetup_reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meetup_id       UUID        NOT NULL REFERENCES meetups(id) ON DELETE CASCADE,
    reporter_uid    VARCHAR(128) NOT NULL,
    reported_uid    VARCHAR(128),          -- NULL = reportar la quedada completa
    reason          VARCHAR(32) NOT NULL,  -- SPAM | INAPPROPRIATE | HARASSMENT | OTHER
    context         TEXT,
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    resolved_by     VARCHAR(128),
    resolved_at     TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_meetup_reports_meetup   ON meetup_reports(meetup_id);
CREATE INDEX idx_meetup_reports_reporter ON meetup_reports(reporter_uid);
CREATE INDEX idx_meetup_reports_status   ON meetup_reports(status);
