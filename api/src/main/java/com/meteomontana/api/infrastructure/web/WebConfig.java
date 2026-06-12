package com.meteomontana.api.infrastructure.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Config web transversal.
 *
 * ETag "shallow": el filtro calcula un hash MD5 del body de cada respuesta GET
 * y lo manda como header ETag. Si el cliente repite la petición con
 * If-None-Match y el body no ha cambiado, respondemos 304 Not Modified sin
 * body — el catálogo de 191 escuelas (~100 KB) deja de viajar en cada
 * refresh de la PWA/app cuando no hay cambios. No ahorra CPU (el body se
 * genera igual para hashearlo), solo ancho de banda.
 */
@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        // Solo lecturas de catálogo y derivados; el filtro ya ignora los no-GET.
        registration.addUrlPatterns("/api/schools", "/api/schools/*");
        return registration;
    }
}
