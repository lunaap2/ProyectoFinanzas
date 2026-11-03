package com.example.finanzasluna;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class FormActivity extends AppCompatActivity {

    // clave del extra que se usa para pasar el Document ID cuando se va a editar
    public static final String EXTRA_ID = "extra_id";

    private static final int LONGITUD_MINIMA_CONCEPTO = 3;

    private FirestoreRepository repositorio;
    private StorageRepository storageRepository;
    private LocationHelper locationHelper;
    private SessionManager sessionManager;

    private TextInputLayout layoutConcepto;
    private TextInputLayout layoutMonto;
    private TextInputEditText edtConcepto;
    private TextInputEditText edtMonto;
    private RadioGroup grupoTipo;
    private RadioButton radioIngreso;
    private RadioButton radioGasto;
    private MaterialButton btnGuardar;

    private ImageView imgEvidencia;
    private ProgressBar progressSubida;
    private TextView txtDireccion;

    // Document ID de Firestore que se edita. null significa que es una transaccion nueva.
    private String idTransaccion = null;

    // Foto nueva elegida en esta sesion de edicion (camara o galeria). Si es null y ya existia
    // una foto previa, se conserva fotoUrlExistente al guardar.
    private Bitmap bitmapEvidenciaNueva = null;
    private String fotoUrlExistente = null;

    private Double latitudCapturada = null;
    private Double longitudCapturada = null;
    private String direccionCapturada = null;

    private final ActivityResultLauncher<Void> tomarFotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    bitmapEvidenciaNueva = bitmap;
                    imgEvidencia.setImageBitmap(bitmap);
                }
            });

    private final ActivityResultLauncher<String> permisoCamaraLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    tomarFotoLauncher.launch(null);
                } else {
                    Toast.makeText(this, "Se necesita permiso de camara", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> elegirDeGaleriaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    cargarBitmapDesdeUri(uri);
                }
            });

    private final ActivityResultLauncher<String[]> permisoUbicacionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultados -> {
                boolean concedido = Boolean.TRUE.equals(resultados.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(resultados.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (concedido) {
                    capturarUbicacion();
                } else {
                    Toast.makeText(this, "Se necesita permiso de ubicacion", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        repositorio = new FirestoreRepository();
        storageRepository = new StorageRepository();
        locationHelper = new LocationHelper(this);
        sessionManager = new SessionManager(this);

        layoutConcepto = findViewById(R.id.layoutConcepto);
        layoutMonto = findViewById(R.id.layoutMonto);
        edtConcepto = findViewById(R.id.edtConcepto);
        edtMonto = findViewById(R.id.edtMonto);
        grupoTipo = findViewById(R.id.grupoTipo);
        radioIngreso = findViewById(R.id.radioIngreso);
        radioGasto = findViewById(R.id.radioGasto);
        btnGuardar = findViewById(R.id.btnGuardar);
        imgEvidencia = findViewById(R.id.imgEvidencia);
        progressSubida = findViewById(R.id.progressSubida);
        txtDireccion = findViewById(R.id.txtDireccion);

        configurarValidacionesEnVivo();

        findViewById(R.id.btnCamara).setOnClickListener(v -> abrirCamara());
        findViewById(R.id.btnGaleria).setOnClickListener(v -> elegirDeGaleriaLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        findViewById(R.id.btnUbicacion).setOnClickListener(v -> pedirUbicacion());

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

    private void abrirCamara() {
        permisoCamaraLauncher.launch(Manifest.permission.CAMERA);
    }

    private void pedirUbicacion() {
        permisoUbicacionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void capturarUbicacion() {
        txtDireccion.setText("Obteniendo ubicacion...");
        locationHelper.obtenerUbicacionActual(new LocationHelper.OnUbicacionListener() {
            @Override
            public void onUbicacion(double latitud, double longitud, String direccion) {
                latitudCapturada = latitud;
                longitudCapturada = longitud;
                direccionCapturada = direccion;
                txtDireccion.setText(direccion);
            }

            @Override
            public void onError(Exception e) {
                txtDireccion.setText("");
                Toast.makeText(FormActivity.this, "No se pudo obtener la ubicacion: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void cargarBitmapDesdeUri(Uri uri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                @SuppressWarnings("deprecation")
                Bitmap legacy = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                bitmap = legacy;
            }
            bitmapEvidenciaNueva = bitmap;
            imgEvidencia.setImageBitmap(bitmap);
        } catch (IOException e) {
            Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show();
        }
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

            fotoUrlExistente = t.getFotoUrl();
            if (!TextUtils.isEmpty(fotoUrlExistente)) {
                Glide.with(this).load(fotoUrlExistente).into(imgEvidencia);
            }

            if (t.tieneUbicacion()) {
                latitudCapturada = t.getLatitud();
                longitudCapturada = t.getLongitud();
                direccionCapturada = t.getDireccion();
                txtDireccion.setText(direccionCapturada);
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
        t.setUserId(sessionManager.getUid());
        t.setUserEmail(sessionManager.getEmail());
        t.setLatitud(latitudCapturada);
        t.setLongitud(longitudCapturada);
        t.setDireccion(direccionCapturada);

        // Se deshabilita el boton mientras se guarda para evitar el doble envio.
        btnGuardar.setEnabled(false);

        if (bitmapEvidenciaNueva != null) {
            subirFotoYGuardar(t);
        } else {
            t.setFotoUrl(fotoUrlExistente);
            guardarEnFirestore(t);
        }
    }

    private void subirFotoYGuardar(Transaccion t) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmapEvidenciaNueva.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] datos = baos.toByteArray();

        progressSubida.setVisibility(View.VISIBLE);
        progressSubida.setProgress(0);

        storageRepository.subirImagen(datos, sessionManager.getUid(), new StorageRepository.OnSubidaListener() {
            @Override
            public void onProgreso(int porcentaje) {
                progressSubida.setProgress(porcentaje);
            }

            @Override
            public void onExito(String downloadUrl) {
                t.setFotoUrl(downloadUrl);
                guardarEnFirestore(t);
            }

            @Override
            public void onError(Exception e) {
                progressSubida.setVisibility(View.GONE);
                btnGuardar.setEnabled(true);
                Toast.makeText(FormActivity.this, "Error al subir la foto: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void guardarEnFirestore(Transaccion t) {
        FirestoreRepository.OnGuardadoListener listener = new FirestoreRepository.OnGuardadoListener() {
            @Override
            public void onExito() {
                progressSubida.setVisibility(View.GONE);
                boolean esNueva = idTransaccion == null;
                Toast.makeText(FormActivity.this,
                        esNueva ? "Transaccion guardada" : "Transaccion actualizada",
                        Toast.LENGTH_SHORT).show();
                NotificationHelper.mostrarNotificacion(FormActivity.this,
                        esNueva ? "Transaccion creada" : "Transaccion actualizada",
                        t.getConcepto() + " · $" + t.getMonto());
                // cierra la pantalla; el SnapshotListener de MainActivity refresca la lista solo
                finish();
            }

            @Override
            public void onError(Exception e) {
                progressSubida.setVisibility(View.GONE);
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
