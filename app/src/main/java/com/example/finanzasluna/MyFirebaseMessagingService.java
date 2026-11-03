package com.example.finanzasluna;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Infraestructura de FCM: recibe mensajes push enviados desde la consola de Firebase
 * y guarda el token de cada dispositivo en el perfil del usuario para poder targetearlo.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo token FCM: " + token);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance()
                    .collection(AuthRepository.COLECCION_USUARIOS)
                    .document(uid)
                    .update("fcmToken", token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage mensaje) {
        super.onMessageReceived(mensaje);

        NotificationHelper.crearCanal(this);

        String titulo = "Finanzas";
        String cuerpo = "Tienes una notificacion nueva";

        if (mensaje.getNotification() != null) {
            if (mensaje.getNotification().getTitle() != null) {
                titulo = mensaje.getNotification().getTitle();
            }
            if (mensaje.getNotification().getBody() != null) {
                cuerpo = mensaje.getNotification().getBody();
            }
        } else if (!mensaje.getData().isEmpty()) {
            cuerpo = mensaje.getData().toString();
        }

        NotificationHelper.mostrarNotificacion(this, titulo, cuerpo);
    }
}
