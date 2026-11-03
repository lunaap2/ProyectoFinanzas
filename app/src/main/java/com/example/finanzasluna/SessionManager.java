package com.example.finanzasluna;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persiste la sesion activa en SharedPreferences para permitir el Auto-Login
 * y para consultar el rol del usuario (RBAC) sin volver a Firestore en cada pantalla.
 */
public class SessionManager {

    private static final String PREFS = "finanzas_session";
    private static final String KEY_UID = "uid";
    private static final String KEY_NOMBRE = "nombre";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROL = "rol";
    private static final String KEY_HAY_SESION = "hay_sesion";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void guardarSesion(String uid, String nombre, String email, String rol) {
        prefs.edit()
                .putString(KEY_UID, uid)
                .putString(KEY_NOMBRE, nombre)
                .putString(KEY_EMAIL, email)
                .putString(KEY_ROL, rol)
                .putBoolean(KEY_HAY_SESION, true)
                .apply();
    }

    public boolean haySesion() {
        return prefs.getBoolean(KEY_HAY_SESION, false);
    }

    public String getUid() {
        return prefs.getString(KEY_UID, null);
    }

    public String getNombre() {
        return prefs.getString(KEY_NOMBRE, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getRol() {
        return prefs.getString(KEY_ROL, Usuario.ROL_OPERARIO);
    }

    public boolean esAdmin() {
        return Usuario.ROL_ADMIN.equals(getRol());
    }

    // Cierre de sesion seguro: limpia toda la cache local de la sesion.
    public void cerrarSesion() {
        prefs.edit().clear().apply();
    }
}
