package com.example.finanzasluna;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centraliza el canal de notificaciones (Android 8+) y la emision de notificaciones
 * locales con un PendingIntent que reabre la app en MainActivity.
 */
public class NotificationHelper {

    public static final String CANAL_ID = "finanzas_canal_principal";
    private static final String CANAL_NOMBRE = "Finanzas - Actividad de transacciones";

    private static final AtomicInteger contadorNotificaciones = new AtomicInteger(1000);

    private NotificationHelper() {
    }

    // Registro del NotificationChannel: requerido en Android 8.0 (API 26) en adelante. Es idempotente.
    public static void crearCanal(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(CANAL_ID) == null) {
                NotificationChannel canal = new NotificationChannel(
                        CANAL_ID, CANAL_NOMBRE, NotificationManager.IMPORTANCE_DEFAULT);
                canal.setDescription("Avisa cuando se crea o modifica una transaccion");
                manager.createNotificationChannel(canal);
            }
        }
    }

    public static void mostrarNotificacion(Context context, String titulo, String mensaje) {
        // Android 13+ exige el permiso POST_NOTIFICATIONS en tiempo de ejecucion.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(contadorNotificaciones.incrementAndGet(), builder.build());
    }
}
