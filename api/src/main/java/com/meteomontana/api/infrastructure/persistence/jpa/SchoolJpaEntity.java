package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "schools")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SchoolJpaEntity {

    @Id
    private  String id;
    @Column(nullable = false)
    @Setter
    private String name;
    private String location;
    private String region;
    private String style;

    @Column( name = "rock_type")
    private String rockType;

    @Column( nullable = false)
    @Setter
    private double lat;

    @Column(nullable = false)
    @Setter
    private double lon;

    private String source;

    /** País ISO 3166-1 alfa-2. Las escuelas anteriores al catálogo son 'ES'. */
    @Column(nullable = false)
    private String country = "ES";

}
