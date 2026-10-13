package com.cardmaster.finance;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.graphics.drawable.GradientDrawable;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.cardmaster.finance.db.CardMasterDbHelper;
import com.cardmaster.finance.model.Budget;
import com.cardmaster.finance.model.BudgetItem;
import com.cardmaster.finance.model.Card;
import com.cardmaster.finance.model.InstallmentPlan;
import com.cardmaster.finance.model.Movement;
import com.cardmaster.finance.model.Person;
import com.cardmaster.finance.model.SetAside;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private CardMasterDbHelper dbHelper;
    private DrawerLayout drawerLayout;
    private FrameLayout containerBody;
    private TextView tvToolbarSubtitle;

    private LinearLayout[] sidebarMenuItems;
    private TextView[] sidebarMenuTexts;
    private int currentTab = 0;

    private static final String[] MODULE_TITLES = {
            "💳 Tarjetas de Crédito",
            "💸 Historial de Gastos",
            "💰 Fondos Apartados",
            "📅 Calendario de Pagos",
            "👥 Deudores y Personas",
            "📊 Presupuestos e Inflación",
            "⚙️ Ajustes y Respaldos"
    };

    // Filter states for movements tab
    private String selectedFilterCardId = "";
    private String selectedFilterPersonId = "";

    // SAF ActivityResultLaunchers
    private final ActivityResultLauncher<Intent> exportJsonLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        handleExportBackupToUri(uri);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> importJsonLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        handleImportBackupFromUri(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = CardMasterDbHelper.getInstance(this);

        // Security check: if vault is not configured, redirect to AuthActivity
        if (!dbHelper.isVaultConfigured()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        containerBody = findViewById(R.id.container_body);
        tvToolbarSubtitle = findViewById(R.id.tv_toolbar_subtitle);

        // Adjust top toolbar padding to avoid overlapping the status bar, notch or clock
        View topToolbar = findViewById(R.id.top_toolbar);
        if (topToolbar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topToolbar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 8, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        // Adjust sidebar header padding to respect status bar
        View sidebarHeader = findViewById(R.id.sidebar_header);
        if (sidebarHeader != null) {
            ViewCompat.setOnApplyWindowInsetsListener(sidebarHeader, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 12, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        // Drawer toggle button (Hamburger icon ☰)
        View btnToggle = findViewById(R.id.btn_toggle_drawer);
        if (btnToggle != null) {
            btnToggle.setOnClickListener(v -> toggleDrawer());
        }

        // Drawer close button (✕)
        View btnClose = findViewById(R.id.btn_close_drawer);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> closeDrawer());
        }

        // Lock buttons
        View.OnClickListener lockListener = v -> {
            Intent intent = new Intent(this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        };

        findViewById(R.id.btn_lock_app).setOnClickListener(lockListener);
        View btnSidebarLock = findViewById(R.id.btn_sidebar_lock);
        if (btnSidebarLock != null) {
            btnSidebarLock.setOnClickListener(lockListener);
        }

        // Intercept back button to close drawer first if open
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        setupSidebarNavigation();
        selectTab(0);
    }

    private void toggleDrawer() {
        if (drawerLayout == null) return;
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void closeDrawer() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void setupSidebarNavigation() {
        sidebarMenuItems = new LinearLayout[]{
                findViewById(R.id.menu_item_cards),
                findViewById(R.id.menu_item_movements),
                findViewById(R.id.menu_item_set_asides),
                findViewById(R.id.menu_item_calendar),
                findViewById(R.id.menu_item_debtors),
                findViewById(R.id.menu_item_budgets),
                findViewById(R.id.menu_item_settings)
        };

        sidebarMenuTexts = new TextView[]{
                findViewById(R.id.tv_menu_cards),
                findViewById(R.id.tv_menu_movements),
                findViewById(R.id.tv_menu_set_asides),
                findViewById(R.id.tv_menu_calendar),
                findViewById(R.id.tv_menu_debtors),
                findViewById(R.id.tv_menu_budgets),
                findViewById(R.id.tv_menu_settings)
        };

        for (int i = 0; i < sidebarMenuItems.length; i++) {
            final int index = i;
            if (sidebarMenuItems[i] != null) {
                sidebarMenuItems[i].setOnClickListener(v -> {
                    selectTab(index);
                    closeDrawer();
                });
            }
        }
    }

    public void selectTab(int index) {
        currentTab = index;

        if (sidebarMenuItems != null) {
            for (int i = 0; i < sidebarMenuItems.length; i++) {
                if (sidebarMenuItems[i] != null && sidebarMenuTexts[i] != null) {
                    if (i == index) {
                        sidebarMenuItems[i].setBackgroundResource(R.drawable.bg_sidebar_item_selected);
                        sidebarMenuTexts[i].setTextColor(Color.WHITE);
                        sidebarMenuTexts[i].setTypeface(null, Typeface.BOLD);
                    } else {
                        sidebarMenuItems[i].setBackgroundResource(R.drawable.bg_sidebar_item_normal);
                        sidebarMenuTexts[i].setTextColor(Color.parseColor("#cbd5e1"));
                        sidebarMenuTexts[i].setTypeface(null, Typeface.NORMAL);
                    }
                }
            }
        }

        if (tvToolbarSubtitle != null && index >= 0 && index < MODULE_TITLES.length) {
            tvToolbarSubtitle.setText(MODULE_TITLES[index]);
        }

        containerBody.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        switch (index) {
            case 0:
                renderCardsModule(inflater);
                break;
            case 1:
                renderMovementsModule(inflater);
                break;
            case 2:
                renderSetAsidesModule(inflater);
                break;
            case 3:
                renderCalendarModule(inflater);
                break;
            case 4:
                renderDebtorsModule(inflater);
                break;
            case 5:
                renderBudgetsModule(inflater);
                break;
            case 6:
                renderSettingsModule(inflater);
                break;
        }
    }

    // =========================================================================
    // 1. MÓDULO: TARJETAS
    // =========================================================================

    private void renderCardsModule(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_module_cards, containerBody, false);
        containerBody.addView(view);

        TextView tvLimit = view.findViewById(R.id.tv_cards_global_limit);
        TextView tvSpent = view.findViewById(R.id.tv_cards_global_spent);
        TextView tvAvail = view.findViewById(R.id.tv_cards_global_available);
        TextView tvUtil = view.findViewById(R.id.tv_cards_global_utilization);
        TextView tvEmpty = view.findViewById(R.id.tv_cards_empty);
        LinearLayout listContainer = view.findViewById(R.id.layout_cards_list);
        Button btnAddCard = view.findViewById(R.id.btn_add_card);

        btnAddCard.setOnClickListener(v -> showCardDialog(null));

        List<Card> cards = dbHelper.getAllCards();

        Button btnReorder = view.findViewById(R.id.btn_reorder_cards);
        if (btnReorder != null) {
            btnReorder.setVisibility(cards.size() > 1 ? View.VISIBLE : View.GONE);
            btnReorder.setOnClickListener(v -> showReorderCardsDialog(cards));
        }

        double totalLimit = 0;
        double totalSpent = 0;
        double totalAvailable = 0;

        listContainer.removeAllViews();
        if (cards.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            for (int i = 0; i < cards.size(); i++) {
                final int cardIndex = i;
                Card c = cards.get(i);
                totalLimit += c.getCreditLimit();
                totalSpent += c.getTotalOwed();
                totalAvailable += c.getAvailable();

                View item = inflater.inflate(R.layout.item_card, listContainer, false);

                View colorDot = item.findViewById(R.id.view_card_color_dot);
                TextView tvName = item.findViewById(R.id.tv_item_card_name);
                TextView tvCutoff = item.findViewById(R.id.tv_item_card_cutoff);
                TextView tvCardLimit = item.findViewById(R.id.tv_item_card_limit);
                TextView tvCardSpentPeriod = item.findViewById(R.id.tv_item_card_spent_period);
                TextView tvCardSpentMsi = item.findViewById(R.id.tv_item_card_spent_msi);
                TextView tvCardTotalOwed = item.findViewById(R.id.tv_item_card_total_owed);
                TextView tvCardAvail = item.findViewById(R.id.tv_item_card_available);
                TextView tvCardUtil = item.findViewById(R.id.tv_item_card_utilization);
                TextView tvSetAside = item.findViewById(R.id.tv_item_card_set_aside);
                TextView tvMissing = item.findViewById(R.id.tv_item_card_missing);
                LinearLayout layoutDebtorsSec = item.findViewById(R.id.layout_item_card_debtors_section);
                LinearLayout layoutDebtorsList = item.findViewById(R.id.layout_item_card_debtors_list);
                Button btnMove = item.findViewById(R.id.btn_item_card_move);
                Button btnMsi = item.findViewById(R.id.btn_item_card_msi);
                Button btnEdit = item.findViewById(R.id.btn_item_card_edit);
                Button btnDelete = item.findViewById(R.id.btn_item_card_delete);

                if (c.getColor() != null && !c.getColor().isEmpty() && colorDot != null) {
                    try {
                        android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
                        dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                        dot.setColor(Color.parseColor(c.getColor()));
                        colorDot.setBackground(dot);
                    } catch (Exception ignored) {}
                }

                tvName.setText(c.getName());
                tvCutoff.setText("✂️ Corte: Día " + c.getCutoffDay());
                tvCardLimit.setText(formatCurrency(c.getCreditLimit()));
                tvCardSpentPeriod.setText(formatCurrency(c.getSpentPeriod()));
                tvCardSpentMsi.setText(formatCurrency(c.getSpentMsi()));
                tvCardTotalOwed.setText(formatCurrency(c.getTotalOwed()));
                tvCardAvail.setText(formatCurrency(c.getAvailable()));

                // Cuánto dinero falta por apartar en la tarjeta
                if (tvSetAside != null) {
                    tvSetAside.setText(formatCurrency(c.getTotalSetAside()));
                }
                if (tvMissing != null) {
                    if (c.getMissingToSetAside() > 0.001) {
                        tvMissing.setText(formatCurrency(c.getMissingToSetAside()));
                        tvMissing.setTextColor(Color.parseColor("#f87171"));
                    } else {
                        tvMissing.setText("100% Apartado ✅");
                        tvMissing.setTextColor(Color.parseColor("#4ade80"));
                    }
                }

                // Deudores de cada tarjeta y el monto restante de cada deudor
                if (layoutDebtorsSec != null && layoutDebtorsList != null) {
                    List<Card.DebtorSummary> debtors = c.getPendingDebtors();
                    if (debtors != null && !debtors.isEmpty()) {
                        layoutDebtorsSec.setVisibility(View.VISIBLE);
                        layoutDebtorsList.removeAllViews();
                        for (Card.DebtorSummary ds : debtors) {
                            TextView tvD = new TextView(this);
                            tvD.setText("• " + ds.getPersonName() + ": Falta apartar " + formatCurrency(ds.getRemainingAmount()));
                            tvD.setTextColor(Color.parseColor("#cbd5e1"));
                            tvD.setTextSize(12);
                            tvD.setPadding(0, 4, 0, 4);
                            layoutDebtorsList.addView(tvD);
                        }
                    } else {
                        layoutDebtorsSec.setVisibility(View.GONE);
                    }
                }

                // Botón interactivo para mover de posición rápidamente
                if (btnMove != null) {
                    if (cards.size() <= 1) {
                        btnMove.setVisibility(View.GONE);
                    } else {
                        btnMove.setVisibility(View.VISIBLE);
                        btnMove.setOnClickListener(v -> showMoveCardDialog(c, cardIndex, cards));
                    }
                }

                // Selección táctil mediante pulsación prolongada en cualquier punto de la tarjeta
                item.setOnLongClickListener(v -> {
                    if (cards.size() > 1) {
                        showMoveCardDialog(c, cardIndex, cards);
                        return true;
                    }
                    return false;
                });

                double util = c.getUtilizationPercent();
                tvCardUtil.setBackground(null);
                if (util < 30) {
                    tvCardUtil.setText(String.format(Locale.US, "%.1f%% Saludable", util));
                    tvCardUtil.setTextColor(Color.parseColor("#4ade80"));
                } else if (util < 70) {
                    tvCardUtil.setText(String.format(Locale.US, "%.1f%% Moderado", util));
                    tvCardUtil.setTextColor(Color.parseColor("#facc15"));
                } else {
                    tvCardUtil.setText(String.format(Locale.US, "%.1f%% Riesgo", util));
                    tvCardUtil.setTextColor(Color.parseColor("#f87171"));
                }

                btnMsi.setOnClickListener(v -> showCardMsiDialog(c));
                btnEdit.setOnClickListener(v -> showCardDialog(c));
                btnDelete.setOnClickListener(v -> showDeleteCardConfirmation(c));

                listContainer.addView(item);
            }
        }

        tvLimit.setText(formatCurrency(totalLimit));
        tvSpent.setText(formatCurrency(totalSpent));
        tvAvail.setText(formatCurrency(totalAvailable));

        double globalUtil = totalLimit > 0 ? (totalSpent / totalLimit * 100) : 0;
        tvUtil.setBackground(null);
        if (globalUtil < 30) {
            tvUtil.setText(String.format(Locale.US, "%.1f%% Saludable", globalUtil));
            tvUtil.setTextColor(Color.parseColor("#4ade80"));
        } else if (globalUtil < 70) {
            tvUtil.setText(String.format(Locale.US, "%.1f%% Moderado", globalUtil));
            tvUtil.setTextColor(Color.parseColor("#facc15"));
        } else {
            tvUtil.setText(String.format(Locale.US, "%.1f%% Riesgo", globalUtil));
            tvUtil.setTextColor(Color.parseColor("#f87171"));
        }
    }

    private void showMoveCardDialog(Card card, int currentIndex, List<Card> cards) {
        if (cards == null || cards.size() <= 1) return;

        String[] options = new String[cards.size()];
        for (int i = 0; i < cards.size(); i++) {
            if (i == currentIndex) {
                options[i] = (i + 1) + "ª posición (" + cards.get(i).getName() + ") ★ Posición Actual";
            } else if (i == 0) {
                options[i] = "1ª posición (Inicio - antes de " + cards.get(0).getName() + ")";
            } else if (i == cards.size() - 1) {
                options[i] = (i + 1) + "ª posición (Al final)";
            } else {
                options[i] = (i + 1) + "ª posición (en lugar de " + cards.get(i).getName() + ")";
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("⇅ Mover '" + card.getName() + "'")
                .setItems(options, (dialog, which) -> {
                    if (which != currentIndex) {
                        cards.remove(currentIndex);
                        cards.add(which, card);
                        dbHelper.updateCardOrder(cards);
                        Toast.makeText(this, "Tarjeta movida a la posición " + (which + 1), Toast.LENGTH_SHORT).show();
                        selectTab(0);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showReorderCardsDialog(List<Card> cards) {
        if (cards == null || cards.size() <= 1) return;

        String[] cardNames = new String[cards.size()];
        for (int i = 0; i < cards.size(); i++) {
            cardNames[i] = (i + 1) + ". " + cards.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("⇅ Selecciona tarjeta para mover de lugar")
                .setItems(cardNames, (dialog, which) -> {
                    Card selected = cards.get(which);
                    showMoveCardDialog(selected, which, cards);
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void showCardDialog(Card existingCard) {
        View dView = LayoutInflater.from(this).inflate(R.layout.dialog_card, null);
        TextView tvTitle = dView.findViewById(R.id.tv_dialog_card_title);
        EditText etName = dView.findViewById(R.id.et_card_name);
        EditText etLimit = dView.findViewById(R.id.et_card_limit);
        EditText etCutoff = dView.findViewById(R.id.et_card_cutoff);

        androidx.cardview.widget.CardView cardSample = dView.findViewById(R.id.card_color_sample_card);
        TextView tvSampleName = dView.findViewById(R.id.tv_card_sample_name);
        TextView tvHexBadge = dView.findViewById(R.id.tv_card_color_hex);
        TextView tvColorHint = dView.findViewById(R.id.tv_card_color_name_hint);
        TextView tvHueDegree = dView.findViewById(R.id.tv_hue_degree);
        SeekBar seekHue = dView.findViewById(R.id.seek_card_color_hue);
        TextView tvSatPercent = dView.findViewById(R.id.tv_sat_percent);
        SeekBar seekSaturation = dView.findViewById(R.id.seek_card_color_saturation);
        TextView tvBrightPercent = dView.findViewById(R.id.tv_bright_percent);
        SeekBar seekBrightness = dView.findViewById(R.id.seek_card_color_brightness);
        EditText etHexInput = dView.findViewById(R.id.et_card_color_hex_input);

        // Estado HSV dinámico (Tono: 0-360, Saturación: 0-1, Brillo/Valor: 0-1)
        final float[] currentHsv = new float[]{204f, 0.85f, 0.80f}; // Default #0284c7
        final boolean[] isUpdatingProgrammatically = new boolean[]{false};

        if (existingCard != null && existingCard.getColor() != null && !existingCard.getColor().isEmpty()) {
            try {
                int parsed = Color.parseColor(existingCard.getColor());
                Color.colorToHSV(parsed, currentHsv);
            } catch (Exception ignored) {}
        }

        // Configuración de degradado arcoíris para el SeekBar de Tono
        if (seekHue != null) {
            GradientDrawable rainbowGrad = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{
                            Color.RED,
                            Color.YELLOW,
                            Color.GREEN,
                            Color.CYAN,
                            Color.BLUE,
                            Color.MAGENTA,
                            Color.RED
                    }
            );
            rainbowGrad.setCornerRadius(16);
            seekHue.setProgressDrawable(rainbowGrad);
        }

        // Actualización dinámica en vivo de color y vistas
        Runnable updateColorUI = () -> {
            int color = Color.HSVToColor(currentHsv);
            String hex = String.format("#%06X", (0xFFFFFF & color));

            if (tvHexBadge != null) tvHexBadge.setText(hex);
            if (cardSample != null) cardSample.setCardBackgroundColor(color);

            int hueVal = Math.round(currentHsv[0]);
            int satVal = Math.round(currentHsv[1] * 100);
            int brightVal = Math.round(currentHsv[2] * 100);

            if (tvHueDegree != null) tvHueDegree.setText(hueVal + "°");
            if (tvSatPercent != null) tvSatPercent.setText(satVal + "%");
            if (tvBrightPercent != null) tvBrightPercent.setText(brightVal + "%");

            if (tvColorHint != null) {
                if (currentHsv[1] < 0.15f) {
                    tvColorHint.setText("Estilo Metálico / Monocromático");
                } else if (currentHsv[2] < 0.35f) {
                    tvColorHint.setText("Estilo Dark / Black Edition");
                } else {
                    tvColorHint.setText("Estilo Vivo Personalizado");
                }
            }

            if (!isUpdatingProgrammatically[0] && etHexInput != null) {
                isUpdatingProgrammatically[0] = true;
                etHexInput.setText(hex);
                isUpdatingProgrammatically[0] = false;
            }

            // Actualizar degradados interactivos de saturación y brillo
            try {
                int desat = Color.HSVToColor(new float[]{currentHsv[0], 0.05f, currentHsv[2]});
                int sat = Color.HSVToColor(new float[]{currentHsv[0], 1.0f, currentHsv[2]});
                GradientDrawable satGrad = new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{desat, sat}
                );
                satGrad.setCornerRadius(16);
                if (seekSaturation != null) seekSaturation.setProgressDrawable(satGrad);

                int dark = Color.HSVToColor(new float[]{currentHsv[0], currentHsv[1], 0.15f});
                int light = Color.HSVToColor(new float[]{currentHsv[0], currentHsv[1], 1.0f});
                GradientDrawable brightGrad = new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{dark, light}
                );
                brightGrad.setCornerRadius(16);
                if (seekBrightness != null) seekBrightness.setProgressDrawable(brightGrad);
            } catch (Exception ignored) {}
        };

        // Asignar progreso inicial
        if (seekHue != null) seekHue.setProgress((int) Math.min(360, Math.max(0, currentHsv[0])));
        if (seekSaturation != null) seekSaturation.setProgress((int) Math.min(100, Math.max(0, currentHsv[1] * 100)));
        if (seekBrightness != null) seekBrightness.setProgress((int) Math.min(100, Math.max(0, currentHsv[2] * 100)));

        if (seekHue != null) {
            seekHue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        currentHsv[0] = (float) progress;
                        updateColorUI.run();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (seekSaturation != null) {
            seekSaturation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        currentHsv[1] = progress / 100f;
                        updateColorUI.run();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (seekBrightness != null) {
            seekBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        currentHsv[2] = Math.max(0.12f, progress / 100f);
                        updateColorUI.run();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (etHexInput != null) {
            etHexInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (isUpdatingProgrammatically[0]) return;
                    String hex = s.toString().trim();
                    if (!hex.startsWith("#")) hex = "#" + hex;
                    if (hex.matches("^#[0-9a-fA-F]{6}$")) {
                        try {
                            int parsed = Color.parseColor(hex);
                            Color.colorToHSV(parsed, currentHsv);
                            isUpdatingProgrammatically[0] = true;
                            if (seekHue != null) seekHue.setProgress((int) currentHsv[0]);
                            if (seekSaturation != null) seekSaturation.setProgress((int) (currentHsv[1] * 100));
                            if (seekBrightness != null) seekBrightness.setProgress((int) (currentHsv[2] * 100));
                            updateColorUI.run();
                            isUpdatingProgrammatically[0] = false;
                        } catch (Exception ignored) {}
                    }
                }
            });
        }

        if (etName != null && tvSampleName != null) {
            etName.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    String n = s.toString().trim();
                    tvSampleName.setText(n.isEmpty() ? "Tarjeta de Muestra" : n);
                }
            });
        }

        updateColorUI.run();

        if (existingCard != null) {
            tvTitle.setText("Editar Tarjeta");
            etName.setText(existingCard.getName());
            if (tvSampleName != null) tvSampleName.setText(existingCard.getName());
            etLimit.setText(String.valueOf(existingCard.getCreditLimit()));
            etCutoff.setText(String.valueOf(existingCard.getCutoffDay()));
        }

        new AlertDialog.Builder(this)
                .setView(dView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String limitStr = etLimit.getText().toString().trim();
                    String cutoffStr = etCutoff.getText().toString().trim();
                    String color = tvHexBadge != null ? tvHexBadge.getText().toString().trim() : "#0284c7";
                    if (!color.startsWith("#")) color = "#0284c7";

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Ingresa un nombre para la tarjeta", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double limit = 0;
                    try { limit = Double.parseDouble(limitStr); } catch (Exception ignored) {}

                    int cutoff = 1;
                    try { cutoff = Integer.parseInt(cutoffStr); } catch (Exception ignored) {}
                    if (cutoff < 1) cutoff = 1;
                    if (cutoff > 31) cutoff = 31;

                    if (existingCard == null) {
                        Card c = new Card(null, name, limit, cutoff, color, null, System.currentTimeMillis());
                        dbHelper.insertCard(c);
                    } else {
                        existingCard.setName(name);
                        existingCard.setCreditLimit(limit);
                        existingCard.setCutoffDay(cutoff);
                        existingCard.setColor(color);
                        dbHelper.updateCard(existingCard);
                    }
                    selectTab(0);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showCardMsiDialog(Card card) {
        View dView = LayoutInflater.from(this).inflate(R.layout.dialog_card_msi, null);
        TextView tvTitle = dView.findViewById(R.id.tv_dialog_msi_title);
        TextView tvEmpty = dView.findViewById(R.id.tv_msi_empty);
        LinearLayout listLayout = dView.findViewById(R.id.layout_dialog_msi_list);

        tvTitle.setText("MSI: " + card.getName());
        List<InstallmentPlan> plans = dbHelper.getInstallmentPlansByCard(card.getId());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dView)
                .setPositiveButton("Cerrar", null)
                .create();

        if (plans.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            for (InstallmentPlan p : plans) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setBackgroundResource(R.drawable.bg_card_secondary);
                row.setPadding(24, 20, 24, 20);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 16);
                row.setLayoutParams(lp);

                TextView tvConcept = new TextView(this);
                tvConcept.setText(p.getConcept() + " (" + formatCurrency(p.getTotalAmount()) + ")");
                tvConcept.setTextColor(Color.WHITE);
                tvConcept.setTextSize(14);
                row.addView(tvConcept);

                double monthly = p.getMonths() > 0 ? (p.getTotalAmount() / p.getMonths()) : 0;
                TextView tvProgress = new TextView(this);
                tvProgress.setText("Mensualidad: " + formatCurrency(monthly) + " • Pagados: " + p.getPaidMonths() + "/" + p.getMonths() + " meses");
                tvProgress.setTextColor(Color.parseColor("#94a3b8"));
                tvProgress.setTextSize(12);
                row.addView(tvProgress);

                LinearLayout actRow = new LinearLayout(this);
                actRow.setOrientation(LinearLayout.HORIZONTAL);
                actRow.setPadding(0, 10, 0, 0);

                if (p.getPaidMonths() < p.getMonths()) {
                    Button btnAdvance = new Button(this);
                    btnAdvance.setText("➕ Abonar 1 Mes");
                    btnAdvance.setTextColor(Color.parseColor("#38bdf8"));
                    btnAdvance.setTextSize(11);
                    btnAdvance.setBackgroundResource(R.drawable.bg_card);
                    btnAdvance.setOnClickListener(v -> {
                        dbHelper.advanceInstallmentMonth(p.getId());
                        dialog.dismiss();
                        selectTab(0);
                    });
                    actRow.addView(btnAdvance);
                } else {
                    TextView tvDone = new TextView(this);
                    tvDone.setText("✅ Liquidado");
                    tvDone.setTextColor(Color.parseColor("#4ade80"));
                    tvDone.setTextSize(12);
                    actRow.addView(tvDone);
                }

                Button btnDel = new Button(this);
                btnDel.setText("🗑️ Eliminar Plan");
                btnDel.setTextColor(Color.parseColor("#ef4444"));
                btnDel.setTextSize(11);
                btnDel.setBackgroundResource(R.drawable.bg_card);
                btnDel.setOnClickListener(v -> {
                    dbHelper.deleteInstallmentPlan(p.getId());
                    dialog.dismiss();
                    selectTab(0);
                });
                actRow.addView(btnDel);

                row.addView(actRow);
                listLayout.addView(row);
            }
        }

        dialog.show();
    }

    private void showDeleteCardConfirmation(Card card) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Tarjeta")
                .setMessage("¿Estás seguro de eliminar la tarjeta '" + card.getName() + "'?\n\nSe eliminarán también todos sus movimientos, MSI y apartados asociados.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    dbHelper.deleteCard(card.getId());
                    selectTab(0);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =========================================================================
    // 2. MÓDULO: GASTOS (MOVIMIENTOS)
    // =========================================================================

    private void renderMovementsModule(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_module_movements, containerBody, false);
        containerBody.addView(view);

        Spinner spCard = view.findViewById(R.id.spinner_filter_card);
        Spinner spPerson = view.findViewById(R.id.spinner_filter_person);
        TextView tvTotal = view.findViewById(R.id.tv_movements_total);
        TextView tvEmpty = view.findViewById(R.id.tv_movements_empty);
        LinearLayout listContainer = view.findViewById(R.id.layout_movements_list);
        Button btnAdd = view.findViewById(R.id.btn_add_movement);

        btnAdd.setOnClickListener(v -> showMovementDialog());

        List<Card> cards = dbHelper.getAllCards();
        List<Person> people = dbHelper.getAllPeople();

        List<String> cardNames = new ArrayList<>();
        cardNames.add("Todas las tarjetas");
        final List<String> cardIds = new ArrayList<>();
        cardIds.add("");
        int selectedCardIndex = 0;
        for (int i = 0; i < cards.size(); i++) {
            cardNames.add(cards.get(i).getName());
            cardIds.add(cards.get(i).getId());
            if (cards.get(i).getId().equals(selectedFilterCardId)) {
                selectedCardIndex = i + 1;
            }
        }

        List<String> personNames = new ArrayList<>();
        personNames.add("Todas las personas");
        final List<String> personIds = new ArrayList<>();
        personIds.add("");
        int selectedPersonIndex = 0;
        for (int i = 0; i < people.size(); i++) {
            personNames.add(people.get(i).getName());
            personIds.add(people.get(i).getId());
            if (people.get(i).getId().equals(selectedFilterPersonId)) {
                selectedPersonIndex = i + 1;
            }
        }

        spCard.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cardNames));
        spPerson.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, personNames));

        spCard.setSelection(selectedCardIndex);
        spPerson.setSelection(selectedPersonIndex);

        spCard.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String newCardId = cardIds.get(position);
                if (!newCardId.equals(selectedFilterCardId)) {
                    selectedFilterCardId = newCardId;
                    reloadMovementsList(inflater, listContainer, tvTotal, tvEmpty);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spPerson.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String newPersonId = personIds.get(position);
                if (!newPersonId.equals(selectedFilterPersonId)) {
                    selectedFilterPersonId = newPersonId;
                    reloadMovementsList(inflater, listContainer, tvTotal, tvEmpty);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        reloadMovementsList(inflater, listContainer, tvTotal, tvEmpty);
    }

    private void reloadMovementsList(LayoutInflater inflater, LinearLayout listContainer, TextView tvTotal, TextView tvEmpty) {
        listContainer.removeAllViews();
        List<Movement> movs = dbHelper.getAllMovements(selectedFilterCardId, selectedFilterPersonId);

        double total = 0;
        if (movs.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            for (Movement m : movs) {
                total += m.getAmount();
                View item = inflater.inflate(R.layout.item_movement, listContainer, false);

                TextView tvConcept = item.findViewById(R.id.tv_item_mov_concept);
                TextView tvDate = item.findViewById(R.id.tv_item_mov_date);
                TextView tvCard = item.findViewById(R.id.tv_item_mov_card);
                TextView tvPerson = item.findViewById(R.id.tv_item_mov_person);
                TextView tvAmount = item.findViewById(R.id.tv_item_mov_amount);
                Button btnDelete = item.findViewById(R.id.btn_item_mov_delete);

                tvConcept.setText(m.getConcept());
                tvDate.setText(m.getDate());
                tvCard.setText(m.getCardName());
                tvPerson.setText(m.getPersonName());
                tvAmount.setText("-" + formatCurrency(m.getAmount()));

                btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Eliminar Gasto")
                            .setMessage("¿Eliminar '" + m.getConcept() + "' por " + formatCurrency(m.getAmount()) + "?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                dbHelper.deleteMovement(m.getId());
                                reloadMovementsList(inflater, listContainer, tvTotal, tvEmpty);
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });

                listContainer.addView(item);
            }
        }
        tvTotal.setText(formatCurrency(total));
    }

    private void showMovementDialog() {
        List<Card> cards = dbHelper.getAllCards();
        if (cards.isEmpty()) {
            Toast.makeText(this, "Primero registra al menos una tarjeta de crédito.", Toast.LENGTH_LONG).show();
            selectTab(0);
            return;
        }

        View dView = LayoutInflater.from(this).inflate(R.layout.dialog_movement, null);
        EditText etConcept = dView.findViewById(R.id.et_mov_concept);
        EditText etAmount = dView.findViewById(R.id.et_mov_amount);
        EditText etDate = dView.findViewById(R.id.et_mov_date);
        Spinner spCard = dView.findViewById(R.id.spinner_mov_card);
        Spinner spPerson = dView.findViewById(R.id.spinner_mov_person);
        CheckBox cbMsi = dView.findViewById(R.id.cb_mov_msi);
        LinearLayout layoutMsi = dView.findViewById(R.id.layout_mov_msi_months);
        Spinner spMonths = dView.findViewById(R.id.spinner_mov_msi_months);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        etDate.setText(sdf.format(new Date()));

        List<String> cardNames = new ArrayList<>();
        for (Card c : cards) cardNames.add(c.getName());
        spCard.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cardNames));

        List<Person> people = dbHelper.getAllPeople();
        List<String> personNames = new ArrayList<>();
        for (Person p : people) personNames.add(p.getName());
        spPerson.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, personNames));

        String[] msiOptions = {"3 meses", "6 meses", "9 meses", "12 meses", "18 meses", "24 meses"};
        int[] msiVals = {3, 6, 9, 12, 18, 24};
        spMonths.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, msiOptions));

        cbMsi.setOnCheckedChangeListener((btn, isChecked) -> {
            layoutMsi.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        new AlertDialog.Builder(this)
                .setView(dView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String concept = etConcept.getText().toString().trim();
                    String amtStr = etAmount.getText().toString().trim();
                    String date = etDate.getText().toString().trim();
                    int cardIdx = spCard.getSelectedItemPosition();
                    int personIdx = spPerson.getSelectedItemPosition();

                    if (concept.isEmpty()) {
                        Toast.makeText(this, "Ingresa el concepto del gasto.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amount = 0;
                    try { amount = Double.parseDouble(amtStr); } catch (Exception ignored) {}
                    if (amount <= 0) {
                        Toast.makeText(this, "Ingresa un monto válido mayor a 0.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String cardId = cards.get(cardIdx).getId();
                    String personId = people.get(personIdx).getId();

                    if (cbMsi.isChecked()) {
                        int months = msiVals[spMonths.getSelectedItemPosition()];
                        InstallmentPlan plan = new InstallmentPlan(null, concept, amount, months, date, cardId, personId, 0, System.currentTimeMillis());
                        dbHelper.insertInstallmentPlan(plan);
                        Toast.makeText(this, "Compra diferida a " + months + " MSI registrada.", Toast.LENGTH_SHORT).show();
                    } else {
                        Movement mov = new Movement(null, concept, amount, date, cardId, personId, 0, System.currentTimeMillis());
                        dbHelper.insertMovement(mov);
                        Toast.makeText(this, "Gasto registrado exitosamente.", Toast.LENGTH_SHORT).show();
                    }

                    selectTab(1);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =========================================================================
    // 3. MÓDULO: APARTADOS (FONDOS & TRASPASOS)
    // =========================================================================

    private void renderSetAsidesModule(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_module_set_asides, containerBody, false);
        containerBody.addView(view);

        TextView tvCash = view.findViewById(R.id.tv_sa_total_cash);
        TextView tvDebit = view.findViewById(R.id.tv_sa_total_debit);
        TextView tvAll = view.findViewById(R.id.tv_sa_total_all);
        TextView tvEmpty = view.findViewById(R.id.tv_sa_empty);
        LinearLayout listContainer = view.findViewById(R.id.layout_sa_list);
        Button btnAdd = view.findViewById(R.id.btn_add_set_aside);
        Button btnPayBank = view.findViewById(R.id.btn_pay_to_bank);

        btnAdd.setOnClickListener(v -> showSetAsideDialog());
        btnPayBank.setOnClickListener(v -> showPayToBankDialog());

        List<SetAside> list = dbHelper.getAllSetAsides();

        double totalCash = 0;
        double totalDebit = 0;

        listContainer.removeAllViews();
        if (list.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            for (SetAside sa : list) {
                if (sa.getIsPaid() == 0) {
                    if ("Débito".equalsIgnoreCase(sa.getFundType())) {
                        totalDebit += sa.getAmount();
                    } else {
                        totalCash += sa.getAmount();
                    }
                }

                View item = inflater.inflate(R.layout.item_set_aside, listContainer, false);

                TextView tvNote = item.findViewById(R.id.tv_item_sa_note);
                TextView tvAmount = item.findViewById(R.id.tv_item_sa_amount);
                TextView tvDate = item.findViewById(R.id.tv_item_sa_date);
                TextView tvCard = item.findViewById(R.id.tv_item_sa_card);
                TextView tvPerson = item.findViewById(R.id.tv_item_sa_person);
                Button btnToggle = item.findViewById(R.id.btn_item_sa_toggle_fund);
                TextView tvStatus = item.findViewById(R.id.tv_item_sa_status);
                Button btnDelete = item.findViewById(R.id.btn_item_sa_delete);

                tvNote.setText(sa.getNote());
                tvAmount.setText(formatCurrency(sa.getAmount()));
                tvDate.setText(sa.getDate());
                tvCard.setText(sa.getCardName());
                tvPerson.setText(sa.getPersonName());

                String fundLabel = "Débito".equalsIgnoreCase(sa.getFundType()) ? "💳 Débito (Cambiar)" : "💵 Efectivo (Cambiar)";
                btnToggle.setText(fundLabel);
                btnToggle.setOnClickListener(v -> {
                    dbHelper.toggleFundType(sa.getId());
                    selectTab(2);
                });

                tvStatus.setBackground(null);
                if (sa.getIsPaid() == 1) {
                    tvStatus.setText("Pagado al Banco");
                    tvStatus.setTextColor(Color.parseColor("#4ade80"));
                } else {
                    tvStatus.setText("Apartado Activo");
                    tvStatus.setTextColor(Color.parseColor("#facc15"));
                }

                btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Eliminar Apartado")
                            .setMessage("¿Deseas eliminar este registro de fondos apartados?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                dbHelper.deleteSetAside(sa.getId());
                                selectTab(2);
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });

                listContainer.addView(item);
            }
        }

        tvCash.setText(formatCurrency(totalCash));
        tvDebit.setText(formatCurrency(totalDebit));
        tvAll.setText(formatCurrency(totalCash + totalDebit));
    }

    private void showSetAsideDialog() {
        List<Card> cards = dbHelper.getAllCards();
        if (cards.isEmpty()) {
            Toast.makeText(this, "Registra al menos una tarjeta primero.", Toast.LENGTH_SHORT).show();
            selectTab(0);
            return;
        }

        View dView = LayoutInflater.from(this).inflate(R.layout.dialog_set_aside, null);
        EditText etNote = dView.findViewById(R.id.et_sa_note);
        EditText etAmount = dView.findViewById(R.id.et_sa_amount);
        EditText etDate = dView.findViewById(R.id.et_sa_date);
        Spinner spCard = dView.findViewById(R.id.spinner_sa_card);
        Spinner spPerson = dView.findViewById(R.id.spinner_sa_person);
        Spinner spFundType = dView.findViewById(R.id.spinner_sa_fund_type);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        etDate.setText(sdf.format(new Date()));

        List<String> cardNames = new ArrayList<>();
        for (Card c : cards) cardNames.add(c.getName());
        spCard.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cardNames));

        List<Person> people = dbHelper.getAllPeople();
        List<String> personNames = new ArrayList<>();
        for (Person p : people) personNames.add(p.getName());
        spPerson.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, personNames));

        String[] fundTypes = {"Efectivo", "Débito"};
        spFundType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fundTypes));

        Spinner spMov = dView.findViewById(R.id.spinner_sa_movement);
        List<Movement> allPendingMovs = dbHelper.getAllPendingMovements();
        List<String> movLabels = new ArrayList<>();
        movLabels.add("-- Apartado General (distribución automática) --");
        for (Movement m : allPendingMovs) {
            movLabels.add(m.getConcept() + " (" + formatCurrency(m.getPendingAmount()) + " pend. • " + m.getCardName() + " - " + m.getPersonName() + ")");
        }
        spMov.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, movLabels));

        TextView tvMovInfo = dView.findViewById(R.id.tv_sa_movement_info);

        spMov.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position - 1 < allPendingMovs.size()) {
                    Movement m = allPendingMovs.get(position - 1);
                    etNote.setText("Apartado: " + m.getConcept());
                    etAmount.setText(String.format(Locale.US, "%.2f", m.getPendingAmount()));
                    for (int i = 0; i < cards.size(); i++) {
                        if (cards.get(i).getId().equals(m.getCardId())) {
                            spCard.setSelection(i);
                            break;
                        }
                    }
                    for (int i = 0; i < people.size(); i++) {
                        if (people.get(i).getId().equals(m.getPersonId())) {
                            spPerson.setSelection(i);
                            break;
                        }
                    }
                    if (tvMovInfo != null) {
                        tvMovInfo.setVisibility(View.VISIBLE);
                        tvMovInfo.setText("Gasto: " + m.getConcept() + " (" + formatCurrency(m.getAmount()) + ")" +
                                " • Ya cubierto: " + formatCurrency(m.getTotalSetAside()) +
                                " • Falta: " + formatCurrency(m.getPendingAmount()));
                    }
                } else {
                    if (tvMovInfo != null) {
                        tvMovInfo.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        new AlertDialog.Builder(this)
                .setView(dView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String note = etNote.getText().toString().trim();
                    String amtStr = etAmount.getText().toString().trim();
                    String date = etDate.getText().toString().trim();
                    int cardIdx = spCard.getSelectedItemPosition();
                    int personIdx = spPerson.getSelectedItemPosition();
                    String fundType = fundTypes[spFundType.getSelectedItemPosition()];

                    if (note.isEmpty()) note = "Apartado";
                    double amount = 0;
                    try { amount = Double.parseDouble(amtStr); } catch (Exception ignored) {}
                    if (amount <= 0) {
                        Toast.makeText(this, "Ingresa un monto válido.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int selectedMovPos = spMov.getSelectedItemPosition();
                    String movementId = (selectedMovPos > 0 && selectedMovPos - 1 < allPendingMovs.size())
                            ? allPendingMovs.get(selectedMovPos - 1).getId() : null;

                    String cardId = cards.get(cardIdx).getId();
                    String personId = people.get(personIdx).getId();

                    SetAside sa = new SetAside(null, cardId, personId, movementId, amount, note, date, fundType, 0, 0, "apartado", System.currentTimeMillis());
                    dbHelper.insertSetAside(sa);
                    Toast.makeText(this, "Fondos apartados correctamente.", Toast.LENGTH_SHORT).show();
                    selectTab(2);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showPayToBankDialog() {
        List<Card> cards = dbHelper.getAllCards();
        if (cards.isEmpty()) {
            Toast.makeText(this, "No tienes tarjetas registradas.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dView = LayoutInflater.from(this).inflate(R.layout.dialog_pay_bank, null);
        Spinner spCard = dView.findViewById(R.id.spinner_pay_bank_card);
        CheckBox cbFull = dView.findViewById(R.id.cb_pay_full_bank);
        LinearLayout layoutCustom = dView.findViewById(R.id.layout_pay_custom_amount);
        EditText etAmount = dView.findViewById(R.id.et_pay_custom_amount);

        List<String> cardNames = new ArrayList<>();
        for (Card c : cards) cardNames.add(c.getName());
        spCard.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cardNames));

        cbFull.setOnCheckedChangeListener((btn, isChecked) -> {
            layoutCustom.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });

        new AlertDialog.Builder(this)
                .setView(dView)
                .setPositiveButton("Aplicar Pago al Banco", (dialog, which) -> {
                    int cardIdx = spCard.getSelectedItemPosition();
                    String cardId = cards.get(cardIdx).getId();
                    boolean isFull = cbFull.isChecked();
                    double amount = 0;
                    if (!isFull) {
                        try { amount = Double.parseDouble(etAmount.getText().toString().trim()); } catch (Exception ignored) {}
                    }

                    dbHelper.payToBank(cardId, amount, isFull);
                    Toast.makeText(this, "¡Pago aplicado al banco exitosamente!", Toast.LENGTH_SHORT).show();
                    selectTab(2);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =========================================================================
    // 4. MÓDULO: CALENDARIO
    // =========================================================================

    private void renderCalendarModule(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_module_calendar, containerBody, false);
        containerBody.addView(view);

        TextView tvEmpty = view.findViewById(R.id.tv_cal_empty);
        LinearLayout listContainer = view.findViewById(R.id.layout_cal_list);

        List<Card> cards = dbHelper.getAllCards();
        listContainer.removeAllViews();

        if (cards.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);

            Calendar cal = Calendar.getInstance();
            int curDay = cal.get(Calendar.DAY_OF_MONTH);
            int curMonth = cal.get(Calendar.MONTH);
            int curYear = cal.get(Calendar.YEAR);

            SimpleDateFormat dateFmt = new SimpleDateFormat("dd 'de' MMMM", new Locale("es", "ES"));

            for (Card c : cards) {
                View item = inflater.inflate(R.layout.item_calendar_card, listContainer, false);

                TextView tvCardName = item.findViewById(R.id.tv_cal_card_name);
                TextView tvBadge = item.findViewById(R.id.tv_cal_card_badge);
                TextView tvCutoffDate = item.findViewById(R.id.tv_cal_cutoff_date);
                TextView tvDueDate = item.findViewById(R.id.tv_cal_due_date);
                TextView tvTotalOwed = item.findViewById(R.id.tv_cal_total_owed);

                tvCardName.setText(c.getName());
                tvTotalOwed.setText(formatCurrency(c.getTotalOwed()));

                Calendar cutoffCal = Calendar.getInstance();
                cutoffCal.set(Calendar.DAY_OF_MONTH, Math.min(c.getCutoffDay(), cutoffCal.getActualMaximum(Calendar.DAY_OF_MONTH)));

                if (curDay > c.getCutoffDay()) {
                    cutoffCal.add(Calendar.MONTH, 1);
                }

                Calendar dueCal = (Calendar) cutoffCal.clone();
                dueCal.add(Calendar.DAY_OF_MONTH, 20);

                tvCutoffDate.setText(dateFmt.format(cutoffCal.getTime()));
                tvDueDate.setText(dateFmt.format(dueCal.getTime()));

                long diffMillis = cutoffCal.getTimeInMillis() - cal.getTimeInMillis();
                long daysUntilCutoff = diffMillis / (24 * 60 * 60 * 1000);

                tvBadge.setBackground(null);
                if (daysUntilCutoff <= 3 && daysUntilCutoff >= 0) {
                    tvBadge.setText("⚠️ Corte en " + daysUntilCutoff + " días");
                    tvBadge.setTextColor(Color.parseColor("#facc15"));
                } else if (daysUntilCutoff < 0) {
                    tvBadge.setText("💰 En Periodo de Gracia");
                    tvBadge.setTextColor(Color.parseColor("#4ade80"));
                } else {
                    tvBadge.setText("Corte en " + daysUntilCutoff + " días");
                    tvBadge.setTextColor(Color.parseColor("#38bdf8"));
                }

                listContainer.addView(item);
            }
        }
    }

    // =========================================================================
    // 5. MÓDULO: DEUDORES
    // =========================================================================

    private void renderDebtorsModule(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_module_debtors, containerBody, false);
        containerBody.addView(view);

        Button btnAdd = view.findViewById(R.id.btn_add_person);
        LinearLayout listContainer = view.findViewById(R.id.layout_people_list);

        btnAdd.setOnClickListener(v -> showPersonDialog());

        List<Person> people = dbHelper.getAllPeople();
        listContainer.removeAllViews();

        for (Person p : people) {
            View item = inflater.inflate(R.layout.item_person, listContainer, false);

            TextView tvName = item.findViewById(R.id.tv_item_person_name);
            TextView tvOwedBadge = item.findViewById(R.id.tv_item_person_owed_badge);
            TextView tvSpent = item.findViewById(R.id.tv_item_person_spent);
            TextView tvDelivered = item.findViewById(R.id.tv_item_person_delivered);
            Button btnFilter = item.findViewById(R.id.btn_item_person_filter);
            Button btnDelete = item.findViewById(R.id.btn_item_person_delete);

            tvName.setText(p.getName());
            tvSpent.setText(formatCurrency(p.getTotalSpent()));
            tvDelivered.setText(formatCurrency(p.getTotalDelivered()));

            double owed = p.getNetOwed();
            tvOwedBadge.setBackground(null);
            boolean isPersonal = p.getName() != null && p.getName().equalsIgnoreCase("Personal");
            if (owed > 0) {
                if (isPersonal) {
                    tvOwedBadge.setText("Falta apartar " + formatCurrency(owed));
                    tvOwedBadge.setTextColor(Color.parseColor("#f59e0b"));
                } else {
                    tvOwedBadge.setText("Debe " + formatCurrency(owed));
                    tvOwedBadge.setTextColor(Color.parseColor("#f87171"));
                }
            } else {
                if (isPersonal) {
                    tvOwedBadge.setText("100% Cubierto ✅");
                } else {
                    tvOwedBadge.setText("Al Corriente $0.00");
                }
                tvOwedBadge.setTextColor(Color.parseColor("#4ade80"));
            }

            // Gastos pendientes de apartar para esta persona
            LinearLayout layoutPendingSec = item.findViewById(R.id.layout_person_pending_movements_section);
            LinearLayout layoutPendingList = item.findViewById(R.id.layout_person_pending_movements_list);

            if (layoutPendingSec != null && layoutPendingList != null) {
                List<Movement> pendingMovs = dbHelper.getPendingMovementsForPerson(p.getId());
                if (!pendingMovs.isEmpty()) {
                    layoutPendingSec.setVisibility(View.VISIBLE);
                    layoutPendingList.removeAllViews();
                    for (Movement m : pendingMovs) {
                        View movView = inflater.inflate(R.layout.item_debtor_pending_movement, layoutPendingList, false);
                        TextView tvConcept = movView.findViewById(R.id.tv_pending_mov_concept);
                        TextView tvDetails = movView.findViewById(R.id.tv_pending_mov_details);
                        TextView tvAmounts = movView.findViewById(R.id.tv_pending_mov_amounts);
                        Button btnApartarMov = movView.findViewById(R.id.btn_pending_mov_apartar);

                        tvConcept.setText(m.getConcept());
                        tvDetails.setText((m.getCardName() != null ? m.getCardName() : "Sin Tarjeta") + " • " + m.getDate());
                        if (m.getTotalSetAside() > 0.001) {
                            tvAmounts.setText("Total: " + formatCurrency(m.getAmount()) + " • Apartado: " + formatCurrency(m.getTotalSetAside()) + " • Falta: " + formatCurrency(m.getPendingAmount()));
                        } else {
                            tvAmounts.setText("Total: " + formatCurrency(m.getAmount()) + " • Falta apartar: " + formatCurrency(m.getPendingAmount()));
                        }

                        btnApartarMov.setOnClickListener(v -> showApartarMovementDialog(m, p));
                        layoutPendingList.addView(movView);
                    }
                } else {
                    layoutPendingSec.setVisibility(View.GONE);
                }
            }

            btnFilter.setOnClickListener(v -> {
                selectedFilterPersonId = p.getId();
                selectTab(1);
            });

            if ("Personal".equalsIgnoreCase(p.getName())) {
                btnDelete.setVisibility(View.GONE);
            } else {
                btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Eliminar Persona")
                            .setMessage("¿Deseas eliminar a '" + p.getName() + "'?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                dbHelper.deletePerson(p.getId());
                                selectTab(4);
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });
            }

            listContainer.addView(item);
        }
    }

    private void showApartarMovementDialog(Movement m, Person p) {
        View dView = LayoutInflater.from(this).inflate(R.layout.dialog_set_aside, null);
        EditText etNote = dView.findViewById(R.id.et_sa_note);
        EditText etAmount = dView.findViewById(R.id.et_sa_amount);
        EditText etDate = dView.findViewById(R.id.et_sa_date);
        Spinner spCard = dView.findViewById(R.id.spinner_sa_card);
        Spinner spPerson = dView.findViewById(R.id.spinner_sa_person);
        Spinner spFundType = dView.findViewById(R.id.spinner_sa_fund_type);

        View tvMovLabel = dView.findViewById(R.id.tv_sa_movement_label);
        Spinner spMov = dView.findViewById(R.id.spinner_sa_movement);
        TextView tvMovInfo = dView.findViewById(R.id.tv_sa_movement_info);
        if (tvMovLabel != null) tvMovLabel.setVisibility(View.GONE);
        if (spMov != null) spMov.setVisibility(View.GONE);
        if (tvMovInfo != null) {
            tvMovInfo.setVisibility(View.VISIBLE);
            tvMovInfo.setText(String.format(Locale.US, "Gasto: %s (%s) • Ya cubierto: %s • Falta: %s",
                    m.getConcept(), formatCurrency(m.getAmount()), formatCurrency(m.getTotalSetAside()), formatCurrency(m.getPendingAmount())));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        etDate.setText(sdf.format(new Date()));
        etNote.setText("Apartado: " + m.getConcept());
        etAmount.setText(String.format(Locale.US, "%.2f", m.getPendingAmount()));

        List<Card> cards = dbHelper.getAllCards();
        List<String> cardNames = new ArrayList<>();
        int selectedCardIndex = 0;
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            cardNames.add(c.getName());
            if (c.getId().equals(m.getCardId())) {
                selectedCardIndex = i;
            }
        }
        spCard.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cardNames));
        spCard.setSelection(selectedCardIndex);

        List<Person> people = dbHelper.getAllPeople();
        List<String> personNames = new ArrayList<>();
        int selectedPersonIndex = 0;
        for (int i = 0; i < people.size(); i++) {
            Person person = people.get(i);
            personNames.add(person.getName());
            if (person.getId().equals(p.getId())) {
                selectedPersonIndex = i;
            }
        }
        spPerson.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, personNames));
        spPerson.setSelection(selectedPersonIndex);

        String[] fundTypes = {"Efectivo", "Débito"};
        spFundType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fundTypes));

        new AlertDialog.Builder(this)
                .setTitle("💰 Apartar Gasto (" + p.getName() + ")")
                .setView(dView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String note = etNote.getText().toString().trim();
                    String amtStr = etAmount.getText().toString().trim();
                    String date = etDate.getText().toString().trim();
                    int cardIdx = spCard.getSelectedItemPosition();
                    int personIdx = spPerson.getSelectedItemPosition();
                    String fundType = fundTypes[spFundType.getSelectedItemPosition()];

                    if (note.isEmpty()) note = "Apartado: " + m.getConcept();
                    double amount = 0;
                    try { amount = Double.parseDouble(amtStr); } catch (Exception ignored) {}
                    if (amount <= 0) {
                        Toast.makeText(this, "Ingresa un monto válido.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String cardId = (cards.size() > cardIdx && cardIdx >= 0) ? cards.get(cardIdx).getId() : m.getCardId();
                    String personId = (m.getPersonId() != null && !m.getPersonId().isEmpty()) ? m.getPersonId() : ((people.size() > personIdx && personIdx >= 0) ? people.get(personIdx).getId() : p.getId());

                    SetAside sa = new SetAside(null, cardId, personId, m.getId(), amount, note, date, fundType, 0, 0, "apartado", System.currentTimeMillis());
                    dbHelper.insertSetAside(sa);

                    if (m.getTotalSetAside() + amount >= m.getAmount()) {
                        dbHelper.updateMovementSetAsideStatus(m.getId(), 1);
                    }

                    Toast.makeText(this, "Fondos apartados correctamente.", Toast.LENGTH_SHORT).show();
                    selectTab(4);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showPersonDialog() {
        View dView = LayoutInflater.from(this).inflate(R.layout.dialog_person, null);
        EditText etName = dView.findViewById(R.id.et_person_name);

        new AlertDialog.Builder(this)
                .setView(dView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Ingresa un nombre.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Person p = new Person(null, name, System.currentTimeMillis());
                    dbHelper.insertPerson(p);
                    Toast.makeText(this, "Persona agregada correctamente.", Toast.LENGTH_SHORT).show();
                    selectTab(4);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =========================================================================
    // 6. MÓDULO: PRESUPUESTOS & INFLACIÓN
    // =========================================================================

    private void renderBudgetsModule(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_module_budgets, containerBody, false);
        containerBody.addView(view);

        Button btnInflation = view.findViewById(R.id.btn_change_inflation);
        TextView tvCurrent = view.findViewById(R.id.tv_budget_total_current);
        TextView tvProjected = view.findViewById(R.id.tv_budget_total_projected);
        TextView tvEmpty = view.findViewById(R.id.tv_budget_empty);
        LinearLayout listContainer = view.findViewById(R.id.layout_budgets_list);
        Button btnAdd = view.findViewById(R.id.btn_add_budget);

        double inflationRate = dbHelper.getInflationRate();
        btnInflation.setText(String.format(Locale.US, "⚙️ Tasa: %.1f%%", inflationRate));

        btnInflation.setOnClickListener(v -> showInflationDialog(inflationRate));
        btnAdd.setOnClickListener(v -> showBudgetDialog());

        List<Budget> budgets = dbHelper.getAllBudgets();
        listContainer.removeAllViews();

        double totalAllCurrent = 0;

        if (budgets.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            for (Budget b : budgets) {
                View item = inflater.inflate(R.layout.item_budget, listContainer, false);

                TextView tvName = item.findViewById(R.id.tv_item_budget_name);
                TextView tvDesc = item.findViewById(R.id.tv_item_budget_desc);
                TextView tvTotal = item.findViewById(R.id.tv_item_budget_total);
                TextView tvFixedVar = item.findViewById(R.id.tv_item_budget_fixed_var);
                TextView tvProj = item.findViewById(R.id.tv_item_budget_projected);
                LinearLayout conceptsContainer = item.findViewById(R.id.layout_item_budget_concepts);
                Button btnAddItem = item.findViewById(R.id.btn_item_budget_add_item);
                Button btnDelete = item.findViewById(R.id.btn_item_budget_delete);

                tvName.setText(b.getName());
                tvDesc.setText(b.getDescription() != null && !b.getDescription().isEmpty() ? b.getDescription() : "Sin descripción");

                double fixed = 0;
                double variable = 0;

                conceptsContainer.removeAllViews();
                if (b.getItems() != null) {
                    for (BudgetItem bi : b.getItems()) {
                        if ("Fijo".equalsIgnoreCase(bi.getType())) fixed += bi.getAmount();
                        else variable += bi.getAmount();

                        View cView = inflater.inflate(R.layout.item_budget_concept, conceptsContainer, false);
                        TextView tvCConcept = cView.findViewById(R.id.tv_bi_item_concept);
                        TextView tvCTag = cView.findViewById(R.id.tv_bi_item_tag);
                        TextView tvCAmount = cView.findViewById(R.id.tv_bi_item_amount);
                        TextView btnCDelete = cView.findViewById(R.id.btn_bi_item_delete);

                        tvCConcept.setText(bi.getConcept());
                        tvCTag.setText(bi.getType() + (bi.getTag() != null && !bi.getTag().isEmpty() ? " • " + bi.getTag() : ""));
                        tvCAmount.setText(formatCurrency(bi.getAmount()));

                        btnCDelete.setOnClickListener(v -> {
                            dbHelper.deleteBudgetItem(bi.getId());
                            selectTab(5);
                        });

                        conceptsContainer.addView(cView);
                    }
                }

                double bTotal = fixed + variable;
                totalAllCurrent += bTotal;
                double bProjected = bTotal * (1 + (inflationRate / 100.0));

                tvTotal.setText(formatCurrency(bTotal));
                tvFixedVar.setText("Fijos: " + formatCurrency(fixed) + " • Variables: " + formatCurrency(variable));
                tvProj.setText("Proy: " + formatCurrency(bProjected));

                btnAddItem.setOnClickListener(v -> showBudgetItemDialog(b.getId()));
                btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Eliminar Presupuesto")
                            .setMessage("¿Deseas eliminar el presupuesto '" + b.getName() + "'?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                dbHelper.deleteBudget(b.getId());
                                selectTab(5);
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });

                listContainer.addView(item);
            }
        }

        tvCurrent.setText(formatCurrency(totalAllCurrent));
        double totalAllProjected = totalAllCurrent * (1 + (inflationRate / 100.0));
        tvProjected.setText(formatCurrency(totalAllProjected));
    }

    private void showInflationDialog(double currentRate) {
        EditText et = new EditText(this);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setText(String.valueOf(currentRate));
        et.setTextColor(Color.WHITE);
        et.setBackgroundResource(R.drawable.bg_input_field);
        et.setPadding(32, 24, 32, 24);

        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setPadding(40, 20, 40, 10);
        wrapper.addView(et);

        new AlertDialog.Builder(this)
                .setTitle("Ajustar Tasa de Inflación (%)")
                .setView(wrapper)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    try {
                        double newRate = Double.parseDouble(et.getText().toString().trim());
                        dbHelper.setInflationRate(newRate);
                        selectTab(5);
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showBudgetDialog() {
        View dView = LayoutInflater.from(this).inflate(R.layout.dialog_budget, null);
        EditText etName = dView.findViewById(R.id.et_budget_name);
        EditText etDesc = dView.findViewById(R.id.et_budget_desc);

        new AlertDialog.Builder(this)
                .setView(dView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Ingresa un nombre.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Budget b = new Budget(null, name, desc, System.currentTimeMillis());
                    dbHelper.insertBudget(b);
                    selectTab(5);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showBudgetItemDialog(String budgetId) {
        View dView = LayoutInflater.from(this).inflate(R.layout.dialog_budget_item, null);
        EditText etConcept = dView.findViewById(R.id.et_bi_concept);
        EditText etAmount = dView.findViewById(R.id.et_bi_amount);
        Spinner spType = dView.findViewById(R.id.spinner_bi_type);
        EditText etTag = dView.findViewById(R.id.et_bi_tag);

        String[] types = {"Fijo", "Variable"};
        spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));

        new AlertDialog.Builder(this)
                .setView(dView)
                .setPositiveButton("Agregar", (dialog, which) -> {
                    String concept = etConcept.getText().toString().trim();
                    String amtStr = etAmount.getText().toString().trim();
                    String type = types[spType.getSelectedItemPosition()];
                    String tag = etTag.getText().toString().trim();

                    if (concept.isEmpty()) {
                        Toast.makeText(this, "Ingresa un concepto.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amount = 0;
                    try { amount = Double.parseDouble(amtStr); } catch (Exception ignored) {}

                    BudgetItem bi = new BudgetItem(null, budgetId, concept, amount, type, tag, System.currentTimeMillis());
                    dbHelper.insertBudgetItem(bi);
                    selectTab(5);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =========================================================================
    // 7. MÓDULO: AJUSTES & SAF EXPORT / IMPORT
    // =========================================================================

    private void renderSettingsModule(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_module_settings, containerBody, false);
        containerBody.addView(view);

        Button btnExport = view.findViewById(R.id.btn_settings_export_backup);
        Button btnImport = view.findViewById(R.id.btn_settings_import_backup);
        Button btnResetPeriod = view.findViewById(R.id.btn_settings_reset_period);
        Button btnChangePwd = view.findViewById(R.id.btn_settings_change_password);
        Button btnFactory = view.findViewById(R.id.btn_settings_factory_reset);

        // SAF EXPORT: The user picks ANY destination directory (Downloads, Google Drive, SD card).
        // This writes only the JSON backup file and NEVER moves or touches the internal SQLite database.
        btnExport.setOnClickListener(v -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String fileName = "cardmaster_backup_" + sdf.format(new Date()) + ".json";

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            exportJsonLauncher.launch(intent);
        });

        // SAF IMPORT
        btnImport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimeTypes = {"application/json", "text/plain", "application/octet-stream"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            importJsonLauncher.launch(intent);
        });

        btnResetPeriod.setOnClickListener(v -> showResetPeriodDialog());
        btnChangePwd.setOnClickListener(v -> showChangePasswordDialog());
        btnFactory.setOnClickListener(v -> showSettingsFactoryResetConfirmation());
    }

    private void handleExportBackupToUri(Uri uri) {
        try {
            JSONObject root = dbHelper.exportBackupJson();
            String jsonStr = root.toString(2);

            OutputStream os = getContentResolver().openOutputStream(uri);
            if (os == null) {
                Toast.makeText(this, "No se pudo escribir en el destino seleccionado.", Toast.LENGTH_LONG).show();
                return;
            }

            os.write(jsonStr.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            new AlertDialog.Builder(this)
                    .setTitle("✅ Respaldo Exportado")
                    .setMessage("Tu respaldo se guardó correctamente en la ubicación que elegiste.\n\n🛡️ Nota de Seguridad:\nTu base de datos interna permanece 100% segura e inalterada en el almacenamiento privado de la app.")
                    .setPositiveButton("Aceptar", null)
                    .show();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Error al Exportar")
                    .setMessage("Ocurrió un error al generar el respaldo:\n" + e.getMessage())
                    .setPositiveButton("Cerrar", null)
                    .show();
        }
    }

    private void handleImportBackupFromUri(Uri uri) {
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

            boolean ok = dbHelper.importBackupJson(sb.toString());
            if (ok) {
                new AlertDialog.Builder(this)
                        .setTitle("✅ Respaldo Importado")
                        .setMessage("La base de datos fue actualizada exitosamente con los datos del archivo.")
                        .setPositiveButton("Aceptar", (d, w) -> selectTab(0))
                        .show();
            } else {
                Toast.makeText(this, "El archivo no contiene un respaldo válido de CardMaster.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Error al Importar")
                    .setMessage("No se pudo restaurar el respaldo:\n" + e.getMessage())
                    .setPositiveButton("Cerrar", null)
                    .show();
        }
    }

    private void showResetPeriodDialog() {
        View dView = LayoutInflater.from(this).inflate(R.layout.dialog_reset_period, null);
        EditText etPwd = dView.findViewById(R.id.et_reset_period_password);

        new AlertDialog.Builder(this)
                .setView(dView)
                .setPositiveButton("Cerrar Periodo", (dialog, which) -> {
                    String pwd = etPwd.getText().toString();
                    boolean ok = dbHelper.resetCardsPeriod(pwd);
                    if (ok) {
                        Toast.makeText(this, "¡Periodo mensual cerrado exitosamente!", Toast.LENGTH_LONG).show();
                        selectTab(0);
                    } else {
                        Toast.makeText(this, "Contraseña incorrecta. No se realizaron cambios.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showChangePasswordDialog() {
        View dView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        EditText etCur = dView.findViewById(R.id.et_pwd_current);
        EditText etNew = dView.findViewById(R.id.et_pwd_new);
        EditText etConf = dView.findViewById(R.id.et_pwd_confirm);

        new AlertDialog.Builder(this)
                .setView(dView)
                .setPositiveButton("Actualizar Contraseña", (dialog, which) -> {
                    String cur = etCur.getText().toString();
                    String nw = etNew.getText().toString();
                    String cf = etConf.getText().toString();

                    if (nw.length() < 4) {
                        Toast.makeText(this, "La nueva contraseña debe tener al menos 4 caracteres.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (!nw.equals(cf)) {
                        Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    boolean ok = dbHelper.changeMasterPassword(cur, nw);
                    if (ok) {
                        Toast.makeText(this, "¡Contraseña actualizada correctamente!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "La contraseña actual es incorrecta.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showSettingsFactoryResetConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Restaurar de Fábrica")
                .setMessage("Esta acción eliminará de forma irreversible todas las tarjetas, gastos, presupuestos y contraseñas.\n\n¿Estás completamente seguro?")
                .setPositiveButton("Eliminar Todo", (dialog, which) -> {
                    dbHelper.factoryReset();
                    Toast.makeText(this, "La aplicación ha sido restaurada de fábrica.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.US, "$%,.2f", amount);
    }
}
