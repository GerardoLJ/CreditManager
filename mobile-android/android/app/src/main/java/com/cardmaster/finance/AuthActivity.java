package com.cardmaster.finance;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cardmaster.finance.db.CardMasterDbHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AuthActivity extends AppCompatActivity {

    private CardMasterDbHelper dbHelper;

    private TextView tvAuthTitle;
    private TextView tvAuthSubtitle;
    private TextView tvAuthStatusText;
    private TextView tvPwdLabel;
    private EditText etMasterPassword;
    private LinearLayout layoutConfirmPassword;
    private EditText etMasterPasswordConfirm;
    private TextView tvAuthError;
    private Button btnAuthSubmit;
    private Button btnAuthImportJson;
    private Button btnAuthFactoryReset;

    private boolean isConfigured = false;

    private final ActivityResultLauncher<Intent> importJsonLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        handleImportBackupUri(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        View root = findViewById(R.id.scroll_auth_root);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, systemBars.top, 0, systemBars.bottom);
                return insets;
            });
        }

        dbHelper = CardMasterDbHelper.getInstance(this);

        tvAuthTitle = findViewById(R.id.tv_auth_title);
        tvAuthSubtitle = findViewById(R.id.tv_auth_subtitle);
        tvAuthStatusText = findViewById(R.id.tv_auth_status_text);
        tvPwdLabel = findViewById(R.id.tv_pwd_label);
        etMasterPassword = findViewById(R.id.et_master_password);
        layoutConfirmPassword = findViewById(R.id.layout_confirm_password);
        etMasterPasswordConfirm = findViewById(R.id.et_master_password_confirm);
        tvAuthError = findViewById(R.id.tv_auth_error);
        btnAuthSubmit = findViewById(R.id.btn_auth_submit);
        btnAuthImportJson = findViewById(R.id.btn_auth_import_json);
        btnAuthFactoryReset = findViewById(R.id.btn_auth_factory_reset);

        setupListeners();
        refreshAuthState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAuthState();
    }

    private void refreshAuthState() {
        isConfigured = dbHelper.isVaultConfigured();
        tvAuthError.setVisibility(View.GONE);
        etMasterPassword.setText("");
        etMasterPasswordConfirm.setText("");

        if (!isConfigured) {
            tvAuthTitle.setText("Configurar Bóveda");
            tvAuthSubtitle.setText("Establece una contraseña maestra para proteger tus finanzas en este dispositivo.");
            tvAuthStatusText.setText("Nueva Instalación • Sin Contraseña");
            tvPwdLabel.setText("Nueva Contraseña Maestra");
            layoutConfirmPassword.setVisibility(View.VISIBLE);
            btnAuthSubmit.setText("Crear Bóveda Segura");
            btnAuthFactoryReset.setVisibility(View.GONE);
        } else {
            tvAuthTitle.setText("CardMaster");
            tvAuthSubtitle.setText("Bóveda Local Protegida (100% Nativo Offline)");
            tvAuthStatusText.setText("Bóveda SQLite Offline • Activa");
            tvPwdLabel.setText("Contraseña Maestra");
            layoutConfirmPassword.setVisibility(View.GONE);
            btnAuthSubmit.setText("Desbloquear");
            btnAuthFactoryReset.setVisibility(View.VISIBLE);
        }
    }

    private void setupListeners() {
        btnAuthSubmit.setOnClickListener(v -> handleSubmit());

        btnAuthImportJson.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimeTypes = {"application/json", "text/plain", "application/octet-stream"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            importJsonLauncher.launch(intent);
        });

        btnAuthFactoryReset.setOnClickListener(v -> showFactoryResetConfirmation());
    }

    private void handleSubmit() {
        tvAuthError.setVisibility(View.GONE);
        String pwd = etMasterPassword.getText().toString();

        if (pwd.trim().isEmpty()) {
            showError("Por favor ingresa la contraseña maestra.");
            return;
        }

        if (!isConfigured) {
            String confirm = etMasterPasswordConfirm.getText().toString();
            if (pwd.length() < 4) {
                showError("La contraseña debe tener al menos 4 caracteres.");
                return;
            }
            if (!pwd.equals(confirm)) {
                showError("Las contraseñas no coinciden.");
                return;
            }

            boolean ok = dbHelper.setupMasterPassword(pwd);
            if (ok) {
                Toast.makeText(this, "¡Bóveda configurada exitosamente!", Toast.LENGTH_SHORT).show();
                openMainActivity();
            } else {
                showError("Error al inicializar la bóveda de seguridad.");
            }
        } else {
            boolean valid = dbHelper.verifyMasterPassword(pwd);
            if (valid) {
                openMainActivity();
            } else {
                showError("Contraseña incorrecta. Inténtalo nuevamente.");
            }
        }
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        tvAuthError.setText(message);
        tvAuthError.setVisibility(View.VISIBLE);
    }

    private void handleImportBackupUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) {
                Toast.makeText(this, "No se pudo leer el archivo seleccionado.", Toast.LENGTH_LONG).show();
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            is.close();

            boolean success = dbHelper.importBackupJson(sb.toString());
            if (success) {
                new AlertDialog.Builder(this)
                        .setTitle("Respaldo Importado")
                        .setMessage("La base de datos se ha restaurado correctamente.\n\nSi el respaldo incluía contraseña maestra, úsala ahora para desbloquear.")
                        .setPositiveButton("Aceptar", (d, w) -> refreshAuthState())
                        .setCancelable(false)
                        .show();
            } else {
                Toast.makeText(this, "El archivo no contiene un respaldo válido de CardMaster.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Error al Importar")
                    .setMessage("No se pudo procesar el archivo:\n" + e.getMessage())
                    .setPositiveButton("Cerrar", null)
                    .show();
        }
    }

    private void showFactoryResetConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Restaurar de Fábrica")
                .setMessage("Esta acción eliminará todas las tarjetas, gastos, presupuestos y contraseñas de esta aplicación.\n\n¿Estás seguro de continuar?")
                .setPositiveButton("Eliminar Todo", (dialog, which) -> {
                    dbHelper.factoryReset();
                    Toast.makeText(this, "La aplicación ha sido restaurada de fábrica.", Toast.LENGTH_LONG).show();
                    refreshAuthState();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}

