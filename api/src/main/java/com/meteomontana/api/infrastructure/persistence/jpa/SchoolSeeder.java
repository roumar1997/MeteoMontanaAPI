package com.meteomontana.api.infrastructure.persistence.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meteomontana.api.domain.model.School;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.List;

@Configuration
public class SchoolSeeder {

    private static final Logger log = LoggerFactory.getLogger(SchoolSeeder.class);

    @Bean
    public CommandLineRunner seedSchools(SpringDataSchoolRepository repo,
                                         ObjectMapper objectMapper) {
        return args -> {
            long count = repo.count();
            if (count > 0) {
                log.info("Schools table already has data ({} rows). Skipping seed.", count);
                return;
            }

            log.info("Schools table is empty. Loading from escuelas.json...");

            try (InputStream in = getClass()
                    .getClassLoader()
                    .getResourceAsStream("escuelas.json")) {

                if (in == null) {
                    log.error("escuelas.json not found in resources. Skipping seed.");
                    return;
                }

                List<School> schools = objectMapper.readValue(
                        in, new TypeReference<List<School>>() {}
                );

                List<SchoolJpaEntity> entities = schools.stream()
                        .map(s -> new SchoolJpaEntity(
                                s.getId(),
                                s.getName(),
                                s.getLocation(),
                                s.getRegion(),
                                s.getStyle(),
                                s.getRockType(),
                                s.getLat(),
                                s.getLon(),
                                s.getSource()
                        ))
                        .toList();

                repo.saveAll(entities);
                log.info("Seed complete. Inserted {} schools.", entities.size());

            } catch (Exception ex) {
                log.error("Seed failed: {}", ex.getMessage(), ex);
            }
        };
    }
}
