package com.example.finanzasluna;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Obtiene la ubicacion actual via FusedLocationProviderClient y la traduce a una
 * direccion legible con Geocoder (geocodificacion inversa). El llamador es responsable
 * de verificar/pedir el permiso de ubicacion ANTES de invocar obtenerUbicacionActual.
 */
public class LocationHelper {

    private static final String TAG = "LocationHelper";

    public interface OnUbicacionListener {
        void onUbicacion(double latitud, double longitud, String direccion);
        void onError(Exception e);
    }

    private final Context context;
    private final FusedLocationProviderClient cliente;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LocationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.cliente = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void obtenerUbicacionActual(OnUbicacionListener listener) {
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        listener.onError(new IllegalStateException("No se pudo obtener la ubicacion. Verifica que el GPS este activo."));
                        return;
                    }
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();
                    // La geocodificacion inversa hace una consulta que puede tardar; se corre fuera del hilo principal.
                    executor.execute(() -> {
                        String direccion = geocodificarInverso(lat, lng);
                        new Handler(Looper.getMainLooper()).post(() -> listener.onUbicacion(lat, lng, direccion));
                    });
                })
                .addOnFailureListener(listener::onError);
    }

    private String geocodificarInverso(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            @SuppressWarnings("deprecation")
            List<Address> direcciones = geocoder.getFromLocation(lat, lng, 1);
            if (direcciones != null && !direcciones.isEmpty()) {
                String linea = direcciones.get(0).getAddressLine(0);
                if (linea != null && !linea.trim().isEmpty()) {
                    return linea;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en geocodificacion inversa", e);
        }
        return String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng);
    }
}
