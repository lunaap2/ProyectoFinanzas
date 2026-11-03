package com.example.finanzasluna;

import androidx.annotation.NonNull;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

/**
 * Sube las fotos de evidencia a Firebase Storage y resuelve el enlace de descarga.
 */
public class StorageRepository {

    private static final String CARPETA = "evidencias";

    public interface OnSubidaListener {
        void onProgreso(int porcentaje);
        void onExito(String downloadUrl);
        void onError(Exception e);
    }

    private final StorageReference raiz;

    public StorageRepository() {
        raiz = FirebaseStorage.getInstance().getReference();
    }

    public void subirImagen(@NonNull byte[] datosJpg, @NonNull String uidPropietario, OnSubidaListener listener) {
        String nombreArchivo = UUID.randomUUID().toString() + ".jpg";
        StorageReference archivoRef = raiz.child(CARPETA).child(uidPropietario).child(nombreArchivo);

        UploadTask tarea = archivoRef.putBytes(datosJpg);
        tarea.addOnProgressListener(snapshot -> {
            int porcentaje = (int) (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
            listener.onProgreso(porcentaje);
        });
        tarea.addOnFailureListener(listener::onError);
        tarea.addOnSuccessListener(taskSnapshot -> archivoRef.getDownloadUrl()
                .addOnSuccessListener(uri -> listener.onExito(uri.toString()))
                .addOnFailureListener(listener::onError));
    }
}
