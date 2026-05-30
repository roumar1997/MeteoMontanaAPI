CREATE TABLE schools (
                         id          VARCHAR(80)      PRIMARY KEY,
                         name        VARCHAR(120)     NOT NULL,
                         location    VARCHAR(200),
                         region      VARCHAR(80),
                         style       VARCHAR(40),
                         rock_type   VARCHAR(60),
                         lat         DOUBLE PRECISION NOT NULL,
                         lon         DOUBLE PRECISION NOT NULL,
                         source      VARCHAR(500)
);

CREATE INDEX idx_schools_region    ON schools (region);
CREATE INDEX idx_schools_rock_type ON schools (rock_type);
CREATE INDEX idx_schools_style     ON schools (style);