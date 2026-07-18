package com.meteomontana.api.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Versión mínima OBLIGATORIA de las apps. Público: las apps lo consultan al
 * arrancar y, si van por debajo, muestran un gate no descartable con el botón
 * a su tienda. Los mínimos se controlan por variables de entorno de Railway
 * (MIN_ANDROID_VC / MIN_IOS_BUILD) — cambiar el número fuerza la
 * actualización AL INSTANTE, sin tocar código ni re-desplegar apps.
 * Por defecto 0 = no se obliga a nadie.
 */
@RestController
public class AppVersionController {

    public record AppVersion(int minAndroidVc, int minIosBuild,
                             String androidUrl, String iosUrl) {}

    private final int minAndroidVc;
    private final int minIosBuild;

    public AppVersionController(
            @Value("${MIN_ANDROID_VC:0}") int minAndroidVc,
            @Value("${MIN_IOS_BUILD:0}") int minIosBuild) {
        this.minAndroidVc = minAndroidVc;
        this.minIosBuild = minIosBuild;
    }

    @GetMapping("/api/app-version")
    public AppVersion get() {
        // /app es el enlace fijo de descarga (redirige por user-agent a la
        // tienda correcta) — la ruta NO puede cambiar, ver bitácora 2.14.0.
        return new AppVersion(
                minAndroidVc, minIosBuild,
                "https://play.google.com/store/apps/details?id=com.meteomontana.android",
                "https://api.climbingteams.com/app");
    }
}
