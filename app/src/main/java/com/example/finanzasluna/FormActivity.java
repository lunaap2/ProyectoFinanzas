package com.example.finanzasluna;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


public class FormActivity extends AppCompatActivity {

    // clave del extra que se usa para pasar el Document ID cuando se va a editar
    public static final String EXTRA_ID = "extra_id";

    private static final int LONGITUD_MINIMA_CONCEPTO = 3;

    private FirestoreRepository repositorio;

    private TextInputLayout layoutConcepto;
    private TextInputLayout layoutMonto;
    private TextInputEditText edtConcepto;
    private TextInputEditText edtMonto;
    private RadioGroup grupoTipo;
    private RadioButton radioIngreso;
    private RadioButton radioGasto;
    private MaterialButton btnGuardar;

    // Document ID de Firestore que se edita. null significa que es una transaccion nueva.
    private String idTransaccion = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        repositorio = new FirestoreRepository();

        layoutConcepto = findViewById(R.id.layoutConcepto);
        layoutMonto = findViewById(R.id.layoutMonto);
        edtConcepto = findViewById(R.id.edtConcepto);
        edtMonto = findViewById(R.id.edtMonto);
        grupoTipo = findViewById(R.id.grupoTipo);
        radioIngreso = findViewById(R.id.radioIngreso);
        radioGasto = findViewById(R.id.radioGasto);
        btnGuardar = findViewById(R.id.btnGuardar);

        configurarValidacionesEnVivo();

        // ¿Venimos a editar? Revisamos si el Intent trae un Document ID valido.
        idTransaccion = getIntent().getStringExtra(EXTRA_ID);
        if (idTransaccion != null) {
            setTitle("Editar transaccion");
            cargarDatosExistentes();
        } else {
            setTitle("Nueva transaccion");
        }

        btnGuardar.setOnClickListener(v -> guardar());
    }

    // Validaciones de negocio en tiempo real mientras el usuario escribe (no solo al guardar).
    private void configurarValidacionesEnVivo() {
        edtConcepto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && s.length() < LONGITUD_MINIMA_CONCEPTO) {
                    layoutConcepto.setError("El concepto es muy corto");
                } else {
                    layoutConcepto.setError(null);
                }
            }
        });

        edtMonto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    layoutMonto.setError(null);
                    return;
                }
                try {
                    double monto = Double.parseDouble(s.toString());
                    layoutMonto.setError(monto <= 0 ? "El monto debe ser mayor a $0" : null);
                } catch (NumberFormatException e) {
                    layoutMonto.setError("Monto invalido");
                }
            }
        });
    }

    private void cargarDatosExistentes() {
        btnGuardar.setEnabled(false);
        repositorio.obtenerPorId(idTransaccion, t -> {
            btnGuardar.setEnabled(true);
            if (t == null) {
                Toast.makeText(this, "No se encontro la transaccion", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            edtConcepto.setText(t.getConcepto());
            edtMonto.setText(String.valueOf(t.getMonto()));
            if (t.esIngreso()) {
                radioIngreso.setChecked(true);
            } else {
                radioGasto.setChecked(true);
            }
        });
    }

    private void guardar() {
        String concepto = edtConcepto.getText() == null ? "" : edtConcepto.getText().toString().trim();
        String montoTexto = edtMonto.getText() == null ? "" : edtMonto.getText().toString().trim();

        // Ningun campo critico puede llegar vacio a Firestore.
        if (TextUtils.isEmpty(concepto)) {
            layoutConcepto.setError("Escribe un concepto");
            return;
        }
        if (concepto.length() < LONGITUD_MINIMA_CONCEPTO) {
            layoutConcepto.setError("El concepto es muy corto");
            return;
        }
        if (TextUtils.isEmpty(montoTexto)) {
            layoutMonto.setError("Escribe un monto");
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(montoTexto);
        } catch (NumberFormatException e) {
            layoutMonto.setError("Monto invalido");
            return;
        }
        if (monto <= 0) {
            layoutMonto.setError("El monto debe ser mayor a $0");
            return;
        }

        layoutConcepto.setError(null);
        layoutMonto.setError(null);

        String tipo = radioIngreso.isChecked()
                ? Transaccion.TIPO_INGRESO
                : Transaccion.TIPO_GASTO;
        Transaccion t = new Transaccion(concepto, monto, tipo);

        // Se deshabilita el boton mientras se guarda para evitar el doble envio.
        btnGuardar.setEnabled(false);

        FirestoreRepository.OnGuardadoListener listener = new FirestoreRepository.OnGuardadoListener() {
            @Override
            public void onExito() {
                Toast.makeText(FormActivity.this,
                        idTransaccion == null ? "Transaccion guardada" : "Transaccion actualizada",
                        Toast.LENGTH_SHORT).show();
                // cierra la pantalla; el SnapshotListener de MainActivity refresca la lista solo
                finish();
            }

            @Override
            public void onError(Exception e) {
                btnGuardar.setEnabled(true);
                Toast.makeText(FormActivity.this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        if (idTransaccion == null) {
            // Modo CREAR.
            repositorio.crear(t, listener);
        } else {
            // Modo EDITAR: apunta al Document ID existente.
            repositorio.actualizar(idTransaccion, t, listener);
        }
    }
}
