package com.meteomontana.api.infrastructure.push;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Pool de hilos dedicado al envío de notificaciones push.
 *
 * ¿Por qué un pool propio y no el executor por defecto de @EnableAsync?
 * El default de Spring ({@code SimpleAsyncTaskExecutor}) crea un HILO NUEVO por
 * cada tarea, sin límite → bajo un pico de mensajes/quedadas podría abrir miles
 * de hilos y tumbar la instancia. Este pool está ACOTADO: unos pocos hilos y una
 * cola; si se saturara, {@code CallerRunsPolicy} hace que el hilo que encola
 * ejecute la tarea (frena la entrada en vez de reventar memoria).
 *
 * Se referencia con {@code @Async("pushExecutor")} en {@link FcmService}.
 */
@Configuration
public class PushAsyncConfig {

    @Bean("pushExecutor")
    public Executor pushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("push-");
        // Si la cola se llena, el hilo llamante ejecuta la tarea (contrapresión)
        // en lugar de descartarla o lanzar excepción.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
