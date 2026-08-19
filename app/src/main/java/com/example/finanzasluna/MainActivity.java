package com.example.finanzasluna;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements TransaccionAdapter.OnItemListener {

    private static final String TAG = "MainActivity";

    private FirestoreRepository repositorio;
    private ListenerRegistration listenerRegistration;

    private TransaccionAdapter adapter;
    private RecyclerView recyclerView;
    private TextView txtSaldo;
    private TextView txtVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repositorio = new FirestoreRepository();

        txtSaldo = findViewById(R.id.txtSaldo);
        txtVacio = findViewById(R.id.txtVacio);
        recyclerView = findViewById(R.id.recyclerTransacciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // El adapter arranca con una lista vacia; el SnapshotListener la llena en tiempo real.
        adapter = new TransaccionAdapter(new ArrayList<>(), this);
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
