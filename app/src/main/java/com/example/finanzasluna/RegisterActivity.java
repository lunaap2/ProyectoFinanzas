package com.example.finanzasluna;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Registro de un nuevo usuario. El rol (admin/operario) se elige aqui con un RadioGroup:
 * es una simplificacion academica para poder crear facilmente las cuentas de prueba
 * (en un entorno de produccion el rol no deberia ser auto-asignable por el propio usuario).
 */
public class RegisterActivity extends AppCompatActivity {

    private static final int LONGITUD_MINIMA_PASSWORD = 6;

    private AuthRepository authRepository;
    private SessionManager sessionManager;

    private TextInputLayout layoutNombre;
    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputLayout layoutConfirmarPassword;
    private TextInputEditText edtNombre;
    private TextInputEditText edtEmail;
    private TextInputEditText edtPassword;
    private TextInputEditText edtConfirmarPassword;
    private RadioGroup grupoRol;
    private MaterialButton btnRegistrar;
    private View progressRegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = new AuthRepository();
        sessionManager = new SessionManager(this);

        layoutNombre = findViewById(R.id.layoutNombre);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        layoutConfirmarPassword = findViewById(R.id.layoutConfirmarPassword);
        edtNombre = findViewById(R.id.edtNombre);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmarPassword = findViewById(R.id.edtConfirmarPassword);
        grupoRol = findViewById(R.id.grupoRol);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        progressRegistro = findViewById(R.id.progressRegistro);

        btnRegistrar.setOnClickListener(v -> registrar());
    }

    private void registrar() {
        String nombre = textoDe(edtNombre);
        String email = textoDe(edtEmail);
        String password = textoDe(edtPassword);
        String confirmar = textoDe(edtConfirmarPassword);

        if (TextUtils.isEmpty(nombre)) {
            layoutNombre.setError("Escribe tu nombre");
            return;
        }
        layoutNombre.setError(null);

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Ingresa un correo valido");
            return;
        }
        layoutEmail.setError(null);

        if (password.length() < LONGITUD_MINIMA_PASSWORD) {
            layoutPassword.setError("Minimo " + LONGITUD_MINIMA_PASSWORD + " caracteres");
            return;
        }
        layoutPassword.setError(null);

        if (!password.equals(confirmar)) {
            layoutConfirmarPassword.setError("Las contrasenas no coinciden");
            return;
        }
        layoutConfirmarPassword.setError(null);

        String rol = grupoRol.getCheckedRadioButtonId() == R.id.radioAdministrador
                ? Usuario.ROL_ADMIN
                : Usuario.ROL_OPERARIO;

        mostrarCargando(true);
        authRepository.registrar(nombre, email, password, rol, new AuthRepository.OnUsuarioListener() {
            @Override
            public void onExito(Usuario usuario) {
                sessionManager.guardarSesion(usuario.getUid(), usuario.getNombre(), usuario.getEmail(), usuario.getRol());
                Toast.makeText(RegisterActivity.this, "Cuenta creada", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(Exception e) {
                mostrarCargando(false);
                Toast.makeText(RegisterActivity.this, "No se pudo registrar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }

    private void mostrarCargando(boolean cargando) {
        progressRegistro.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnRegistrar.setEnabled(!cargando);
    }
}
