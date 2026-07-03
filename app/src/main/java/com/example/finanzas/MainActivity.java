package com.example.finanzas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements TransaccionAdapter.OnItemListener {

    private DbHelper dbHelper;
    private TransaccionAdapter adapter;
    private RecyclerView recyclerView;
    private TextView txtSaldo;
    private TextView txtVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        dbHelper = new DbHelper(this);

        txtSaldo = findViewById(R.id.txtSaldo);
        txtVacio = findViewById(R.id.txtVacio);
        recyclerView = findViewById(R.id.recyclerTransacciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // El adapter arranca con una lista vacia; se llena en onResume.
        adapter = new TransaccionAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Boton flotante -> abre el formulario para crear una transaccion nueva.
        FloatingActionButton fab = findViewById(R.id.fabAgregar);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FormActivity.class);
            startActivity(intent);
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        cargarTransacciones();
    }

    private void cargarTransacciones() {
        List<Transaccion> lista = dbHelper.obtenerTodas();
        adapter.actualizarLista(lista);

        // Mostrar mensaje cuando no hay registros.
        txtVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);

        calcularSaldo(lista);
    }

    //suma ingresos y resta gastos para mostrar el saldo total
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
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    dbHelper.eliminar(t.getId());
                    cargarTransacciones(); // refresca la lista en tiempo real
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
