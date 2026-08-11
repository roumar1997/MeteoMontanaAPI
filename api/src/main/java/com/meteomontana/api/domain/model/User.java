package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class User {
    private final String uid;
    private final String email;
    private final String username;
    private final String displayName;
    private final String photoPath;
    private final String bio;
    private final boolean isPublic;
    private final String topGrade;
    private final boolean isAdmin;
    private final boolean isPremium;
    private final String fcmToken;
    private final String gender;           // WOMAN | MAN | OTHER | UNSPECIFIED | null — PRIVADO
    // Material propio (perfil): JSON simple, mismo formato que meetup_members.gear_json.
    // {"cuerda":true,"grigri":false,"cintas":12,"crashpads":2}. Se usa para
    // autorrellenar el material al unirte a una quedada (editable ahí igualmente).
    private final String gearJson;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    // AQUI HABIA un constructor "de conveniencia" sin gearJson que lo rellenaba
    // con null. Parecia inofensivo y borraba el material de los usuarios: quien
    // reconstruia el User para cambiar OTRA cosa (el token de notificaciones al
    // arrancar la app, la foto de perfil) se dejaba el material a nulo sin
    // enterarse. Resultado: guardabas tu material, se guardaba bien, y el
    // siguiente arranque de la app lo borraba -- en iOS y en Android.
    // NO volver a anadirlo: que el compilador obligue a decidir que pasa con
    // cada campo.

}
