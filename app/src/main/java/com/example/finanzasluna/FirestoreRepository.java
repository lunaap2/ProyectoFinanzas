package com.example.finanzasluna;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Encapsula todo el acceso a la coleccion "transacciones" en Firebase Firestore.
 */
public class FirestoreRepository {

    public static final String COLECCION = "transacciones";

    private final CollectionReference coleccion;

    public FirestoreRepository() {
        coleccion = FirebaseFirestore.getInstance().collection(COLECCION);
    }

    public interface OnGuardadoListener {
        void onExito();
        void onError(Exception e);
    }

    public interface OnDocumentoListener {
        void onDocumento(Transaccion t);
    }

    // CREATE: agrega un nuevo documento (Firestore genera el Document ID).
    public void crear(Transaccion t, OnGuardadoListener listener) {
        coleccion.add(t)
                .addOnSuccessListener(ref -> listener.onExito())
                .addOnFailureListener(listener::onError);
    }

    // UPDATE: sobrescribe el documento apuntando a su Document ID.
    public void actualizar(@NonNull String id, Transaccion t, OnGuardadoListener listener) {
        coleccion.document(id).set(t)
                .addOnSuccessListener(unused -> listener.onExito())
                .addOnFailureListener(listener::onError);
    }

    // DELETE: elimina el documento apuntando a su Document ID.
    public void eliminar(@NonNull String id, OnGuardadoListener listener) {
        coleccion.document(id).delete()
                .addOnSuccessListener(unused -> listener.onExito())
                .addOnFailureListener(listener::onError);
    }

    // READ puntual: se usa para precargar el formulario en modo edicion.
    public void obtenerPorId(@NonNull String id, OnDocumentoListener listener) {
        coleccion.document(id).get()
                .addOnSuccessListener(doc -> {
                    Transaccion t = doc.toObject(Transaccion.class);
                    if (t != null) {
                        t.setId(doc.getId());
                    }
                    listener.onDocumento(t);
                })
                .addOnFailureListener(e -> listener.onDocumento(null));
    }

    // READ en tiempo real: SnapshotListener que alimenta el RecyclerView.
    public ListenerRegistration escucharCambios(EventListener<QuerySnapshot> listener) {
        return coleccion.orderBy("creadoEn", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }
}
