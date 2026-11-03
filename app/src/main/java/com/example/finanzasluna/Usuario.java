package com.example.finanzasluna;

/**
 * Representa el documento de un usuario en la coleccion "usuarios" de Firestore.
 * El rol ("admin" o "operario") controla el RBAC de la app.
 */
public class Usuario {

    public static final String ROL_ADMIN = "admin";
    public static final String ROL_OPERARIO = "operario";

    private String uid;
    private String nombre;
    private String email;
    private String rol;
    private String fcmToken;

    // Constructor vacio requerido por Firestore para el mapeo automatico (POJO).
    public Usuario() {
    }

    public Usuario(String uid, String nombre, String email, String rol) {
        this.uid = uid;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public boolean esAdmin() {
        return ROL_ADMIN.equals(rol);
    }
}
