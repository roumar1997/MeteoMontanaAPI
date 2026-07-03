package com.meteomontana.api.application.meetups;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Invitaciones a quedadas por enlace.
 *
 * El token es un HMAC simple (sha256 de meetupId + secreto de servidor): no
 * caduca mientras exista la quedada, no requiere tabla, y solo puede
 * generarlo/validarlo el backend. Quien tiene el enlace fue invitado por un
 * miembro → puede unirse aunque la quedada sea de "solo seguidos"
 * (FOLLOWERS). La restricción de género (WOMEN) NO se salta nunca.
 */
@Service
public class MeetupInviteService {

    private final String secret;

    public MeetupInviteService(@Value("${invite.secret:cumbre-invita-2026}") String secret) {
        this.secret = secret;
    }

    public String tokenFor(String meetupId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((meetupId + "|" + secret).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 20);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isValid(String meetupId, String token) {
        return token != null && !token.isBlank() && tokenFor(meetupId).equals(token);
    }
}
