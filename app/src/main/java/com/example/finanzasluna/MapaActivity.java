package com.example.finanzasluna;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

/**
 * Mapa interactivo con un pin por cada transaccion que tenga coordenadas guardadas.
 * Se alimenta del mismo SnapshotListener de Firestore que la lista principal.
 */
public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapaActivity";

    private FirestoreRepository repositorio;
    private ListenerRegistration listenerRegistration;
    private GoogleMap mapa;
    private boolean modoSatelite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        repositorio = new FirestoreRepository();

        FragmentManager fm = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) fm.findFragmentById(R.id.mapa);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapa = googleMap;
        mapa.getUiSettings().setZoomControlsEnabled(true);
        mapa.getUiSettings().setZoomGesturesEnabled(true);
        mapa.getUiSettings().setCompassEnabled(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mapa.setMyLocationEnabled(true);
        }

        listenerRegistration = repositorio.escucharCambios((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Error escuchando cambios en Firestore", error);
                return;
            }
            if (snapshots == null) {
                return;
            }
            mapa.clear();

            LatLngBounds.Builder bounds = LatLngBounds.builder();
            boolean hayAlMenosUno = false;

            for (QueryDocumentSnapshot doc : snapshots) {
                Transaccion t = doc.toObject(Transaccion.class);
                if (!t.tieneUbicacion()) {
                    continue;
                }
                LatLng posicion = new LatLng(t.getLatitud(), t.getLongitud());
                String snippet = String.format(Locale.getDefault(), "%s $%.2f · %s",
                        t.esIngreso() ? "+" : "-", t.getMonto(),
                        t.getDireccion() != null ? t.getDireccion() : "");

                mapa.addMarker(new MarkerOptions()
                        .position(posicion)
                        .title(t.getConcepto())
                        .snippet(snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                t.esIngreso() ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED)));

                bounds.include(posicion);
                hayAlMenosUno = true;
            }

            if (hayAlMenosUno) {
                try {
                    mapa.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100));
                } catch (IllegalStateException e) {
                    // Puede fallar si solo hay un punto sin area; se ignora, el marcador ya quedo visible.
                    Log.w(TAG, "No se pudo ajustar la camara a los limites", e);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mapa, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.accionAlternarCapa) {
            modoSatelite = !modoSatelite;
            if (mapa != null) {
                mapa.setMapType(modoSatelite ? GoogleMap.MAP_TYPE_SATELLITE : GoogleMap.MAP_TYPE_NORMAL);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
