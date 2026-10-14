package com.cardmaster.finance.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.cardmaster.finance.model.Budget;
import com.cardmaster.finance.model.BudgetItem;
import com.cardmaster.finance.model.Card;
import com.cardmaster.finance.model.InstallmentPlan;
import com.cardmaster.finance.model.Movement;
import com.cardmaster.finance.model.Person;
import com.cardmaster.finance.model.SetAside;
import com.cardmaster.finance.util.CryptoUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Motor de persistencia nativo 100% Android basado en SQLite.
 * Almacena físicamente los datos en la memoria interna privada de la aplicación
 * (/data/data/com.cardmaster.finance/databases/cardmaster.db).
 *
 * Elimina toda dependencia de WebViews, WebAssembly, IndexedDB y servidores HTTP.
 * Proporciona importación y exportación de respaldos JSON universales totalmente
 * compatibles con la versión web para PC.
 */
public class CardMasterDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "cardmaster.db";
    private static final int DATABASE_VERSION = 1;

    private static CardMasterDbHelper instance;

    public static synchronized CardMasterDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new CardMasterDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    public CardMasterDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE cards (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "credit_limit REAL NOT NULL, " +
                "cutoff_day INTEGER NOT NULL, " +
                "color TEXT, " +
                "logo_base64 TEXT, " +
                "display_order INTEGER DEFAULT 0, " +
                "updated_at INTEGER)");

        db.execSQL("CREATE TABLE people (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "parent_id TEXT, " +
                "updated_at INTEGER)");

        db.execSQL("CREATE TABLE movements (" +
                "id TEXT PRIMARY KEY, " +
                "concept TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "date TEXT NOT NULL, " +
                "card_id TEXT NOT NULL, " +
                "person_id TEXT NOT NULL, " +
                "sub_person_id TEXT, " +
                "is_set_aside INTEGER DEFAULT 0, " +
                "updated_at INTEGER)");

        db.execSQL("CREATE TABLE installment_plans (" +
                "id TEXT PRIMARY KEY, " +
                "concept TEXT NOT NULL, " +
                "total_amount REAL NOT NULL, " +
                "months INTEGER NOT NULL, " +
                "start_date TEXT NOT NULL, " +
                "card_id TEXT NOT NULL, " +
                "person_id TEXT NOT NULL, " +
                "paid_months INTEGER DEFAULT 0, " +
                "updated_at INTEGER)");

        db.execSQL("CREATE TABLE set_asides (" +
                "id TEXT PRIMARY KEY, " +
                "card_id TEXT NOT NULL, " +
                "person_id TEXT NOT NULL, " +
                "movement_id TEXT, " +
                "amount REAL NOT NULL, " +
                "note TEXT, " +
                "date TEXT NOT NULL, " +
                "fund_type TEXT DEFAULT 'Efectivo', " +
                "is_paid INTEGER DEFAULT 0, " +
                "paid_amount REAL DEFAULT 0, " +
                "status TEXT DEFAULT 'apartado', " +
                "updated_at INTEGER)");

        db.execSQL("CREATE TABLE budgets (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "updated_at INTEGER)");

        db.execSQL("CREATE TABLE budget_items (" +
                "id TEXT PRIMARY KEY, " +
                "budget_id TEXT NOT NULL, " +
                "concept TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "type TEXT NOT NULL, " +
                "tag TEXT, " +
                "updated_at INTEGER)");

        db.execSQL("CREATE TABLE app_config (" +
                "key TEXT PRIMARY KEY, " +
                "value TEXT)");

        seedInitialData(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        try {
            db.execSQL("ALTER TABLE cards ADD COLUMN display_order INTEGER DEFAULT 0");
        } catch (Exception ignored) {}
        try {
            db.execSQL("ALTER TABLE people ADD COLUMN parent_id TEXT");
        } catch (Exception ignored) {}
        try {
            db.execSQL("ALTER TABLE movements ADD COLUMN sub_person_id TEXT");
        } catch (Exception ignored) {}
        seedInitialData(db);
    }

    private void markSeedApplied(SQLiteDatabase db) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("key", "seed_applied");
            cv.put("value", "1");
            db.insertWithOnConflict("app_config", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception ignored) {}
    }

    private void insertCardRow(SQLiteDatabase db, String id, String name, double limit, int cutoff, String color, int order, long now) {
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("name", name);
        cv.put("credit_limit", limit);
        cv.put("cutoff_day", cutoff);
        cv.put("color", color);
        cv.put("display_order", order);
        cv.put("updated_at", now);
        db.insertWithOnConflict("cards", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void insertPersonRow(SQLiteDatabase db, String id, String name, long now) {
        insertPersonRow(db, id, name, null, now);
    }

    private void insertPersonRow(SQLiteDatabase db, String id, String name, String parentId, long now) {
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("name", name);
        cv.put("parent_id", parentId);
        cv.put("updated_at", now);
        db.insertWithOnConflict("people", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void insertMovementRow(SQLiteDatabase db, String id, String concept, double amount, String date, String cardId, String personId, int isSetAside, long now) {
        insertMovementRow(db, id, concept, amount, date, cardId, personId, null, isSetAside, now);
    }

    private void insertMovementRow(SQLiteDatabase db, String id, String concept, double amount, String date, String cardId, String personId, String subPersonId, int isSetAside, long now) {
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("concept", concept);
        cv.put("amount", amount);
        cv.put("date", date);
        cv.put("card_id", cardId);
        cv.put("person_id", personId);
        cv.put("sub_person_id", subPersonId);
        cv.put("is_set_aside", isSetAside);
        cv.put("updated_at", now);
        db.insertWithOnConflict("movements", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void insertSetAsideRow(SQLiteDatabase db, String id, String cardId, String personId, String movementId, double amount, String note, String date, String fundType, long now) {
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("card_id", cardId);
        cv.put("person_id", personId);
        cv.put("movement_id", movementId);
        cv.put("amount", amount);
        cv.put("note", note);
        cv.put("date", date);
        cv.put("fund_type", fundType);
        cv.put("is_paid", 0);
        cv.put("paid_amount", 0.0);
        cv.put("status", "apartado");
        cv.put("updated_at", now);
        db.insertWithOnConflict("set_asides", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void seedInitialData(SQLiteDatabase db) {
        Cursor cCfg = null;
        try {
            cCfg = db.rawQuery("SELECT value FROM app_config WHERE key = 'seed_applied'", null);
            if (cCfg != null && cCfg.moveToFirst()) {
                return;
            }
        } catch (Exception ignored) {
        } finally {
            if (cCfg != null) cCfg.close();
        }

        Cursor cCards = null;
        try {
            cCards = db.rawQuery("SELECT COUNT(*) FROM cards", null);
            if (cCards != null && cCards.moveToFirst() && cCards.getInt(0) > 0) {
                markSeedApplied(db);
                return;
            }
        } catch (Exception ignored) {
        } finally {
            if (cCards != null) cCards.close();
        }

        db.beginTransaction();
        try {
            long now = System.currentTimeMillis();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            // 1. Tarjetas de Crédito
            insertCardRow(db, "card-bbva-platinum", "BBVA Platinum", 509700.0, 16, "#004481", 0, now);
            insertCardRow(db, "card-stori", "Stori", 20000.0, 12, "#00a86b", 1, now);
            insertCardRow(db, "card-banamex-costco", "Banamex Costco", 58000.0, 8, "#0284c7", 2, now);
            insertCardRow(db, "card-hsbc-viva", "HSBC VIVA", 150000.0, 11, "#db0011", 3, now);
            insertCardRow(db, "card-nu", "NU", 4000.0, 23, "#820ad1", 4, now);
            insertCardRow(db, "card-rappi-card", "Rappi Card", 8000.0, 21, "#ff441f", 5, now);

            // 2. Personas / Deudores
            insertPersonRow(db, "person-personal", "Personal", now);
            insertPersonRow(db, "person-plj", "PLJ", now);
            insertPersonRow(db, "person-blj", "BLJ", now);
            insertPersonRow(db, "person-gjg", "GJG", now);
            insertPersonRow(db, "person-elh", "ELH", now);
            insertPersonRow(db, "person-jess", "Jess", now);
            insertPersonRow(db, "person-gaby", "Gaby", now);

            // 3. Configuración base
            ContentValues cvCfg = new ContentValues();
            cvCfg.put("key", "inflation_rate");
            cvCfg.put("value", "4.5");
            db.insertWithOnConflict("app_config", null, cvCfg, SQLiteDatabase.CONFLICT_REPLACE);

            ContentValues cvSeed = new ContentValues();
            cvSeed.put("key", "seed_applied");
            cvSeed.put("value", "1");
            db.insertWithOnConflict("app_config", null, cvSeed, SQLiteDatabase.CONFLICT_REPLACE);

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migraciones futuras de esquema
    }

    // =========================================================================
    // 1. GESTIÓN DE AUTENTICACIÓN Y SEGURIDAD (BÓVEDA)
    // =========================================================================

    public boolean isVaultConfigured() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT value FROM app_config WHERE key = 'master_pwd_hash'", null);
        boolean configured = false;
        if (c != null) {
            if (c.moveToFirst()) {
                String hash = c.getString(0);
                configured = (hash != null && !hash.trim().isEmpty());
            }
            c.close();
        }
        return configured;
    }

    public boolean setupMasterPassword(String password) {
        if (password == null || password.trim().isEmpty()) return false;
        String salt = CryptoUtils.generateSaltHex(16);
        String hash = CryptoUtils.hashPassword(password, salt);
        if (hash == null) return false;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cvHash = new ContentValues();
            cvHash.put("key", "master_pwd_hash");
            cvHash.put("value", hash);
            db.insertWithOnConflict("app_config", null, cvHash, SQLiteDatabase.CONFLICT_REPLACE);

            ContentValues cvSalt = new ContentValues();
            cvSalt.put("key", "master_pwd_salt");
            cvSalt.put("value", salt);
            db.insertWithOnConflict("app_config", null, cvSalt, SQLiteDatabase.CONFLICT_REPLACE);

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public boolean verifyMasterPassword(String password) {
        if (password == null) return false;
        SQLiteDatabase db = getReadableDatabase();
        String storedHash = null;
        String storedSalt = null;

        Cursor c = db.rawQuery("SELECT key, value FROM app_config WHERE key IN ('master_pwd_hash', 'master_pwd_salt')", null);
        if (c != null) {
            while (c.moveToNext()) {
                String k = c.getString(0);
                String v = c.getString(1);
                if ("master_pwd_hash".equals(k)) storedHash = v;
                else if ("master_pwd_salt".equals(k)) storedSalt = v;
            }
            c.close();
        }

        if (storedHash == null || storedSalt == null) return false;
        return CryptoUtils.verifyPassword(password, storedHash, storedSalt);
    }

    public boolean changeMasterPassword(String oldPassword, String newPassword) {
        if (!verifyMasterPassword(oldPassword)) return false;
        return setupMasterPassword(newPassword);
    }

    // =========================================================================
    // 2. MANTENIMIENTO: RESETEO MENSUAL Y RESTAURACIÓN DE FÁBRICA
    // =========================================================================

    public boolean resetCardsPeriod(String password) {
        if (!verifyMasterPassword(password)) return false;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Eliminar gastos ordinarios del periodo
            db.execSQL("DELETE FROM movements");
            // Eliminar fondos apartados que ya fueron pagados al banco
            db.execSQL("DELETE FROM set_asides WHERE is_paid = 1");

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public void factoryReset() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM movements");
            db.execSQL("DELETE FROM installment_plans");
            db.execSQL("DELETE FROM set_asides");
            db.execSQL("DELETE FROM budget_items");
            db.execSQL("DELETE FROM budgets");
            db.execSQL("DELETE FROM cards");
            db.execSQL("DELETE FROM people");
            db.execSQL("DELETE FROM app_config");

            long now = System.currentTimeMillis();
            ContentValues cvPerson = new ContentValues();
            cvPerson.put("id", UUID.randomUUID().toString());
            cvPerson.put("name", "Personal");
            cvPerson.put("updated_at", now);
            db.insert("people", null, cvPerson);

            ContentValues cvCfg = new ContentValues();
            cvCfg.put("key", "inflation_rate");
            cvCfg.put("value", "10");
            db.insert("app_config", null, cvCfg);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // =========================================================================
    // 3. TARJETAS DE CRÉDITO (CRUD & MÉTRICAS)
    // =========================================================================

    public List<Card> getAllCards() {
        List<Card> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, credit_limit, cutoff_day, color, logo_base64, updated_at, display_order FROM cards ORDER BY display_order ASC, name ASC", null);
        if (c != null) {
            while (c.moveToNext()) {
                Card card = new Card(
                        c.getString(0),
                        c.getString(1),
                        c.getDouble(2),
                        c.getInt(3),
                        c.getString(4),
                        c.getString(5),
                        c.getLong(6),
                        c.getInt(7)
                );
                computeCardMetrics(card, db);
                list.add(card);
            }
            c.close();
        }
        return list;
    }

    public Card getCard(String id) {
        if (id == null) return null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, credit_limit, cutoff_day, color, logo_base64, updated_at, display_order FROM cards WHERE id = ?", new String[]{id});
        Card card = null;
        if (c != null) {
            if (c.moveToFirst()) {
                card = new Card(
                        c.getString(0),
                        c.getString(1),
                        c.getDouble(2),
                        c.getInt(3),
                        c.getString(4),
                        c.getString(5),
                        c.getLong(6),
                        c.getInt(7)
                );
                computeCardMetrics(card, db);
            }
            c.close();
        }
        return card;
    }

    private void computeCardMetrics(Card card, SQLiteDatabase db) {
        // 1. Saldo gastado en compras ordinarias
        double spentPeriod = 0;
        Cursor cMov = db.rawQuery("SELECT SUM(amount) FROM movements WHERE card_id = ?", new String[]{card.getId()});
        if (cMov != null) {
            if (cMov.moveToFirst()) spentPeriod = cMov.getDouble(0);
            cMov.close();
        }

        // 2. Saldo en mensualidades de MSI
        double spentMsi = 0;
        Cursor cMsi = db.rawQuery("SELECT total_amount, months, paid_months FROM installment_plans WHERE card_id = ?", new String[]{card.getId()});
        if (cMsi != null) {
            while (cMsi.moveToNext()) {
                double total = cMsi.getDouble(0);
                int months = cMsi.getInt(1);
                int paid = cMsi.getInt(2);
                if (months > 0 && paid < months) {
                    spentMsi += (total / months);
                }
            }
            cMsi.close();
        }

        double paidToBank = 0;
        Cursor cPaid = db.rawQuery(
                "SELECT COALESCE(SUM(CASE WHEN is_paid = 1 THEN amount ELSE paid_amount END), 0) " +
                "FROM set_asides WHERE card_id = ?", new String[]{card.getId()});
        if (cPaid != null) {
            if (cPaid.moveToFirst()) paidToBank = cPaid.getDouble(0);
            cPaid.close();
        }

        double totalOwed = Math.max(0, (spentPeriod - paidToBank) + spentMsi);
        double available = Math.max(0, card.getCreditLimit() - totalOwed);
        double utilization = card.getCreditLimit() > 0 ? (totalOwed / card.getCreditLimit() * 100) : 0;

        // 3. Dinero apartado activo en mano para esta tarjeta (no pagado aún al banco)
        double totalSetAside = 0;
        Cursor cSa = db.rawQuery(
                "SELECT COALESCE(SUM(amount - paid_amount), 0) FROM set_asides WHERE card_id = ? AND is_paid = 0",
                new String[]{card.getId()});
        if (cSa != null) {
            if (cSa.moveToFirst()) totalSetAside = Math.max(0, cSa.getDouble(0));
            cSa.close();
        }
        double missingToSetAside = Math.max(0, totalOwed - totalSetAside);

        // 4. Deudores de esta tarjeta y su monto restante
        // Regla: Si los deudores ya apartaron/cubrieron el 100% o no tienen gastos en la tarjeta, NO aparecen.
        List<Card.DebtorSummary> pendingDebtors = new ArrayList<>();
        Cursor cPeople = db.rawQuery("SELECT id, name FROM people ORDER BY name ASC", null);
        if (cPeople != null) {
            while (cPeople.moveToNext()) {
                String pId = cPeople.getString(0);
                String pName = cPeople.getString(1);

                double pSpent = 0;
                Cursor cPSpent = db.rawQuery("SELECT SUM(amount) FROM movements WHERE card_id = ? AND person_id = ?", new String[]{card.getId(), pId});
                if (cPSpent != null) {
                    if (cPSpent.moveToFirst()) pSpent = cPSpent.getDouble(0);
                    cPSpent.close();
                }

                Cursor cPMsi = db.rawQuery("SELECT total_amount, months, paid_months FROM installment_plans WHERE card_id = ? AND person_id = ?", new String[]{card.getId(), pId});
                if (cPMsi != null) {
                    while (cPMsi.moveToNext()) {
                        double tot = cPMsi.getDouble(0);
                        int mos = cPMsi.getInt(1);
                        int pd = cPMsi.getInt(2);
                        if (mos > 0 && pd < mos) {
                            pSpent += (tot / mos);
                        }
                    }
                    cPMsi.close();
                }

                if (pSpent <= 0.001) continue; // No tiene gastos en esta tarjeta

                double pSetAside = 0;
                Cursor cPSa = db.rawQuery(
                        "SELECT COALESCE(SUM(s.amount), 0) FROM set_asides s " +
                        "LEFT JOIN movements m ON s.movement_id = m.id " +
                        "WHERE (s.card_id = ? OR m.card_id = ?) AND (s.person_id = ? OR m.person_id = ?)",
                        new String[]{card.getId(), card.getId(), pId, pId});
                if (cPSa != null) {
                    if (cPSa.moveToFirst()) pSetAside = cPSa.getDouble(0);
                    cPSa.close();
                }

                double pRemaining = Math.max(0, pSpent - pSetAside);
                // Solo si aún le falta por apartar
                if (pRemaining > 0.001) {
                    pendingDebtors.add(new Card.DebtorSummary(pId, pName, pRemaining));
                }
            }
            cPeople.close();
        }

        card.setSpentPeriod(spentPeriod);
        card.setSpentMsi(spentMsi);
        card.setTotalOwed(totalOwed);
        card.setAvailable(available);
        card.setUtilizationPercent(utilization);
        card.setTotalSetAside(totalSetAside);
        card.setMissingToSetAside(missingToSetAside);
        card.setPendingDebtors(pendingDebtors);
    }

    public boolean insertCard(Card card) {
        if (card.getId() == null) card.setId(UUID.randomUUID().toString());
        card.setUpdatedAt(System.currentTimeMillis());

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", card.getId());
        cv.put("name", card.getName());
        cv.put("credit_limit", card.getCreditLimit());
        cv.put("cutoff_day", card.getCutoffDay());
        cv.put("color", card.getColor());
        cv.put("logo_base64", card.getLogoBase64());
        cv.put("display_order", card.getDisplayOrder());
        cv.put("updated_at", card.getUpdatedAt());

        long res = db.insertWithOnConflict("cards", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return res != -1;
    }

    public boolean updateCard(Card card) {
        card.setUpdatedAt(System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", card.getName());
        cv.put("credit_limit", card.getCreditLimit());
        cv.put("cutoff_day", card.getCutoffDay());
        cv.put("color", card.getColor());
        cv.put("logo_base64", card.getLogoBase64());
        cv.put("display_order", card.getDisplayOrder());
        cv.put("updated_at", card.getUpdatedAt());

        int count = db.update("cards", cv, "id = ?", new String[]{card.getId()});
        return count > 0;
    }

    public void updateCardOrder(List<Card> cards) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < cards.size(); i++) {
                ContentValues cv = new ContentValues();
                cv.put("display_order", i);
                db.update("cards", cv, "id = ?", new String[]{cards.get(i).getId()});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public boolean deleteCard(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("movements", "card_id = ?", new String[]{id});
            db.delete("installment_plans", "card_id = ?", new String[]{id});
            db.delete("set_asides", "card_id = ?", new String[]{id});
            int count = db.delete("cards", "id = ?", new String[]{id});
            db.setTransactionSuccessful();
            return count > 0;
        } finally {
            db.endTransaction();
        }
    }

    // =========================================================================
    // 4. MOVIMIENTOS Y GASTOS (CRUD)
    // =========================================================================

    public List<Movement> getAllMovements(String cardIdFilter, String personIdFilter) {
        List<Movement> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        StringBuilder query = new StringBuilder(
                "SELECT m.id, m.concept, m.amount, m.date, m.card_id, m.person_id, m.is_set_aside, m.updated_at, " +
                "c.name as card_name, p.name as person_name, m.sub_person_id, sp.name as sub_person_name " +
                "FROM movements m " +
                "LEFT JOIN cards c ON m.card_id = c.id " +
                "LEFT JOIN people p ON m.person_id = p.id " +
                "LEFT JOIN people sp ON m.sub_person_id = sp.id " +
                "WHERE 1=1 ");

        List<String> args = new ArrayList<>();
        if (cardIdFilter != null && !cardIdFilter.isEmpty()) {
            query.append("AND m.card_id = ? ");
            args.add(cardIdFilter);
        }
        if (personIdFilter != null && !personIdFilter.isEmpty()) {
            query.append("AND m.person_id = ? ");
            args.add(personIdFilter);
        }
        query.append("ORDER BY m.date DESC, m.updated_at DESC");

        Cursor c = db.rawQuery(query.toString(), args.toArray(new String[0]));
        if (c != null) {
            while (c.moveToNext()) {
                Movement m = new Movement(
                        c.getString(0),
                        c.getString(1),
                        c.getDouble(2),
                        c.getString(3),
                        c.getString(4),
                        c.getString(5),
                        c.getString(10),
                        c.getInt(6),
                        c.getLong(7)
                );
                m.setCardName(c.getString(8) != null ? c.getString(8) : "Sin Tarjeta");
                m.setPersonName(c.getString(9) != null ? c.getString(9) : "Personal");
                m.setSubPersonName(c.getString(11));
                list.add(m);
            }
            c.close();
        }
        return list;
    }

    public boolean insertMovement(Movement mov) {
        if (mov.getId() == null) mov.setId(UUID.randomUUID().toString());
        mov.setUpdatedAt(System.currentTimeMillis());

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", mov.getId());
        cv.put("concept", mov.getConcept());
        cv.put("amount", mov.getAmount());
        cv.put("date", mov.getDate());
        cv.put("card_id", mov.getCardId());
        cv.put("person_id", mov.getPersonId());
        cv.put("sub_person_id", mov.getSubPersonId());
        cv.put("is_set_aside", mov.getIsSetAside());
        cv.put("updated_at", mov.getUpdatedAt());

        long res = db.insertWithOnConflict("movements", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return res != -1;
    }

    public boolean deleteMovement(String id) {
        SQLiteDatabase db = getWritableDatabase();
        int count = db.delete("movements", "id = ?", new String[]{id});
        return count > 0;
    }

    public List<Movement> getPendingMovementsForPerson(String personId) {
        List<Movement> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // 1. Obtener todos los movimientos de esta persona ordenados cronológicamente (FIFO)
        List<Movement> allMovs = new ArrayList<>();
        Cursor cMovs = db.rawQuery(
                "SELECT m.id, m.concept, m.amount, m.date, m.card_id, m.person_id, m.is_set_aside, m.updated_at, " +
                "c.name as card_name, p.name as person_name, m.sub_person_id, sp.name as sub_person_name " +
                "FROM movements m " +
                "LEFT JOIN cards c ON m.card_id = c.id " +
                "LEFT JOIN people p ON m.person_id = p.id " +
                "LEFT JOIN people sp ON m.sub_person_id = sp.id " +
                "WHERE m.person_id = ? " +
                "ORDER BY m.date ASC, m.updated_at ASC", new String[]{personId});

        if (cMovs != null) {
            while (cMovs.moveToNext()) {
                Movement m = new Movement(
                        cMovs.getString(0),
                        cMovs.getString(1),
                        cMovs.getDouble(2),
                        cMovs.getString(3),
                        cMovs.getString(4),
                        cMovs.getString(5),
                        cMovs.getString(10),
                        cMovs.getInt(6),
                        cMovs.getLong(7)
                );
                m.setCardName(cMovs.getString(8) != null ? cMovs.getString(8) : "Sin Tarjeta");
                m.setPersonName(cMovs.getString(9) != null ? cMovs.getString(9) : "Personal");
                m.setSubPersonName(cMovs.getString(11));
                allMovs.add(m);
            }
            cMovs.close();
        }

        if (allMovs.isEmpty()) return list;

        // 2. Mapear apartados vinculados directamente y apartados libres/no vinculados por tarjeta
        Map<String, Double> directAsideMap = new HashMap<>();
        Map<String, Double> unallocatedByCard = new HashMap<>();
        double unallocatedGlobal = 0.0;

        Cursor cSa = db.rawQuery(
                "SELECT s.id, s.card_id, s.movement_id, s.amount FROM set_asides s " +
                "LEFT JOIN movements m ON s.movement_id = m.id " +
                "WHERE (s.person_id = ? OR m.person_id = ?)",
                new String[]{personId, personId});

        if (cSa != null) {
            while (cSa.moveToNext()) {
                String cardId = cSa.getString(1);
                String movId = cSa.getString(2);
                double amount = cSa.getDouble(3);

                if (movId != null && !movId.trim().isEmpty()) {
                    directAsideMap.put(movId, directAsideMap.getOrDefault(movId, 0.0) + amount);
                } else if (cardId != null && !cardId.trim().isEmpty()) {
                    unallocatedByCard.put(cardId, unallocatedByCard.getOrDefault(cardId, 0.0) + amount);
                } else {
                    unallocatedGlobal += amount;
                }
            }
            cSa.close();
        }

        // 3. Asignar apartados directos
        for (Movement m : allMovs) {
            double direct = directAsideMap.getOrDefault(m.getId(), 0.0);
            m.setTotalSetAside(direct);
        }

        // 4. Distribuir apartados no vinculados de la misma tarjeta (FIFO)
        for (Movement m : allMovs) {
            double rem = Math.max(0, m.getAmount() - m.getTotalSetAside());
            if (rem > 0.001 && m.getCardId() != null) {
                double pool = unallocatedByCard.getOrDefault(m.getCardId(), 0.0);
                if (pool > 0.001) {
                    double alloc = Math.min(rem, pool);
                    m.setTotalSetAside(m.getTotalSetAside() + alloc);
                    unallocatedByCard.put(m.getCardId(), pool - alloc);
                }
            }
        }

        // 5. Distribuir apartados globales sin tarjeta específica
        for (Movement m : allMovs) {
            double rem = Math.max(0, m.getAmount() - m.getTotalSetAside());
            if (rem > 0.001 && unallocatedGlobal > 0.001) {
                double alloc = Math.min(rem, unallocatedGlobal);
                m.setTotalSetAside(m.getTotalSetAside() + alloc);
                unallocatedGlobal -= alloc;
            }
        }

        // 6. Filtrar solo aquellos que todavía tengan saldo pendiente real
        for (Movement m : allMovs) {
            double pending = Math.max(0, m.getAmount() - m.getTotalSetAside());
            if (pending > 0.001) {
                list.add(m);
            }
        }

        // 7. Ordenar los pendientes del más nuevo al más antiguo para visualización
        Collections.sort(list, (a, b) -> {
            int cmp = (b.getDate() != null ? b.getDate() : "").compareTo(a.getDate() != null ? a.getDate() : "");
            if (cmp != 0) return cmp;
            return Long.compare(b.getUpdatedAt(), a.getUpdatedAt());
        });

        return list;
    }

    public List<Movement> getAllPendingMovements() {
        List<Movement> allPending = new ArrayList<>();
        List<Person> people = getAllPeople();
        for (Person p : people) {
            allPending.addAll(getPendingMovementsForPerson(p.getId()));
        }
        return allPending;
    }

    public void updateMovementSetAsideStatus(String movementId, int isSetAside) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_set_aside", isSetAside);
        cv.put("updated_at", System.currentTimeMillis());
        db.update("movements", cv, "id = ?", new String[]{movementId});
    }

    // =========================================================================
    // 5. PLANES A MESES SIN INTERESES (MSI)
    // =========================================================================

    public List<InstallmentPlan> getInstallmentPlansByCard(String cardId) {
        List<InstallmentPlan> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, concept, total_amount, months, start_date, card_id, person_id, paid_months, updated_at " +
                "FROM installment_plans WHERE card_id = ? ORDER BY start_date DESC", new String[]{cardId});
        if (c != null) {
            while (c.moveToNext()) {
                InstallmentPlan plan = new InstallmentPlan(
                        c.getString(0),
                        c.getString(1),
                        c.getDouble(2),
                        c.getInt(3),
                        c.getString(4),
                        c.getString(5),
                        c.getString(6),
                        c.getInt(7),
                        c.getLong(8)
                );
                list.add(plan);
            }
            c.close();
        }
        return list;
    }

    public boolean insertInstallmentPlan(InstallmentPlan plan) {
        if (plan.getId() == null) plan.setId(UUID.randomUUID().toString());
        plan.setUpdatedAt(System.currentTimeMillis());

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", plan.getId());
        cv.put("concept", plan.getConcept());
        cv.put("total_amount", plan.getTotalAmount());
        cv.put("months", plan.getMonths());
        cv.put("start_date", plan.getStartDate());
        cv.put("card_id", plan.getCardId());
        cv.put("person_id", plan.getPersonId());
        cv.put("paid_months", plan.getPaidMonths());
        cv.put("updated_at", plan.getUpdatedAt());

        long res = db.insertWithOnConflict("installment_plans", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return res != -1;
    }

    public boolean advanceInstallmentMonth(String id) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT paid_months, months FROM installment_plans WHERE id = ?", new String[]{id});
        if (c != null) {
            if (c.moveToFirst()) {
                int paid = c.getInt(0);
                int months = c.getInt(1);
                c.close();
                if (paid < months) {
                    ContentValues cv = new ContentValues();
                    cv.put("paid_months", paid + 1);
                    cv.put("updated_at", System.currentTimeMillis());
                    int count = db.update("installment_plans", cv, "id = ?", new String[]{id});
                    return count > 0;
                }
            } else {
                c.close();
            }
        }
        return false;
    }

    public boolean deleteInstallmentPlan(String id) {
        SQLiteDatabase db = getWritableDatabase();
        int count = db.delete("installment_plans", "id = ?", new String[]{id});
        return count > 0;
    }

    // =========================================================================
    // 6. FONDOS APARTADOS (APARTADOS & TRASPASOS)
    // =========================================================================

    public List<SetAside> getAllSetAsides() {
        List<SetAside> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT s.id, s.card_id, s.person_id, s.movement_id, s.amount, s.note, s.date, s.fund_type, " +
                "s.is_paid, s.paid_amount, s.status, s.updated_at, " +
                "COALESCE(c.name, mc.name, 'Sin Tarjeta') as card_name, " +
                "COALESCE(p.name, mp.name, 'Personal') as person_name " +
                "FROM set_asides s " +
                "LEFT JOIN cards c ON s.card_id = c.id " +
                "LEFT JOIN people p ON s.person_id = p.id " +
                "LEFT JOIN movements m ON s.movement_id = m.id " +
                "LEFT JOIN cards mc ON m.card_id = mc.id " +
                "LEFT JOIN people mp ON m.person_id = mp.id " +
                "ORDER BY s.date DESC, s.updated_at DESC", null);
        if (c != null) {
            while (c.moveToNext()) {
                SetAside sa = new SetAside(
                        c.getString(0),
                        c.getString(1),
                        c.getString(2),
                        c.getString(3),
                        c.getDouble(4),
                        c.getString(5),
                        c.getString(6),
                        c.getString(7),
                        c.getInt(8),
                        c.getDouble(9),
                        c.getString(10),
                        c.getLong(11)
                );
                sa.setCardName(c.getString(12) != null ? c.getString(12) : "Sin Tarjeta");
                sa.setPersonName(c.getString(13) != null ? c.getString(13) : "Personal");
                list.add(sa);
            }
            c.close();
        }
        return list;
    }

    public boolean insertSetAside(SetAside sa) {
        if (sa.getId() == null) sa.setId(UUID.randomUUID().toString());
        sa.setUpdatedAt(System.currentTimeMillis());

        SQLiteDatabase db = getWritableDatabase();

        // Si tiene movement_id y faltan card_id o person_id, complementarlos desde movements
        if (sa.getMovementId() != null && !sa.getMovementId().isEmpty()) {
            if (sa.getCardId() == null || sa.getCardId().isEmpty() || sa.getPersonId() == null || sa.getPersonId().isEmpty()) {
                Cursor cMov = db.rawQuery("SELECT card_id, person_id FROM movements WHERE id = ?", new String[]{sa.getMovementId()});
                if (cMov != null) {
                    if (cMov.moveToFirst()) {
                        if (sa.getCardId() == null || sa.getCardId().isEmpty()) sa.setCardId(cMov.getString(0));
                        if (sa.getPersonId() == null || sa.getPersonId().isEmpty()) sa.setPersonId(cMov.getString(1));
                    }
                    cMov.close();
                }
            }
        }

        ContentValues cv = new ContentValues();
        cv.put("id", sa.getId());
        cv.put("card_id", sa.getCardId());
        cv.put("person_id", sa.getPersonId());
        cv.put("movement_id", sa.getMovementId());
        cv.put("amount", sa.getAmount());
        cv.put("note", sa.getNote());
        cv.put("date", sa.getDate());
        cv.put("fund_type", sa.getFundType());
        cv.put("is_paid", sa.getIsPaid());
        cv.put("paid_amount", sa.getPaidAmount());
        cv.put("status", sa.getStatus());
        cv.put("updated_at", sa.getUpdatedAt());

        long res = db.insertWithOnConflict("set_asides", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        if (res != -1 && sa.getMovementId() != null && !sa.getMovementId().isEmpty()) {
            Cursor cMov = db.rawQuery("SELECT amount FROM movements WHERE id = ?", new String[]{sa.getMovementId()});
            if (cMov != null) {
                if (cMov.moveToFirst()) {
                    double movAmt = cMov.getDouble(0);
                    Cursor cSum = db.rawQuery("SELECT SUM(amount) FROM set_asides WHERE movement_id = ?", new String[]{sa.getMovementId()});
                    double sumAside = 0;
                    if (cSum != null) {
                        if (cSum.moveToFirst()) sumAside = cSum.getDouble(0);
                        cSum.close();
                    }
                    if (sumAside >= movAmt && movAmt > 0) {
                        updateMovementSetAsideStatus(sa.getMovementId(), 1);
                    }
                }
                cMov.close();
            }
        }
        return res != -1;
    }

    public boolean toggleFundType(String id) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT fund_type FROM set_asides WHERE id = ?", new String[]{id});
        String cur = "Efectivo";
        if (c != null) {
            if (c.moveToFirst()) cur = c.getString(0);
            c.close();
        }
        String next = "Efectivo".equalsIgnoreCase(cur) ? "Débito" : "Efectivo";

        ContentValues cv = new ContentValues();
        cv.put("fund_type", next);
        cv.put("updated_at", System.currentTimeMillis());
        int count = db.update("set_asides", cv, "id = ?", new String[]{id});
        return count > 0;
    }

    public boolean payToBank(String cardId, double amountToPay, boolean isFull) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("SELECT id, amount, paid_amount, is_paid FROM set_asides WHERE card_id = ? AND is_paid = 0 ORDER BY date ASC", new String[]{cardId});
            double remaining = amountToPay;

            if (c != null) {
                while (c.moveToNext()) {
                    String saId = c.getString(0);
                    double totalSa = c.getDouble(1);
                    double curPaid = c.getDouble(2);
                    double activeLeft = totalSa - curPaid;

                    if (isFull) {
                        ContentValues cv = new ContentValues();
                        cv.put("is_paid", 1);
                        cv.put("paid_amount", totalSa);
                        cv.put("status", "pagado");
                        cv.put("updated_at", System.currentTimeMillis());
                        db.update("set_asides", cv, "id = ?", new String[]{saId});
                    } else if (remaining > 0) {
                        double payForThis = Math.min(remaining, activeLeft);
                        double newPaid = curPaid + payForThis;
                        boolean markPaid = (newPaid >= totalSa);

                        ContentValues cv = new ContentValues();
                        cv.put("is_paid", markPaid ? 1 : 0);
                        cv.put("paid_amount", newPaid);
                        cv.put("status", markPaid ? "pagado" : "parcial");
                        cv.put("updated_at", System.currentTimeMillis());
                        db.update("set_asides", cv, "id = ?", new String[]{saId});

                        remaining -= payForThis;
                    }
                }
                c.close();
            }

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public boolean deleteSetAside(String id) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT movement_id FROM set_asides WHERE id = ?", new String[]{id});
        String movId = null;
        if (c != null) {
            if (c.moveToFirst()) movId = c.getString(0);
            c.close();
        }
        int count = db.delete("set_asides", "id = ?", new String[]{id});
        if (movId != null && !movId.isEmpty()) {
            Cursor cLeft = db.rawQuery("SELECT SUM(amount) FROM set_asides WHERE movement_id = ?", new String[]{movId});
            double left = 0;
            if (cLeft != null) {
                if (cLeft.moveToFirst()) left = cLeft.getDouble(0);
                cLeft.close();
            }
            Cursor cMov = db.rawQuery("SELECT amount FROM movements WHERE id = ?", new String[]{movId});
            if (cMov != null) {
                if (cMov.moveToFirst()) {
                    double movAmt = cMov.getDouble(0);
                    updateMovementSetAsideStatus(movId, (left >= movAmt && movAmt > 0) ? 1 : 0);
                }
                cMov.close();
            }
        }
        return count > 0;
    }

    // =========================================================================
    // 7. PERSONAS Y DEUDORES (CRUD & BALANCES)
    // =========================================================================

    public List<Person> getAllPeople() {
        List<Person> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, parent_id, updated_at FROM people WHERE parent_id IS NULL OR parent_id = '' ORDER BY name ASC", null);
        if (c != null) {
            while (c.moveToNext()) {
                Person p = new Person(c.getString(0), c.getString(1), c.getString(2), c.getLong(3));
                computePersonMetrics(p, db);
                p.setSubPeople(getSubPeopleForPerson(p.getId(), db));
                list.add(p);
            }
            c.close();
        }
        return list;
    }

    public List<Person> getSubPeopleForPerson(String parentId) {
        SQLiteDatabase db = getReadableDatabase();
        return getSubPeopleForPerson(parentId, db);
    }

    private List<Person> getSubPeopleForPerson(String parentId, SQLiteDatabase db) {
        List<Person> list = new ArrayList<>();
        if (parentId == null || parentId.isEmpty()) return list;
        Cursor c = db.rawQuery("SELECT id, name, parent_id, updated_at FROM people WHERE parent_id = ? ORDER BY name ASC", new String[]{parentId});
        if (c != null) {
            while (c.moveToNext()) {
                Person sub = new Person(c.getString(0), c.getString(1), c.getString(2), c.getLong(3));
                Cursor cSpent = db.rawQuery("SELECT COALESCE(SUM(amount), 0) FROM movements WHERE person_id = ? AND sub_person_id = ?", new String[]{parentId, sub.getId()});
                if (cSpent != null) {
                    if (cSpent.moveToFirst()) {
                        sub.setSpentForSubPerson(cSpent.getDouble(0));
                    }
                    cSpent.close();
                }
                list.add(sub);
            }
            c.close();
        }
        return list;
    }

    public double getSpentForPersonGeneral(String personId) {
        SQLiteDatabase db = getReadableDatabase();
        double gen = 0;
        Cursor c = db.rawQuery("SELECT COALESCE(SUM(amount), 0) FROM movements WHERE person_id = ? AND (sub_person_id IS NULL OR sub_person_id = '')", new String[]{personId});
        if (c != null) {
            if (c.moveToFirst()) gen = c.getDouble(0);
            c.close();
        }
        return gen;
    }

    private void computePersonMetrics(Person p, SQLiteDatabase db) {
        double spentMovs = 0;
        Cursor cMov = db.rawQuery("SELECT SUM(amount) FROM movements WHERE person_id = ?", new String[]{p.getId()});
        if (cMov != null) {
            if (cMov.moveToFirst()) spentMovs = cMov.getDouble(0);
            cMov.close();
        }

        double spentMsi = 0;
        Cursor cMsi = db.rawQuery("SELECT total_amount, months, paid_months FROM installment_plans WHERE person_id = ?", new String[]{p.getId()});
        if (cMsi != null) {
            while (cMsi.moveToNext()) {
                double total = cMsi.getDouble(0);
                int months = cMsi.getInt(1);
                int paid = cMsi.getInt(2);
                if (months > 0 && paid < months) {
                    spentMsi += (total / months);
                }
            }
            cMsi.close();
        }
        double spent = spentMovs + spentMsi;

        double delivered = 0;
        Cursor cSa = db.rawQuery(
                "SELECT COALESCE(SUM(s.amount), 0) FROM set_asides s " +
                "LEFT JOIN movements m ON s.movement_id = m.id " +
                "WHERE (s.person_id = ? OR m.person_id = ?)",
                new String[]{p.getId(), p.getId()});
        if (cSa != null) {
            if (cSa.moveToFirst()) delivered = cSa.getDouble(0);
            cSa.close();
        }

        p.setTotalSpent(spent);
        p.setTotalDelivered(delivered);
        p.setNetOwed(Math.max(0, spent - delivered));
    }

    public boolean insertPerson(Person p) {
        if (p.getId() == null) p.setId(UUID.randomUUID().toString());
        p.setUpdatedAt(System.currentTimeMillis());

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", p.getId());
        cv.put("name", p.getName());
        cv.put("parent_id", p.getParentId());
        cv.put("updated_at", p.getUpdatedAt());

        long res = db.insertWithOnConflict("people", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return res != -1;
    }

    public boolean updatePerson(Person p) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", p.getName());
        cv.put("parent_id", p.getParentId());
        cv.put("updated_at", System.currentTimeMillis());
        int count = db.update("people", cv, "id = ?", new String[]{p.getId()});
        return count > 0;
    }

    public boolean deletePerson(String id) {
        SQLiteDatabase db = getWritableDatabase();
        int count = db.delete("people", "id = ? OR parent_id = ?", new String[]{id, id});
        return count > 0;
    }

    public boolean deleteSubPerson(String subPersonId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.putNull("sub_person_id");
        db.update("movements", cv, "sub_person_id = ?", new String[]{subPersonId});
        return db.delete("people", "id = ?", new String[]{subPersonId}) > 0;
    }

    // =========================================================================
    // 8. PRESUPUESTOS Y PROYECCIONES
    // =========================================================================

    public List<Budget> getAllBudgets() {
        List<Budget> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, description, updated_at FROM budgets ORDER BY name ASC", null);
        if (c != null) {
            while (c.moveToNext()) {
                Budget b = new Budget(c.getString(0), c.getString(1), c.getString(2), c.getLong(3));
                b.setItems(getBudgetItems(b.getId(), db));
                list.add(b);
            }
            c.close();
        }
        return list;
    }

    private List<BudgetItem> getBudgetItems(String budgetId, SQLiteDatabase db) {
        List<BudgetItem> items = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT id, budget_id, concept, amount, type, tag, updated_at FROM budget_items WHERE budget_id = ? ORDER BY concept ASC", new String[]{budgetId});
        if (c != null) {
            while (c.moveToNext()) {
                BudgetItem bi = new BudgetItem(
                        c.getString(0),
                        c.getString(1),
                        c.getString(2),
                        c.getDouble(3),
                        c.getString(4),
                        c.getString(5),
                        c.getLong(6)
                );
                items.add(bi);
            }
            c.close();
        }
        return items;
    }

    public boolean insertBudget(Budget b) {
        if (b.getId() == null) b.setId(UUID.randomUUID().toString());
        b.setUpdatedAt(System.currentTimeMillis());

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", b.getId());
        cv.put("name", b.getName());
        cv.put("description", b.getDescription());
        cv.put("updated_at", b.getUpdatedAt());

        long res = db.insertWithOnConflict("budgets", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return res != -1;
    }

    public boolean deleteBudget(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("budget_items", "budget_id = ?", new String[]{id});
            int count = db.delete("budgets", "id = ?", new String[]{id});
            db.setTransactionSuccessful();
            return count > 0;
        } finally {
            db.endTransaction();
        }
    }

    public boolean insertBudgetItem(BudgetItem bi) {
        if (bi.getId() == null) bi.setId(UUID.randomUUID().toString());
        bi.setUpdatedAt(System.currentTimeMillis());

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", bi.getId());
        cv.put("budget_id", bi.getBudgetId());
        cv.put("concept", bi.getConcept());
        cv.put("amount", bi.getAmount());
        cv.put("type", bi.getType());
        cv.put("tag", bi.getTag());
        cv.put("updated_at", bi.getUpdatedAt());

        long res = db.insertWithOnConflict("budget_items", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return res != -1;
    }

    public boolean deleteBudgetItem(String id) {
        SQLiteDatabase db = getWritableDatabase();
        int count = db.delete("budget_items", "id = ?", new String[]{id});
        return count > 0;
    }

    public double getInflationRate() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT value FROM app_config WHERE key = 'inflation_rate'", null);
        double rate = 10.0;
        if (c != null) {
            if (c.moveToFirst()) {
                try { rate = Double.parseDouble(c.getString(0)); } catch (Exception e) {}
            }
            c.close();
        }
        return rate;
    }

    public boolean setInflationRate(double rate) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("key", "inflation_rate");
        cv.put("value", String.valueOf(rate));
        long res = db.insertWithOnConflict("app_config", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return res != -1;
    }

    // =========================================================================
    // 9. EXPORTACIÓN E IMPORTACIÓN INTEROPERABLE CON WEB-APP (JSON PLANO)
    // =========================================================================

    public JSONObject exportBackupJson() throws Exception {
        SQLiteDatabase db = getReadableDatabase();
        JSONObject root = new JSONObject();

        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

        root.put("app", "CardMaster");
        root.put("version", "2.0");
        root.put("format", "cardmaster-json-backup");
        root.put("schema_version", 1);
        root.put("exported_at", now);
        root.put("exported_iso", sdf.format(new Date(now)));

        // Tarjetas
        JSONArray arrCards = new JSONArray();
        Cursor cCards = db.rawQuery("SELECT id, name, credit_limit, cutoff_day, color, logo_base64, updated_at FROM cards", null);
        if (cCards != null) {
            while (cCards.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("id", cCards.getString(0));
                o.put("name", cCards.getString(1));
                o.put("credit_limit", cCards.getDouble(2));
                o.put("cutoff_day", cCards.getInt(3));
                o.put("color", cCards.getString(4));
                o.put("logo_base64", cCards.getString(5));
                o.put("updated_at", cCards.getLong(6));
                arrCards.put(o);
            }
            cCards.close();
        }
        root.put("cards", arrCards);

        // Personas
        JSONArray arrPeople = new JSONArray();
        Cursor cPeople = db.rawQuery("SELECT id, name, parent_id, updated_at FROM people", null);
        if (cPeople != null) {
            while (cPeople.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("id", cPeople.getString(0));
                o.put("name", cPeople.getString(1));
                o.put("parent_id", cPeople.getString(2) != null ? cPeople.getString(2) : JSONObject.NULL);
                o.put("updated_at", cPeople.getLong(3));
                arrPeople.put(o);
            }
            cPeople.close();
        }
        root.put("people", arrPeople);

        // Movimientos
        JSONArray arrMovs = new JSONArray();
        Cursor cMovs = db.rawQuery("SELECT id, concept, amount, date, card_id, person_id, sub_person_id, is_set_aside, updated_at FROM movements", null);
        if (cMovs != null) {
            while (cMovs.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("id", cMovs.getString(0));
                o.put("concept", cMovs.getString(1));
                o.put("amount", cMovs.getDouble(2));
                o.put("date", cMovs.getString(3));
                o.put("card_id", cMovs.getString(4));
                o.put("person_id", cMovs.getString(5));
                o.put("sub_person_id", cMovs.getString(6) != null ? cMovs.getString(6) : JSONObject.NULL);
                o.put("is_set_aside", cMovs.getInt(7));
                o.put("updated_at", cMovs.getLong(8));
                arrMovs.put(o);
            }
            cMovs.close();
        }
        root.put("movements", arrMovs);

        // Meses Sin Intereses
        JSONArray arrMsi = new JSONArray();
        Cursor cMsi = db.rawQuery("SELECT id, concept, total_amount, months, start_date, card_id, person_id, paid_months, updated_at FROM installment_plans", null);
        if (cMsi != null) {
            while (cMsi.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("id", cMsi.getString(0));
                o.put("concept", cMsi.getString(1));
                o.put("total_amount", cMsi.getDouble(2));
                o.put("months", cMsi.getInt(3));
                o.put("start_date", cMsi.getString(4));
                o.put("card_id", cMsi.getString(5));
                o.put("person_id", cMsi.getString(6));
                o.put("paid_months", cMsi.getInt(7));
                o.put("updated_at", cMsi.getLong(8));
                arrMsi.put(o);
            }
            cMsi.close();
        }
        root.put("installment_plans", arrMsi);

        // Apartados
        JSONArray arrSa = new JSONArray();
        Cursor cSa = db.rawQuery("SELECT id, card_id, person_id, movement_id, amount, note, date, fund_type, is_paid, paid_amount, status, updated_at FROM set_asides", null);
        if (cSa != null) {
            while (cSa.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("id", cSa.getString(0));
                o.put("card_id", cSa.getString(1));
                o.put("person_id", cSa.getString(2));
                o.put("movement_id", cSa.getString(3));
                o.put("amount", cSa.getDouble(4));
                o.put("note", cSa.getString(5));
                o.put("date", cSa.getString(6));
                o.put("fund_type", cSa.getString(7));
                o.put("is_paid", cSa.getInt(8));
                o.put("paid_amount", cSa.getDouble(9));
                o.put("status", cSa.getString(10));
                o.put("updated_at", cSa.getLong(11));
                arrSa.put(o);
            }
            cSa.close();
        }
        root.put("set_asides", arrSa);

        // Presupuestos
        JSONArray arrBudgets = new JSONArray();
        Cursor cBudgets = db.rawQuery("SELECT id, name, description, updated_at FROM budgets", null);
        if (cBudgets != null) {
            while (cBudgets.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("id", cBudgets.getString(0));
                o.put("name", cBudgets.getString(1));
                o.put("description", cBudgets.getString(2));
                o.put("updated_at", cBudgets.getLong(3));
                arrBudgets.put(o);
            }
            cBudgets.close();
        }
        root.put("budgets", arrBudgets);

        // Conceptos de presupuesto
        JSONArray arrBi = new JSONArray();
        Cursor cBi = db.rawQuery("SELECT id, budget_id, concept, amount, type, tag, updated_at FROM budget_items", null);
        if (cBi != null) {
            while (cBi.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("id", cBi.getString(0));
                o.put("budget_id", cBi.getString(1));
                o.put("concept", cBi.getString(2));
                o.put("amount", cBi.getDouble(3));
                o.put("type", cBi.getString(4));
                o.put("tag", cBi.getString(5));
                o.put("updated_at", cBi.getLong(6));
                arrBi.put(o);
            }
            cBi.close();
        }
        root.put("budget_items", arrBi);

        // Configuración
        JSONObject oConfig = new JSONObject();
        Cursor cCfg = db.rawQuery("SELECT key, value FROM app_config", null);
        if (cCfg != null) {
            while (cCfg.moveToNext()) {
                oConfig.put(cCfg.getString(0), cCfg.getString(1));
            }
            cCfg.close();
        }
        root.put("config", oConfig);

        return root;
    }

    public boolean importBackupJson(String jsonString) throws Exception {
        if (jsonString == null || jsonString.trim().isEmpty()) return false;
        JSONObject root = new JSONObject(jsonString);

        boolean hasCards = root.has("cards");
        boolean hasMovs = root.has("movements");
        boolean hasPeople = root.has("people");
        boolean hasSig = "CardMaster".equals(root.optString("app")) || "cardmaster-json-backup".equals(root.optString("format"));

        if (!hasSig && !hasCards && !hasMovs && !hasPeople) {
            throw new Exception("El archivo seleccionado no corresponde a un respaldo válido de CardMaster");
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Preservar credenciales maestras actuales del dispositivo
            Cursor cPwd = db.rawQuery("SELECT key, value FROM app_config WHERE key IN ('master_pwd_hash', 'master_pwd_salt')", null);
            java.util.Map<String, String> preservedAuth = new java.util.HashMap<>();
            if (cPwd != null) {
                while (cPwd.moveToNext()) {
                    preservedAuth.put(cPwd.getString(0), cPwd.getString(1));
                }
                cPwd.close();
            }

            db.execSQL("DELETE FROM movements");
            db.execSQL("DELETE FROM installment_plans");
            db.execSQL("DELETE FROM set_asides");
            db.execSQL("DELETE FROM budget_items");
            db.execSQL("DELETE FROM budgets");
            db.execSQL("DELETE FROM cards");
            db.execSQL("DELETE FROM people");
            db.execSQL("DELETE FROM app_config");

            long now = System.currentTimeMillis();

            if (root.has("cards")) {
                JSONArray arr = root.getJSONArray("cards");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    ContentValues cv = new ContentValues();
                    cv.put("id", o.optString("id", UUID.randomUUID().toString()));
                    cv.put("name", o.optString("name", "Tarjeta"));
                    cv.put("credit_limit", o.optDouble("credit_limit", 0));
                    cv.put("cutoff_day", o.optInt("cutoff_day", 1));
                    cv.put("color", o.optString("color", "#0284c7"));
                    cv.put("logo_base64", o.optString("logo_base64", null));
                    cv.put("updated_at", o.optLong("updated_at", now));
                    db.insert("cards", null, cv);
                }
            }

            if (root.has("people")) {
                JSONArray arr = root.getJSONArray("people");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    ContentValues cv = new ContentValues();
                    cv.put("id", o.optString("id", UUID.randomUUID().toString()));
                    cv.put("name", o.optString("name", "Personal"));
                    cv.put("parent_id", o.has("parent_id") && !o.isNull("parent_id") ? o.optString("parent_id", null) : null);
                    cv.put("updated_at", o.optLong("updated_at", now));
                    db.insert("people", null, cv);
                }
            }

            if (root.has("movements")) {
                JSONArray arr = root.getJSONArray("movements");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    ContentValues cv = new ContentValues();
                    cv.put("id", o.optString("id", UUID.randomUUID().toString()));
                    cv.put("concept", o.optString("concept", "Gasto"));
                    cv.put("amount", o.optDouble("amount", 0));
                    cv.put("date", o.optString("date", ""));
                    cv.put("card_id", o.optString("card_id", ""));
                    cv.put("person_id", o.optString("person_id", ""));
                    cv.put("sub_person_id", o.has("sub_person_id") && !o.isNull("sub_person_id") ? o.optString("sub_person_id", null) : null);
                    cv.put("is_set_aside", o.optInt("is_set_aside", 0));
                    cv.put("updated_at", o.optLong("updated_at", now));
                    db.insert("movements", null, cv);
                }
            }

            if (root.has("installment_plans")) {
                JSONArray arr = root.getJSONArray("installment_plans");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    ContentValues cv = new ContentValues();
                    cv.put("id", o.optString("id", UUID.randomUUID().toString()));
                    cv.put("concept", o.optString("concept", "MSI"));
                    cv.put("total_amount", o.optDouble("total_amount", 0));
                    cv.put("months", o.optInt("months", 12));
                    cv.put("start_date", o.optString("start_date", ""));
                    cv.put("card_id", o.optString("card_id", ""));
                    cv.put("person_id", o.optString("person_id", ""));
                    cv.put("paid_months", o.optInt("paid_months", 0));
                    cv.put("updated_at", o.optLong("updated_at", now));
                    db.insert("installment_plans", null, cv);
                }
            }

            if (root.has("set_asides")) {
                JSONArray arr = root.getJSONArray("set_asides");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    ContentValues cv = new ContentValues();
                    cv.put("id", o.optString("id", UUID.randomUUID().toString()));
                    cv.put("card_id", o.optString("card_id", ""));
                    cv.put("person_id", o.optString("person_id", ""));
                    cv.put("movement_id", o.optString("movement_id", null));
                    cv.put("amount", o.optDouble("amount", 0));
                    cv.put("note", o.optString("note", ""));
                    cv.put("date", o.optString("date", ""));
                    cv.put("fund_type", o.optString("fund_type", "Efectivo"));
                    cv.put("is_paid", o.optInt("is_paid", 0));
                    cv.put("paid_amount", o.optDouble("paid_amount", 0));
                    cv.put("status", o.optString("status", "apartado"));
                    cv.put("updated_at", o.optLong("updated_at", now));
                    db.insert("set_asides", null, cv);
                }
            }

            if (root.has("budgets")) {
                JSONArray arr = root.getJSONArray("budgets");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    ContentValues cv = new ContentValues();
                    cv.put("id", o.optString("id", UUID.randomUUID().toString()));
                    cv.put("name", o.optString("name", "Presupuesto"));
                    cv.put("description", o.optString("description", ""));
                    cv.put("updated_at", o.optLong("updated_at", now));
                    db.insert("budgets", null, cv);
                }
            }

            if (root.has("budget_items")) {
                JSONArray arr = root.getJSONArray("budget_items");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    ContentValues cv = new ContentValues();
                    cv.put("id", o.optString("id", UUID.randomUUID().toString()));
                    cv.put("budget_id", o.optString("budget_id", ""));
                    cv.put("concept", o.optString("concept", "Concepto"));
                    cv.put("amount", o.optDouble("amount", 0));
                    cv.put("type", o.optString("type", "fijo"));
                    cv.put("tag", o.optString("tag", ""));
                    cv.put("updated_at", o.optLong("updated_at", now));
                    db.insert("budget_items", null, cv);
                }
            }

            if (root.has("config")) {
                JSONObject o = root.getJSONObject("config");
                java.util.Iterator<String> keys = o.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    ContentValues cv = new ContentValues();
                    cv.put("key", k);
                    cv.put("value", o.optString(k, ""));
                    db.insert("app_config", null, cv);
                }
            }

            // Si la bóveda ya tenía contraseña configurada en este dispositivo, mantenerla intacta
            if (!preservedAuth.isEmpty()) {
                for (java.util.Map.Entry<String, String> entry : preservedAuth.entrySet()) {
                    ContentValues cvAuth = new ContentValues();
                    cvAuth.put("key", entry.getKey());
                    cvAuth.put("value", entry.getValue());
                    db.insertWithOnConflict("app_config", null, cvAuth, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            // Asegurar persona Personal e inflación
            Cursor cCheckP = db.rawQuery("SELECT id FROM people WHERE name = 'Personal'", null);
            if (cCheckP != null) {
                if (!cCheckP.moveToFirst()) {
                    ContentValues cv = new ContentValues();
                    cv.put("id", UUID.randomUUID().toString());
                    cv.put("name", "Personal");
                    cv.put("updated_at", now);
                    db.insert("people", null, cv);
                }
                cCheckP.close();
            }

            Cursor cCheckInf = db.rawQuery("SELECT value FROM app_config WHERE key = 'inflation_rate'", null);
            if (cCheckInf != null) {
                if (!cCheckInf.moveToFirst()) {
                    ContentValues cv = new ContentValues();
                    cv.put("key", "inflation_rate");
                    cv.put("value", "10");
                    db.insert("app_config", null, cv);
                }
                cCheckInf.close();
            }

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }
}
