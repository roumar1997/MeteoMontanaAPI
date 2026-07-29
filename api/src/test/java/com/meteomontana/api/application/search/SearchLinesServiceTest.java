package com.meteomontana.api.application.search;

import com.meteomontana.api.domain.model.LineSearchHit;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.LineSearchRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SearchLinesServiceTest {

    LineSearchRepository search = mock(LineSearchRepository.class);
    SchoolRepository schools = mock(SchoolRepository.class);
    SearchLinesService service = new SearchLinesService(search, schools);

    private LineSearchHit line(String name, String schoolId) {
        return new LineSearchHit(schoolId, null, "b1", "Piedra", "l1", name, "6a",
                null, "foto.jpg", "[{\"x\":0.1}]", "SIT");
    }

    private LineSearchHit block(String name) {
        return new LineSearchHit("s1", null, "b2", name, null, null, null,
                null, "cover.jpg", null, null);
    }

    @Test
    void queryCortaNoBusca() {
        assertThat(service.search("a")).isEmpty();
        assertThat(service.search("  ")).isEmpty();
        assertThat(service.search(null)).isEmpty();
        verifyNoInteractions(search);
    }

    @Test
    void viasPrimeroYBloquesDespuesConNombreDeEscuela() {
        when(search.searchLinesByName(anyString(), anyInt())).thenReturn(List.of(line("La ola", "s1")));
        when(search.searchBlocksByName(anyString(), anyInt())).thenReturn(List.of(block("El barco")));
        when(schools.findById("s1")).thenReturn(Optional.of(
                new School("s1", "Zarzalejo", null, null, null, null, 0, 0, null)));

        var out = service.search("ola");

        assertThat(out).hasSize(2);
        assertThat(out.get(0).lineName()).isEqualTo("La ola");     // vías primero
        assertThat(out.get(0).schoolName()).isEqualTo("Zarzalejo");
        assertThat(out.get(0).photoPath()).isEqualTo("foto.jpg");  // mini-topo
        assertThat(out.get(0).linePath()).contains("0.1");
        assertThat(out.get(1).blockName()).isEqualTo("El barco");
    }

    @Test
    void piedrasAutonumeradasFuera() {
        when(search.searchLinesByName(anyString(), anyInt())).thenReturn(List.of());
        when(search.searchBlocksByName(anyString(), anyInt()))
                .thenReturn(List.of(block("12"), block("El barco")));
        when(schools.findById("s1")).thenReturn(Optional.empty());

        var out = service.search("barco");

        assertThat(out).hasSize(1);
        assertThat(out.get(0).blockName()).isEqualTo("El barco");
    }
}
