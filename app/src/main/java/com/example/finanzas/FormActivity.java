package com.example.finanzas;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class FormActivity extends AppCompatActivity {

    //clave del extra que se usa para pasar el id cuando se va a editar
    public static final String EXTRA_ID = "extra_id";

    private DbHelper dbHelper;

    private EditText edtConcepto;
    private EditText edtMonto;
    private RadioGroup grupoTipo;
    private RadioButton radioIngreso;
    private RadioButton radioGasto;
    private Button btnGuardar;

    //ID de la transaccion que se edita. -1 significa que es una nueva.
    private long idTransaccion = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        dbHelper = new DbHelper(this);

        edtConcepto = findViewById(R.id.edtConcepto);
        edtMonto = findViewById(R.id.edtMonto);
        grupoTipo = findViewById(R.id.grupoTipo);
        radioIngreso = findViewById(R.id.radioIngreso);
        radioGasto = findViewById(R.id.radioGasto);
        btnGuardar = findViewById(R.id.btnGuardar);

        // ¿Venimos a editar? Revisamos si el Intent trae un id valido.
        idTransaccion = getIntent().getLongExtra(EXTRA_ID, -1);
        if (idTransaccion != -1) {
            setTitle("Editar transaccion");
            cargarDatosExistentes();
        } else {
            setTitle("Nueva transaccion");
        }

        btnGuardar.setOnClickListener(v -> guardar());
    }

    private void cargarDatosExistentes() {
        Transaccion t = dbHelper.obtenerPorId(idTransaccion);
        if (t == null) {
            return;
        }
        edtConcepto.setText(t.getConcepto());
        edtMonto.setText(String.valueOf(t.getMonto()));
        if (t.esIngreso()) {
            radioIngreso.setChecked(true);
        } else {
            radioGasto.setChecked(true);
        }
    }

    private void guardar() {
        String concepto = edtConcepto.getText().toString().trim();
        String montoTexto = edtMonto.getText().toString().trim();

        // Validaciones basicas para evitar crashes y datos vacios.
        if (TextUtils.isEmpty(concepto)) {
            edtConcepto.setError("Escribe un concepto");
            return;
        }
        if (TextUtils.isEmpty(montoTexto)) {
            edtMonto.setError("Escribe un monto");
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(montoTexto);
        } catch (NumberFormatException e) {
            edtMonto.setError("Monto invalido");
            return;
        }
        if (monto <= 0) {
            edtMonto.setError("El monto debe ser mayor a 0");
            return;
        }

        String tipo = radioIngreso.isChecked()
                ? Transaccion.TIPO_INGRESO
                : Transaccion.TIPO_GASTO;

        if (idTransaccion == -1) {
            // Modo CREAR.
            Transaccion nueva = new Transaccion(concepto, monto, tipo);
            dbHelper.insertar(nueva);
            Toast.makeText(this, "Transaccion guardada", Toast.LENGTH_SHORT).show();
        } else {
            // Modo EDITAR.
            Transaccion editada = new Transaccion(idTransaccion, concepto, monto, tipo);
            dbHelper.actualizar(editada);
            Toast.makeText(this, "Transaccion actualizada", Toast.LENGTH_SHORT).show();
        }

        //cierra la pantalla; al volver, MainActivity recarga la lista en onResume
        finish();
    }
}
