package com.example.finanzasluna;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements TransaccionAdapter.OnItemListener {

    private static final String TAG = "MainActivity";

    private FirestoreRepository repositorio;
    private SessionManager sessionManager;
    private AuthRepository authRepository;
    private ListenerRegistration listenerRegistration;

    private TransaccionAdapter adapter;
    private RecyclerView recyclerView;
    private TextView txtSaldo;
    private TextView txtUsuario;
    private TextView txtVacio;

    private final ActivityResultLauncher<String> solicitarPermisoNotificaciones =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                // Si el usuario no concede el permiso, simplemente no vera notificaciones;
                // el resto de la app sigue funcionando con normalidad.
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        authRepository = new AuthRepository();

        // Guard de sesion: si alguien llega directo a esta pantalla sin haber iniciado sesion,
        // se le redirige al Login.
        if (!sessionManager.haySesion() || !authRepository.haySesionFirebase()) {
            irALogin();
            return;
        }

        setContentView(R.layout.activity_main);

        repositorio = new FirestoreRepository();

        pedirPermisoNotificacionesSiAplica();
        NotificationHelper.crearCanal(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtSaldo = findViewById(R.id.txtSaldo);
        txtUsuario = findViewById(R.id.txtUsuario);
        txtVacio = findViewById(R.id.txtVacio);
        recyclerView = findViewById(R.id.recyclerTransacciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        String rolLegible = sessionManager.esAdmin() ? "Administrador" : "Operario";
        txtUsuario.setText(String.format("Hola, %s (%s)", sessionManager.getNombre(), rolLegible));

        // El adapter arranca con una lista vacia; el SnapshotListener la llena en tiempo real.
        // El rol (RBAC) decide si el boton de eliminar se muestra en cada tarjeta.
        adapter = new TransaccionAdapter(new ArrayList<>(), this, sessionManager.esAdmin());
        recyclerView.setAdapter(adapter);

        // Boton flotante -> abre el formulario para crear una transaccion nueva.
        FloatingActionButton fab = findViewById(R.id.fabAgregar);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FormActivity.class);
            startActivity(intent);
        });

        // Conecta el RecyclerView a Firestore: cualquier cambio (creacion, edicion o borrado)
        // hecho desde cualquier telefono se refleja aqui automaticamente, sin recargar nada.
        listenerRegistration = repositorio.escucharCambios((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Error escuchando cambios en Firestore", error);
                return;
            }
            if (snapshots == null) {
                return;
            }

            List<Transaccion> lista = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshots) {
                Transaccion t = doc.toObject(Transaccion.class);
                t.setId(doc.getId());
                lista.add(t);
            }

            adapter.actualizarLista(lista);
            txtVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            calcularSaldo(lista);
        });
    }

    // Android 13+ exige pedir este permiso en tiempo de ejecucion antes de poder notificar.
    private void pedirPermisoNotificacionesSiAplica() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            solicitarPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.accionMapa) {
            startActivity(new Intent(this, MapaActivity.class));
            return true;
        }
        if (id == R.id.accionCerrarSesion) {
            cerrarSesion();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Cierre de sesion seguro: destruye el token de FirebaseAuth y limpia la cache local
    // (SharedPreferences) antes de volver al Login.
    private void cerrarSesion() {
        authRepository.logout();
        sessionManager.cerrarSesion();
        irALogin();
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Libera la memoria/conexion del listener de Firestore al cerrar la pantalla.
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    // suma ingresos y resta gastos para mostrar el saldo total
    private void calcularSaldo(List<Transaccion> lista) {
        double saldo = 0;
        for (Transaccion t : lista) {
            if (t.esIngreso()) {
                saldo += t.getMonto();
            } else {
                saldo -= t.getMonto();
            }
        }
        txtSaldo.setText(String.format(Locale.getDefault(), "Saldo: $%.2f", saldo));
    }

    @Override
    public void onItemClick(Transaccion t) {
        Intent intent = new Intent(this, FormActivity.class);
        intent.putExtra(FormActivity.EXTRA_ID, t.getId());
        startActivity(intent);
    }

    @Override
    public void onItemLongClick(Transaccion t) {
        // Solo llega aqui si el adapter permitio la accion (ya filtrada por rol Administrador).
        new AlertDialog.Builder(this)
                .setTitle("Eliminar")
                .setMessage("¿Deseas eliminar \"" + t.getConcepto() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) ->
                        repositorio.eliminar(t.getId(), new FirestoreRepository.OnGuardadoListener() {
                            @Override
                            public void onExito() {
                                // El SnapshotListener actualiza la lista solo; no hace falta recargar.
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Error al eliminar la transaccion", e);
                            }
                        }))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
