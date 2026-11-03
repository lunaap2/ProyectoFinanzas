package com.example.finanzasluna;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Pantalla de entrada (launcher). Si ya hay una sesion activa guardada en
 * SharedPreferences y FirebaseAuth la reconoce, salta directo a MainActivity (Auto-Login).
 */
public class LoginActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private SessionManager sessionManager;

    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputEditText edtEmail;
    private TextInputEditText edtPassword;
    private MaterialButton btnLogin;
    private View progressLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authRepository = new AuthRepository();
        sessionManager = new SessionManager(this);

        // Auto-Login: si la sesion local y la de FirebaseAuth coinciden, no mostramos el formulario.
        if (sessionManager.haySesion() && authRepository.haySesionFirebase()) {
            irAMain();
            return;
        }

        setContentView(R.layout.activity_login);

        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressLogin = findViewById(R.id.progressLogin);

        btnLogin.setOnClickListener(v -> iniciarSesion());

        findViewById(R.id.btnIrRegistro).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void iniciarSesion() {
        String email = edtEmail.getText() == null ? "" : edtEmail.getText().toString().trim();
        String password = edtPassword.getText() == null ? "" : edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Ingresa un correo valido");
            return;
        }
        layoutEmail.setError(null);

        if (TextUtils.isEmpty(password)) {
            layoutPassword.setError("Ingresa tu contrasena");
            return;
        }
        layoutPassword.setError(null);

        mostrarCargando(true);
        authRepository.login(email, password, new AuthRepository.OnUsuarioListener() {
            @Override
            public void onExito(Usuario usuario) {
                sessionManager.guardarSesion(usuario.getUid(), usuario.getNombre(), usuario.getEmail(), usuario.getRol());
                irAMain();
            }

            @Override
            public void onError(Exception e) {
                mostrarCargando(false);
                Toast.makeText(LoginActivity.this, "No se pudo iniciar sesion: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarCargando(boolean cargando) {
        progressLogin.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!cargando);
    }

    private void irAMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
