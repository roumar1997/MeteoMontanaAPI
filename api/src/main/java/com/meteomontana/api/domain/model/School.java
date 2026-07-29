package com.meteomontana.api.domain.model;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class School {
    private final String id;
    private final String name;
    private final String location;
    private final String region;
    private final String style;
    private final String rockType;
    private final double lat;
    private final double lon;
    private final String source;

}
