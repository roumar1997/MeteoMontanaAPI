package com.meteomontana.api.application.blocks;

import com.meteomontana.api.domain.model.BlockLine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Decide el id de cada vía al guardar una piedra desde el editor.
 *
 * <p>El editor manda la lista completa de vías sin decir cuál era cuál, así que
 * antes se creaban todas de cero con id nuevo. Y del id de la vía cuelga medio
 * mundo: el ✓ del diario de cada usuario, sus estrellas, los votos de grado,
 * los comentarios, los posts del feed y los enlaces compartidos. Editar una
 * piedra —aunque solo fuese para corregir una tilde— borraba todo eso.
 *
 * <p>Aquí se empareja cada vía entrante con la que ya existía y se le devuelve
 * SU id. Dos pasadas, de más fiable a menos:
 * <ol>
 *   <li>por cara y nombre: si el nombre no cambió, es la misma vía sin duda;</li>
 *   <li>por posición dentro de la cara, para las que quedaron sueltas: cubre
 *       justo el caso de renombrar una vía sin mover nada de sitio.</li>
 * </ol>
 * Lo que no empareja con nada es una vía nueva y estrena id. Lo que existía y
 * nadie reclama es una vía borrada: desaparece al guardar.
 *
 * <p>Clase pura y sin dependencias a propósito: es la parte con criterio, y así
 * se prueba entera sin base de datos.
 */
public final class BlockLineIdReconciler {

    private BlockLineIdReconciler() {}

    /** Una vía tal y como llega del editor: solo lo que sirve para identificarla. */
    public record Incoming(String id, String name, Integer faceOrder) {

        /** Compatibilidad: una via sin id declarado (apps viejas y tests). */
        public Incoming(String name, Integer faceOrder) { this(null, name, faceOrder); }

        int face() { return faceOrder == null ? 0 : faceOrder; }
        Clave clave() { return new Clave(face(), normaliza(name)); }
        /** El id es la unica pista que no puede equivocarse. */
        boolean tieneId() { return id != null && !id.isBlank(); }
    }

    /**
     * Cara + nombre normalizado. Es un registro y no una cadena concatenada a
     * propósito: con un separador, una vía llamada "2|algo" podría chocar con
     * otra de la cara 12, y el emparejamiento fallaría en silencio.
     */
    private record Clave(int face, String nombre) {}

    private static String normaliza(String nombre) {
        return nombre == null ? "" : nombre.trim().toLowerCase();
    }

    /**
     * @return un id por cada vía entrante, en el mismo orden: el de la vía
     *         existente con la que casa, o uno nuevo si no casa con ninguna.
     */
    public static List<String> assignIds(List<BlockLine> existing, List<Incoming> incoming) {
        List<String> result = new ArrayList<>(java.util.Collections.nCopies(incoming.size(), null));
        if (existing == null) existing = List.of();

        // Las que aún nadie ha reclamado, agrupadas por cara y en su orden.
        Map<Integer, List<BlockLine>> libresPorCara = new LinkedHashMap<>();
        for (BlockLine l : existing) {
            libresPorCara.computeIfAbsent(l.getFaceOrder(), k -> new ArrayList<>()).add(l);
        }

        // Si el editor manda ids, es una app moderna: entonces una fila SIN id
        // es una via NUEVA de verdad, y no puede heredar el hueco que dejo otra.
        // Adivinar ahi seria peor que no hacer nada, porque le colgaria el ✓ del
        // diario de alguien a una via que acaba de nacer.
        boolean editorConIds = incoming.stream().anyMatch(Incoming::tieneId);

        // 0a pasada: el id que manda el editor desde 2.21.3. Es la unica pista
        // que no puede equivocarse, asi que va primero y retira la via de las
        // libres para que ninguna otra fila la reclame.
        for (int i = 0; i < incoming.size(); i++) {
            Incoming in = incoming.get(i);
            if (!in.tieneId()) continue;
            for (BlockLine l : existing) {
                if (l.getId().equals(in.id())) {
                    result.set(i, l.getId());
                    libresPorCara.getOrDefault(l.getFaceOrder(), List.of()).remove(l);
                    break;
                }
            }
        }

        // 1ª pasada: cara + nombre. Un nombre repetido dentro de la misma cara
        // se asigna por orden de aparición, que es lo único razonable.
        Map<Clave, List<BlockLine>> porNombre = new LinkedHashMap<>();
        java.util.Set<String> yaAsignados = new java.util.HashSet<>(result);
        for (BlockLine l : existing) {
            if (yaAsignados.contains(l.getId())) continue;   // reclamada por su id
            porNombre.computeIfAbsent(new Clave(l.getFaceOrder(), normaliza(l.getName())),
                    x -> new ArrayList<>()).add(l);
        }
        for (int i = 0; i < incoming.size() && !editorConIds; i++) {
            if (result.get(i) != null) continue;                      // ya resuelta por id
            Incoming in = incoming.get(i);
            if (in.name() == null || in.name().isBlank()) continue;   // sin nombre no hay pista
            List<BlockLine> candidatas = porNombre.get(in.clave());
            if (candidatas != null && !candidatas.isEmpty()) {
                BlockLine elegida = candidatas.remove(0);
                result.set(i, elegida.getId());
                libresPorCara.getOrDefault(elegida.getFaceOrder(), List.of()).remove(elegida);
            }
        }

        // 2ª pasada: posición dentro de la cara, para las que siguen sin id
        // (típico de haber renombrado una vía sin cambiarla de sitio).
        for (int i = 0; i < incoming.size() && !editorConIds; i++) {
            if (result.get(i) != null) continue;
            List<BlockLine> libres = libresPorCara.get(incoming.get(i).face());
            if (libres != null && !libres.isEmpty()) {
                result.set(i, libres.remove(0).getId());
            }
        }

        // Lo que quede sin emparejar es una vía nueva.
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i) == null) result.set(i, UUID.randomUUID().toString());
        }
        return result;
    }
}
