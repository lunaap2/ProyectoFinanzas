package com.example.finanzasluna;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Encapsula el registro, login y logout contra FirebaseAuth, y la sincronizacion
 * del perfil (rol incluido) con la coleccion "usuarios" de Firestore.
 */
public class AuthRepository {

    public static final String COLECCION_USUARIOS = "usuarios";

    public interface OnUsuarioListener {
        void onExito(Usuario usuario);
        void onError(Exception e);
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public AuthRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // Registro: crea el usuario en FirebaseAuth y su perfil (con rol) en Firestore.
    public void registrar(@NonNull String nombre, @NonNull String email, @NonNull String password,
                           @NonNull String rol, OnUsuarioListener listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(resultado -> {
                    FirebaseUser firebaseUser = resultado.getUser();
                    if (firebaseUser == null) {
                        listener.onError(new IllegalStateException("No se pudo crear el usuario"));
                        return;
                    }
                    Usuario usuario = new Usuario(firebaseUser.getUid(), nombre, email, rol);
                    db.collection(COLECCION_USUARIOS).document(firebaseUser.getUid())
                            .set(usuario)
                            .addOnSuccessListener(unused -> listener.onExito(usuario))
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(listener::onError);
    }

    // Login: autentica y luego lee el perfil (rol) desde Firestore.
    public void login(@NonNull String email, @NonNull String password, OnUsuarioListener listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(resultado -> {
                    FirebaseUser firebaseUser = resultado.getUser();
                    if (firebaseUser == null) {
                        listener.onError(new IllegalStateException("No se pudo iniciar sesion"));
                        return;
                    }
                    db.collection(COLECCION_USUARIOS).document(firebaseUser.getUid())
                            .get()
                            .addOnSuccessListener(doc -> {
                                Usuario usuario = doc.toObject(Usuario.class);
                                if (usuario == null) {
                                    listener.onError(new IllegalStateException("No se encontro el perfil del usuario"));
                                    return;
                                }
                                usuario.setUid(firebaseUser.getUid());
                                listener.onExito(usuario);
                            })
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(listener::onError);
    }

    // Cierre de sesion seguro: destruye el token/sesion de FirebaseAuth.
    public void logout() {
        auth.signOut();
    }

    public boolean haySesionFirebase() {
        return auth.getCurrentUser() != null;
    }
}
