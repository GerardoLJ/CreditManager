const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const sqlite3 = require('sqlite3').verbose();
const crypto = require('crypto');

const app = express();
const PORT = process.env.PORT || 3000;

// Configuración de almacenamiento físico de datos (admite Docker o ruta personalizada)
const DATA_DIR = process.env.DATA_DIR ? path.resolve(process.env.DATA_DIR) : __dirname;
if (!fs.existsSync(DATA_DIR)) {
  try {
    fs.mkdirSync(DATA_DIR, { recursive: true });
  } catch (err) {
    console.error("Error creando directorio de datos:", err);
  }
}
const DB_PATH = process.env.DB_PATH ? path.resolve(process.env.DB_PATH) : path.join(DATA_DIR, 'tarjetas.db');

// Si se utiliza una carpeta de datos externa y aún no existe tarjetas.db allí, transferir copia inicial si existe
if (!fs.existsSync(DB_PATH) && fs.existsSync(path.join(__dirname, 'tarjetas.db')) && path.join(__dirname, 'tarjetas.db') !== DB_PATH) {
  try {
    fs.copyFileSync(path.join(__dirname, 'tarjetas.db'), DB_PATH);
    console.log(`📋 Copia inicial de tarjetas.db transferida a: ${DB_PATH}`);
  } catch (err) {
    console.warn("Aviso copiando base de datos inicial:", err.message);
  }
}

let db = new sqlite3.Database(DB_PATH, (err) => {
  if (err) {
    console.error(`❌ Error abriendo SQLite en ${DB_PATH}:`, err.message);
  } else {
    console.log(`📁 Base de datos activa en: ${DB_PATH}`);
  }
});

app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.static(__dirname));
app.use(express.static(path.join(__dirname, 'public')));

// Inicializar tablas
function initDbSchema() {
  db.serialize(() => {
    db.run(`CREATE TABLE IF NOT EXISTS cards (
      id TEXT PRIMARY KEY, name TEXT NOT NULL, credit_limit REAL NOT NULL,
      cutoff_day INTEGER NOT NULL, color TEXT, logo_base64 TEXT, updated_at INTEGER
    )`);
    db.run(`CREATE TABLE IF NOT EXISTS people (
      id TEXT PRIMARY KEY, name TEXT NOT NULL UNIQUE, updated_at INTEGER
    )`);
    db.run(`CREATE TABLE IF NOT EXISTS movements (
      id TEXT PRIMARY KEY, concept TEXT NOT NULL, amount REAL NOT NULL,
      date TEXT NOT NULL, card_id TEXT NOT NULL, person_id TEXT NOT NULL,
      is_set_aside INTEGER DEFAULT 0, updated_at INTEGER
    )`);
    db.run(`CREATE TABLE IF NOT EXISTS installment_plans (
      id TEXT PRIMARY KEY, concept TEXT NOT NULL, total_amount REAL NOT NULL,
      months INTEGER NOT NULL, start_date TEXT NOT NULL, card_id TEXT NOT NULL,
      person_id TEXT NOT NULL, paid_months INTEGER DEFAULT 0, updated_at INTEGER
    )`);
    db.run(`CREATE TABLE IF NOT EXISTS set_asides (
      id TEXT PRIMARY KEY, card_id TEXT NOT NULL, person_id TEXT NOT NULL,
      movement_id TEXT, amount REAL NOT NULL, note TEXT, date TEXT NOT NULL, updated_at INTEGER
    )`);
    db.run(`CREATE TABLE IF NOT EXISTS budgets (
      id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT, updated_at INTEGER
    )`);
    db.run(`CREATE TABLE IF NOT EXISTS budget_items (
      id TEXT PRIMARY KEY, budget_id TEXT NOT NULL, concept TEXT NOT NULL,
      amount REAL NOT NULL, type TEXT NOT NULL, tag TEXT, updated_at INTEGER
    )`);
    
    db.run("ALTER TABLE set_asides ADD COLUMN fund_type TEXT DEFAULT 'Efectivo'", (err) => {
      // Si ya existe la columna, no hace nada
    });
    db.run("ALTER TABLE set_asides ADD COLUMN is_paid INTEGER DEFAULT 0", (err) => {});
    db.run("ALTER TABLE set_asides ADD COLUMN paid_amount REAL DEFAULT 0", (err) => {});
    db.run("ALTER TABLE set_asides ADD COLUMN status TEXT DEFAULT 'apartado'", (err) => {});
    db.run("ALTER TABLE people ADD COLUMN parent_id TEXT", (err) => {});
    db.run("ALTER TABLE movements ADD COLUMN sub_person_id TEXT", (err) => {});
    db.run("ALTER TABLE installment_plans ADD COLUMN sub_person_id TEXT", (err) => {});

    db.run(`CREATE TABLE IF NOT EXISTS app_config (key TEXT PRIMARY KEY, value TEXT)`);

    db.get("SELECT value FROM app_config WHERE key = 'seed_applied'", (err, cfgRow) => {
      if (!err && cfgRow) {
        return;
      }
      db.get("SELECT COUNT(*) as count FROM cards", (err2, cardsRow) => {
        if (!err2 && cardsRow && cardsRow.count > 0) {
          db.run("INSERT OR REPLACE INTO app_config (key, value) VALUES ('seed_applied', '1')");
          return;
        }
        seedInitialDatabase(db);
      });
    });
  });
}

function seedInitialDatabase(database) {
  const backupPath = path.join(__dirname, 'cardmaster_backup.json');
  let dump = null;
  if (fs.existsSync(backupPath)) {
    try {
      dump = JSON.parse(fs.readFileSync(backupPath, 'utf8'));
    } catch (e) {
      console.warn("Aviso leyendo cardmaster_backup.json:", e.message);
    }
  }

  if (!dump) {
    dump = {
      cards: [
        { id: "card-bbva-platinum", name: "BBVA Platinum", credit_limit: 509700, cutoff_day: 16, color: "#004481" },
        { id: "card-stori", name: "Stori", credit_limit: 20000, cutoff_day: 12, color: "#00a86b" },
        { id: "card-banamex-costco", name: "Banamex Costco", credit_limit: 58000, cutoff_day: 8, color: "#0284c7" },
        { id: "card-hsbc-viva", name: "HSBC VIVA", credit_limit: 150000, cutoff_day: 11, color: "#db0011" },
        { id: "card-nu", name: "NU", credit_limit: 4000, cutoff_day: 23, color: "#820ad1" },
        { id: "card-rappi-card", name: "Rappi Card", credit_limit: 8000, cutoff_day: 21, color: "#ff441f" }
      ],
      people: [
        { id: "person-personal", name: "Personal" },
        { id: "person-plj", name: "PLJ" },
        { id: "person-blj", name: "BLJ" },
        { id: "person-gjg", name: "GJG" },
        { id: "person-elh", name: "ELH" },
        { id: "person-jess", name: "Jess" },
        { id: "person-gaby", name: "Gaby" }
      ],
      movements: [],
      installment_plans: [],
      set_asides: [],
      budgets: [],
      budget_items: [],
      config: { inflation_rate: "4.5" }
    };
  }

  const now = Date.now();
  database.serialize(() => {
    if (Array.isArray(dump.cards)) {
      const stmt = database.prepare("INSERT OR REPLACE INTO cards (id, name, credit_limit, cutoff_day, color, logo_base64, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)");
      dump.cards.forEach(c => stmt.run(c.id, c.name, c.credit_limit, c.cutoff_day, c.color || '#004481', c.logo_base64 || null, c.updated_at || now));
      stmt.finalize();
    }
    if (Array.isArray(dump.people)) {
      const stmt = database.prepare("INSERT OR REPLACE INTO people (id, name, updated_at) VALUES (?, ?, ?)");
      dump.people.forEach(p => stmt.run(p.id, p.name, p.updated_at || now));
      stmt.finalize();
    }
    if (Array.isArray(dump.movements)) {
      const stmt = database.prepare("INSERT OR REPLACE INTO movements (id, concept, amount, date, card_id, person_id, is_set_aside, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
      dump.movements.forEach(m => stmt.run(m.id, m.concept, m.amount, m.date, m.card_id, m.person_id, m.is_set_aside || 0, m.updated_at || now));
      stmt.finalize();
    }
    if (Array.isArray(dump.set_asides)) {
      const stmt = database.prepare("INSERT OR REPLACE INTO set_asides (id, card_id, person_id, movement_id, amount, note, date, fund_type, is_paid, paid_amount, status, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
      dump.set_asides.forEach(s => stmt.run(s.id, s.card_id, s.person_id, s.movement_id || null, s.amount, s.note || '', s.date, s.fund_type || 'Efectivo', s.is_paid || 0, s.paid_amount || 0, s.status || 'apartado', s.updated_at || now));
      stmt.finalize();
    }
    if (dump.config) {
      const stmt = database.prepare("INSERT OR REPLACE INTO app_config (key, value) VALUES (?, ?)");
      Object.entries(dump.config).forEach(([k, v]) => stmt.run(k, String(v)));
      stmt.finalize();
    }
    database.run("INSERT OR REPLACE INTO app_config (key, value) VALUES ('seed_applied', '1')");
    console.log("🌱 Base de datos SQLite inicializada exitosamente con datos precargados.");
  });
}

initDbSchema();

function hashPassword(pwd, salt) {
  return crypto.pbkdf2Sync(pwd, salt, 100000, 32, 'sha256').toString('hex');
}

function hashPasswordHex(pwd, salt) {
  try {
    return crypto.pbkdf2Sync(pwd, Buffer.from(salt, 'hex'), 100000, 32, 'sha256').toString('hex');
  } catch (e) {
    return null;
  }
}

function checkPassword(pwd, expectedHash, salt) {
  if (hashPassword(pwd, salt) === expectedHash) return true;
  if (hashPasswordHex(pwd, salt) === expectedHash) return true;
  return false;
}

// Auth
app.get('/api/auth/status', (req, res) => {
  db.get("SELECT value FROM app_config WHERE key = 'master_pwd_hash'", (err, row) => {
    res.json({ isConfigured: Boolean(row && row.value) });
  });
});

app.post('/api/auth/setup', (req, res) => {
  const { password } = req.body;
  const salt = crypto.randomBytes(16).toString('hex');
  const hash = hashPassword(password, salt);
  db.serialize(() => {
    db.run("INSERT OR REPLACE INTO app_config (key, value) VALUES ('master_pwd_hash', ?)", [hash]);
    db.run("INSERT OR REPLACE INTO app_config (key, value) VALUES ('master_pwd_salt', ?)", [salt]);
    res.json({ success: true });
  });
});

app.post('/api/auth/login', (req, res) => {
  const { password } = req.body;
  db.all("SELECT key, value FROM app_config WHERE key IN ('master_pwd_hash', 'master_pwd_salt')", (err, rows) => {
    if (!rows) return res.status(500).json({ error: 'Error interno de base de datos' });
    const hashRow = rows.find(r => r.key === 'master_pwd_hash');
    const saltRow = rows.find(r => r.key === 'master_pwd_salt');
    if (!hashRow || !saltRow) return res.status(400).json({ error: 'No configurado' });
    if (checkPassword(password, hashRow.value, saltRow.value)) res.json({ success: true });
    else res.status(401).json({ error: 'Contraseña incorrecta' });
  });
});

app.post('/api/auth/change-pwd', (req, res) => {
  const { oldPassword, newPassword } = req.body;
  db.all("SELECT key, value FROM app_config WHERE key IN ('master_pwd_hash', 'master_pwd_salt')", (err, rows) => {
    const hashRow = rows.find(r => r.key === 'master_pwd_hash');
    const saltRow = rows.find(r => r.key === 'master_pwd_salt');
    if (!checkPassword(oldPassword, hashRow.value, saltRow.value)) {
      return res.status(401).json({ error: 'Contraseña actual incorrecta' });
    }
    const newSalt = crypto.randomBytes(16).toString('hex');
    const newHash = hashPassword(newPassword, newSalt);
    db.serialize(() => {
      db.run("INSERT OR REPLACE INTO app_config (key, value) VALUES ('master_pwd_hash', ?)", [newHash]);
      db.run("INSERT OR REPLACE INTO app_config (key, value) VALUES ('master_pwd_salt', ?)", [newSalt]);
      res.json({ success: true });
    });
  });
});

// Tarjetas
app.get('/api/cards', (req, res) => {
  db.all("SELECT * FROM cards ORDER BY name ASC", (err, rows) => res.json(rows || []));
});

app.post('/api/cards', (req, res) => {
  const { id, name, credit_limit, cutoff_day, color, logo_base64 } = req.body;
  const cardId = id || crypto.randomUUID();
  db.run(`INSERT INTO cards (id, name, credit_limit, cutoff_day, color, logo_base64, updated_at)
          VALUES (?, ?, ?, ?, ?, ?, ?)
          ON CONFLICT(id) DO UPDATE SET
          name=excluded.name, credit_limit=excluded.credit_limit, cutoff_day=excluded.cutoff_day,
          color=excluded.color, logo_base64=excluded.logo_base64, updated_at=excluded.updated_at`,
    [cardId, name, credit_limit, cutoff_day, color || '#1e293b', logo_base64 || null, Date.now()],
    () => res.json({ success: true, id: cardId })
  );
});

app.post('/api/cards/:id/delete', (req, res) => {
  const { password } = req.body;
  db.all("SELECT key, value FROM app_config WHERE key IN ('master_pwd_hash', 'master_pwd_salt')", (err, rows) => {
    if (!rows) return res.status(500).json({ error: 'Error interno de base de datos' });
    const hashRow = rows.find(r => r.key === 'master_pwd_hash');
    const saltRow = rows.find(r => r.key === 'master_pwd_salt');
    if (!hashRow || !saltRow || !checkPassword(password, hashRow.value, saltRow.value)) {
      return res.status(401).json({ error: 'Contraseña incorrecta. No se eliminó la tarjeta.' });
    }
    db.serialize(() => {
      db.run("DELETE FROM movements WHERE card_id = ?", [req.params.id]);
      db.run("DELETE FROM installment_plans WHERE card_id = ?", [req.params.id]);
      db.run("DELETE FROM set_asides WHERE card_id = ?", [req.params.id]);
      db.run("DELETE FROM cards WHERE id = ?", [req.params.id], () => res.json({ success: true }));
    });
  });
});

// Personas
app.get('/api/people', (req, res) => {
  db.all("SELECT * FROM people ORDER BY name ASC", (err, rows) => res.json(rows || []));
});

app.post('/api/people', (req, res) => {
  const id = crypto.randomUUID();
  const parentId = req.body.parent_id || null;
  db.run("INSERT INTO people (id, name, parent_id, updated_at) VALUES (?, ?, ?, ?)", [id, req.body.name.trim(), parentId, Date.now()], (err) => {
    if (err) return res.status(400).json({ error: 'Ya existe esa persona o subetiqueta' });
    res.json({ success: true, id });
  });
});

app.delete('/api/people/:id', (req, res) => {
  const pId = req.params.id;
  db.serialize(() => {
    db.run("DELETE FROM people WHERE parent_id = ?", [pId]);
    db.run("DELETE FROM people WHERE id = ?", [pId], () => res.json({ success: true }));
  });
});

// Movimientos
app.get('/api/movements', (req, res) => {
  db.all(`SELECT m.*, COALESCE(c.name, 'Desconocida') as card_name, COALESCE(p.name, 'Desconocido') as person_name,
                 COALESCE(sp.name, '') as sub_person_name
          FROM movements m
          LEFT JOIN cards c ON m.card_id = c.id
          LEFT JOIN people p ON m.person_id = p.id
          LEFT JOIN people sp ON m.sub_person_id = sp.id
          ORDER BY m.date ASC`, (err, rows) => {
    if (err) return res.status(500).json({ error: err.message });
    db.all(`SELECT * FROM set_asides`, (errSa, asides) => {
      if (errSa) return res.status(500).json({ error: errSa.message });

      const directAsideMap = {};
      const directPaidMap = {};
      const unallocatedByPersonAndCard = {};
      const unallocatedPaidByPersonAndCard = {};

      (asides || []).forEach(s => {
        const amt = parseFloat(s.amount) || 0;
        const pAmt = parseFloat(s.paid_amount) || (s.is_paid ? amt : 0);
        if (s.movement_id) {
          const mId = String(s.movement_id);
          directAsideMap[mId] = (directAsideMap[mId] || 0) + amt;
          directPaidMap[mId] = (directPaidMap[mId] || 0) + pAmt;
        } else {
          const key = `${s.person_id}_${s.card_id}`;
          unallocatedByPersonAndCard[key] = (unallocatedByPersonAndCard[key] || 0) + amt;
          unallocatedPaidByPersonAndCard[key] = (unallocatedPaidByPersonAndCard[key] || 0) + pAmt;
        }
      });

      const formatted = (rows || []).map(m => {
        let directAside = directAsideMap[String(m.id)] || 0;
        let directPaid = directPaidMap[String(m.id)] || 0;
        let totalSetAside = directAside;
        let totalPaid = directPaid;

        const key = `${m.person_id}_${m.card_id}`;
        let remAside = Math.max(0, m.amount - totalSetAside);
        if (remAside > 0.001 && unallocatedByPersonAndCard[key] > 0.001) {
          const alloc = Math.min(remAside, unallocatedByPersonAndCard[key]);
          totalSetAside += alloc;
          unallocatedByPersonAndCard[key] -= alloc;
        }

        let remPaid = Math.max(0, m.amount - totalPaid);
        if (remPaid > 0.001 && unallocatedPaidByPersonAndCard[key] > 0.001) {
          const alloc = Math.min(remPaid, unallocatedPaidByPersonAndCard[key]);
          totalPaid += alloc;
          unallocatedPaidByPersonAndCard[key] -= alloc;
        }

        let status = 'gastado';
        if (totalPaid >= m.amount && m.amount > 0) {
          status = 'pagado';
        } else if (totalPaid > 0) {
          status = 'pago_parcial';
        } else if (totalSetAside >= m.amount && m.amount > 0) {
          status = 'apartado';
        } else if (totalSetAside > 0) {
          status = 'apartado_parcial';
        }

        return {
          ...m,
          total_set_aside: totalSetAside,
          total_paid: totalPaid,
          status
        };
      });

      formatted.sort((a, b) => (b.date || '').localeCompare(a.date || ''));
      res.json(formatted);
    });
  });
});

app.post('/api/movements', (req, res) => {
  const { concept, amount, date, card_id, person_id, sub_person_id, is_msi, msi_months } = req.body;
  const now = Date.now();
  if (is_msi) {
    const msiId = crypto.randomUUID();
    db.run(`INSERT INTO installment_plans (id, concept, total_amount, months, start_date, card_id, person_id, sub_person_id, paid_months, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)`, [msiId, concept, amount, msi_months || 12, date, card_id, person_id, sub_person_id || null, now],
      () => res.json({ success: true, msiId }));
  } else {
    const movId = crypto.randomUUID();
    db.run(`INSERT INTO movements (id, concept, amount, date, card_id, person_id, sub_person_id, is_set_aside, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?)`, [movId, concept, amount, date, card_id, person_id, sub_person_id || null, now],
      () => res.json({ success: true, id: movId }));
  }
});

app.put('/api/movements/:id', (req, res) => {
  const { concept, amount, date, card_id, person_id, sub_person_id } = req.body;
  const now = Date.now();
  db.run(`UPDATE movements 
          SET concept = ?, amount = ?, date = ?, card_id = ?, person_id = ?, sub_person_id = ?, updated_at = ?
          WHERE id = ?`,
    [concept, amount, date, card_id, person_id, sub_person_id || null, now, req.params.id],
    function(err) {
      if (err) return res.status(500).json({ error: err.message });
      res.json({ success: true, changes: this.changes });
    }
  );
});

app.post('/api/movements/:id/update', (req, res) => {
  const { concept, amount, date, card_id, person_id, sub_person_id } = req.body;
  const now = Date.now();
  db.run(`UPDATE movements 
          SET concept = ?, amount = ?, date = ?, card_id = ?, person_id = ?, sub_person_id = ?, updated_at = ?
          WHERE id = ?`,
    [concept, amount, date, card_id, person_id, sub_person_id || null, now, req.params.id],
    function(err) {
      if (err) return res.status(500).json({ error: err.message });
      res.json({ success: true, changes: this.changes });
    }
  );
});

app.delete('/api/movements/:id', (req, res) => {
  const movId = req.params.id;
  db.serialize(() => {
    db.run("DELETE FROM set_asides WHERE movement_id = ?", [movId]);
    db.run("DELETE FROM movements WHERE id = ?", [movId], () => res.json({ success: true }));
  });
});

// Apartados
app.post('/api/set-asides', (req, res) => {
  const { card_id, person_id, movement_id, amount, fund_type, note, date, paid_amount, is_paid, status } = req.body;
  const id = crypto.randomUUID();
  const now = Date.now();
  const typeVal = (fund_type === 'Débito' || fund_type === 'Debito') ? 'Débito' : 'Efectivo';
  const numAmount = parseFloat(amount) || 0;
  const numPaid = parseFloat(paid_amount) || 0;
  const paidFlag = is_paid ? 1 : (numPaid >= numAmount && numAmount > 0 ? 1 : 0);
  const statusVal = status || (paidFlag ? 'pagado' : (numPaid > 0 ? 'pago_parcial' : 'apartado'));

  db.serialize(() => {
    db.run(`INSERT INTO set_asides (id, card_id, person_id, movement_id, amount, fund_type, note, date, is_paid, paid_amount, status, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [id, card_id, person_id, movement_id || null, numAmount, typeVal, note || '', date || new Date().toISOString().split('T')[0], paidFlag, numPaid, statusVal, now],
      function(err) {
        if (err) return res.status(500).json({ error: err.message });
        if (movement_id) {
          db.run("UPDATE movements SET is_set_aside = 1, updated_at = ? WHERE id = ?", [now, movement_id]);
        }
        res.json({ success: true, id });
      }
    );
  });
});

app.get('/api/set-asides', (req, res) => {
  db.all(`SELECT s.*, COALESCE(c.name, 'Desconocida') as card_name, COALESCE(p.name, 'Desconocido') as person_name FROM set_asides s
          LEFT JOIN cards c ON s.card_id = c.id LEFT JOIN people p ON s.person_id = p.id
          ORDER BY s.date DESC`, (err, rows) => {
    if (err) return res.status(500).json({ error: err.message });
    const formatted = (rows || []).map(s => {
      const paidAmount = Number(s.paid_amount) || (s.is_paid ? s.amount : 0);
      let status = s.status || 'apartado';
      if (paidAmount >= s.amount && s.amount > 0) {
        status = 'pagado';
      } else if (paidAmount > 0) {
        status = 'pago_parcial';
      } else {
        status = 'apartado';
      }
      return {
        ...s,
        paid_amount: paidAmount,
        is_paid: s.is_paid ? 1 : 0,
        status
      };
    });
    res.json(formatted);
  });
});

app.put('/api/set-asides/:id', (req, res) => {
  const id = req.params.id;
  const { card_id, person_id, movement_id, amount, fund_type, note, date, is_paid, paid_amount, status } = req.body;
  const numAmount = parseFloat(amount) || 0;
  const numPaid = parseFloat(paid_amount) || 0;
  const paidFlag = is_paid ? 1 : (numPaid >= numAmount && numAmount > 0 ? 1 : 0);
  const statusVal = status || (paidFlag ? 'pagado' : (numPaid > 0 ? 'pago_parcial' : 'apartado'));
  const typeVal = (fund_type === 'Débito' || fund_type === 'Debito') ? 'Débito' : 'Efectivo';
  const now = Date.now();

  db.run(`UPDATE set_asides SET card_id = ?, person_id = ?, movement_id = ?, amount = ?, fund_type = ?, note = ?, date = ?, is_paid = ?, paid_amount = ?, status = ?, updated_at = ? WHERE id = ?`,
    [card_id, person_id, movement_id || null, numAmount, typeVal, note || '', date || new Date().toISOString().split('T')[0], paidFlag, numPaid, statusVal, now, id],
    function(err) {
      if (err) return res.status(500).json({ error: err.message });
      res.json({ success: true, id });
    }
  );
});

app.delete('/api/set-asides/:id', (req, res) => {
  const id = req.params.id;
  db.get("SELECT movement_id FROM set_asides WHERE id = ?", [id], (err, row) => {
    const movId = row ? row.movement_id : null;
    db.run("DELETE FROM set_asides WHERE id = ?", [id], function(delErr) {
      if (delErr) return res.status(500).json({ error: delErr.message });
      if (movId) {
        db.get("SELECT COUNT(*) as count FROM set_asides WHERE movement_id = ?", [movId], (mErr, mRow) => {
          const hasRemaining = mRow && mRow.count > 0;
          db.run("UPDATE movements SET is_set_aside = ?, updated_at = ? WHERE id = ?",
            [hasRemaining ? 1 : 0, Date.now(), movId]);
        });
      }
      res.json({ success: true });
    });
  });
});

app.post('/api/set-asides/:id/pay', (req, res) => {
  const id = req.params.id;
  const { paid_amount } = req.body;
  db.get("SELECT * FROM set_asides WHERE id = ? OR CAST(id AS TEXT) = ?", [id, String(id)], (err, row) => {
    if (err || !row) return res.status(404).json({ error: 'Apartado no encontrado' });
    const targetAmount = (paid_amount !== undefined && paid_amount !== null && !isNaN(parseFloat(paid_amount)))
      ? Math.min(row.amount, parseFloat(paid_amount))
      : row.amount;
    const isFull = targetAmount >= (row.amount - 0.001);
    const now = Date.now();

    if (isFull) {
      // 100% liquidado: eliminar movimiento y eliminar apartado
      db.serialize(() => {
        if (row.movement_id) {
          db.run("DELETE FROM movements WHERE id = ? OR CAST(id AS TEXT) = ?", [row.movement_id, String(row.movement_id)]);
        } else {
          db.run("DELETE FROM movements WHERE id IN (SELECT id FROM movements WHERE (card_id = ? OR CAST(card_id AS TEXT) = ?) AND (person_id = ? OR CAST(person_id AS TEXT) = ?) AND ABS(amount - ?) < 0.01 LIMIT 1)", [row.card_id, String(row.card_id), row.person_id, String(row.person_id), row.amount]);
        }
        db.run("DELETE FROM set_asides WHERE id = ? OR CAST(id AS TEXT) = ?", [id, String(id)], function(delErr) {
          if (delErr) return res.status(500).json({ error: delErr.message });
          res.json({ success: true, fully_settled: true, amount_paid: targetAmount });
        });
      });
    } else {
      // Pago parcial: descontar del apartado y del movimiento
      const newAmount = Math.max(0, row.amount - targetAmount);
      db.serialize(() => {
        if (row.movement_id) {
          db.run("UPDATE movements SET amount = MAX(0, amount - ?), updated_at = ? WHERE id = ? OR CAST(id AS TEXT) = ?", [targetAmount, now, row.movement_id, String(row.movement_id)]);
        } else {
          db.run("UPDATE movements SET amount = MAX(0, amount - ?), updated_at = ? WHERE id IN (SELECT id FROM movements WHERE (card_id = ? OR CAST(card_id AS TEXT) = ?) AND (person_id = ? OR CAST(person_id AS TEXT) = ?) LIMIT 1)", [targetAmount, now, row.card_id, String(row.card_id), row.person_id, String(row.person_id)]);
        }
        db.run("UPDATE set_asides SET amount = ?, updated_at = ? WHERE id = ? OR CAST(id AS TEXT) = ?", [newAmount, now, id, String(id)], function(upErr) {
          if (upErr) return res.status(500).json({ error: upErr.message });
          res.json({ success: true, fully_settled: false, remaining: newAmount, amount_paid: targetAmount });
        });
      });
    }
  });
});

app.post('/api/set-asides/:id/unpay', (req, res) => {
  const id = req.params.id;
  db.run(`UPDATE set_asides SET paid_amount = 0, is_paid = 0, status = 'apartado', updated_at = ? WHERE id = ?`,
    [Date.now(), id],
    function(err) {
      if (err) return res.status(500).json({ error: err.message });
      res.json({ success: true });
    }
  );
});

// MSI
app.get('/api/msi/:cardId', (req, res) => {
  db.all(`SELECT ip.*, p.name as person_name FROM installment_plans ip
          JOIN people p ON ip.person_id = p.id WHERE ip.card_id = ? ORDER BY ip.start_date DESC`,
    [req.params.cardId], (err, rows) => res.json(rows || []));
});

app.post('/api/msi/:id/advance', (req, res) => {
  db.run("UPDATE installment_plans SET paid_months = paid_months + 1, updated_at = ? WHERE id = ?",
    [Date.now(), req.params.id], () => res.json({ success: true }));
});

app.delete('/api/msi/:id', (req, res) => {
  db.run("DELETE FROM installment_plans WHERE id = ?", [req.params.id], () => res.json({ success: true }));
});

// Presupuestos
app.get('/api/budgets', (req, res) => {
  db.all("SELECT * FROM budgets ORDER BY name ASC", (err, budgets) => {
    db.all("SELECT * FROM budget_items ORDER BY concept ASC", (err2, items) => {
      res.json({ budgets: budgets || [], items: items || [] });
    });
  });
});

app.post('/api/budgets', (req, res) => {
  const id = crypto.randomUUID();
  db.run("INSERT INTO budgets (id, name, description, updated_at) VALUES (?, ?, ?, ?)",
    [id, req.body.name, req.body.description || '', Date.now()], () => res.json({ success: true, id }));
});

app.delete('/api/budgets/:id', (req, res) => {
  db.run("DELETE FROM budgets WHERE id = ?", [req.params.id], () => res.json({ success: true }));
});

app.post('/api/budget-items', (req, res) => {
  const { budget_id, concept, amount, type, tag } = req.body;
  const id = crypto.randomUUID();
  db.run(`INSERT INTO budget_items (id, budget_id, concept, amount, type, tag, updated_at)
          VALUES (?, ?, ?, ?, ?, ?, ?)`,
    [id, budget_id, concept, amount, type || 'Fijo', tag || 'General', Date.now()], () => res.json({ success: true, id }));
});

app.delete('/api/budget-items/:id', (req, res) => {
  db.run("DELETE FROM budget_items WHERE id = ?", [req.params.id], () => res.json({ success: true }));
});

// Reseteo
app.post('/api/cards/reset', (req, res) => {
  const { password } = req.body;
  db.all("SELECT key, value FROM app_config WHERE key IN ('master_pwd_hash', 'master_pwd_salt')", (err, rows) => {
    const hashRow = rows.find(r => r.key === 'master_pwd_hash');
    const saltRow = rows.find(r => r.key === 'master_pwd_salt');
    if (!hashRow || !saltRow || !checkPassword(password, hashRow.value, saltRow.value)) {
      return res.status(401).json({ error: 'Contraseña incorrecta' });
    }
    db.serialize(() => {
      db.run("DELETE FROM movements");
      db.run("DELETE FROM set_asides");
      res.json({ success: true });
    });
  });
});

// Restauración de Fábrica (Limpia toda la base de datos y la contraseña maestra)
app.post('/api/system/factory-reset', (req, res) => {
  const { password } = req.body;
  db.all("SELECT key, value FROM app_config WHERE key IN ('master_pwd_hash', 'master_pwd_salt')", (err, rows) => {
    if (!rows) return res.status(500).json({ error: 'Error accediendo a la configuración' });
    const hashRow = rows.find(r => r.key === 'master_pwd_hash');
    const saltRow = rows.find(r => r.key === 'master_pwd_salt');
    if (!hashRow || !saltRow || !checkPassword(password, hashRow.value, saltRow.value)) {
      return res.status(401).json({ error: 'Contraseña incorrecta. No se pudo restaurar de fábrica.' });
    }
    db.serialize(() => {
      db.run("DELETE FROM movements");
      db.run("DELETE FROM installment_plans");
      db.run("DELETE FROM set_asides");
      db.run("DELETE FROM budget_items");
      db.run("DELETE FROM budgets");
      db.run("DELETE FROM cards");
      db.run("DELETE FROM people WHERE name != 'Personal'");
      db.run("DELETE FROM app_config WHERE key IN ('master_pwd_hash', 'master_pwd_salt')");
      db.get("SELECT id FROM people WHERE name = 'Personal'", (pErr, pRow) => {
        if (!pRow) {
          db.run("INSERT INTO people (id, name, updated_at) VALUES (?, 'Personal', ?)", [crypto.randomUUID(), Date.now()]);
        }
      });
      db.run("VACUUM", () => {
        res.json({ success: true, message: 'Base de datos restaurada de fábrica con éxito' });
      });
    });
  });
});

// Sincronización JSON
app.get('/api/sync/export', (req, res) => {
  db.serialize(() => {
    const data = { exported_at: Date.now() };
    db.all("SELECT * FROM cards", (e, r) => { data.cards = r || []; });
    db.all("SELECT * FROM people", (e, r) => { data.people = r || []; });
    db.all("SELECT * FROM movements", (e, r) => { data.movements = r || []; });
    db.all("SELECT * FROM installment_plans", (e, r) => { data.installment_plans = r || []; });
    db.all("SELECT * FROM set_asides", (e, r) => { data.set_asides = r || []; });
    db.all("SELECT * FROM budgets", (e, r) => { data.budgets = r || []; });
    db.all("SELECT * FROM budget_items", (e, r) => {
      data.budget_items = r || [];
      res.setHeader('Content-Disposition', 'attachment; filename="sync_data.json"');
      res.json(data);
    });
  });
});

app.post('/api/sync/import', (req, res) => {
  const data = req.body;
  let count = 0;
  db.serialize(() => {
    if (data.cards) {
      data.cards.forEach(c => {
        db.run(`INSERT INTO cards (id, name, credit_limit, cutoff_day, color, logo_base64, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET name=excluded.name, credit_limit=excluded.credit_limit,
                cutoff_day=excluded.cutoff_day, color=excluded.color, logo_base64=excluded.logo_base64, updated_at=excluded.updated_at`,
          [c.id, c.name, c.credit_limit, c.cutoff_day, c.color, c.logo_base64, c.updated_at || Date.now()]);
        count++;
      });
    }
    if (data.people) {
      data.people.forEach(p => {
        db.run("INSERT INTO people (id, name, updated_at) VALUES (?, ?, ?) ON CONFLICT(id) DO NOTHING",
          [p.id, p.name, p.updated_at || Date.now()]);
        count++;
      });
    }
    if (data.movements) {
      data.movements.forEach(m => {
        db.run(`INSERT INTO movements (id, concept, amount, date, card_id, person_id, is_set_aside, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO NOTHING`,
          [m.id, m.concept, m.amount, m.date, m.card_id, m.person_id, m.is_set_aside || 0, m.updated_at || Date.now()]);
        count++;
      });
    }
    if (data.set_asides) {
      data.set_asides.forEach(s => {
        db.run(`INSERT INTO set_asides (id, card_id, person_id, movement_id, amount, note, date, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO NOTHING`,
          [s.id, s.card_id, s.person_id, s.movement_id, s.amount, s.note, s.date, s.updated_at || Date.now()]);
        count++;
      });
    }
    res.json({ success: true, processed: count });
  });
});

function formatBytes(bytes) {
  if (!bytes || bytes === 0) return '0 KB';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

// Información de almacenamiento en disco de SQLite
app.get('/api/database/info', (req, res) => {
  try {
    const exists = fs.existsSync(DB_PATH);
    const stats = exists ? fs.statSync(DB_PATH) : null;
    res.json({
      dbPath: DB_PATH,
      dataDir: DATA_DIR,
      exists: exists,
      sizeBytes: stats ? stats.size : 0,
      sizeFormatted: stats ? formatBytes(stats.size) : '0 KB',
      isDocker: Boolean(process.env.DATA_DIR && process.env.DATA_DIR === '/data'),
      updatedAt: stats ? stats.mtimeMs : null
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Descargar respaldo completo en formato JSON plano
app.get('/api/database/download', (req, res) => {
  db.serialize(() => {
    const data = {
      app: 'CardMaster',
      version: '2.0',
      format: 'cardmaster-json-backup',
      schema_version: 1,
      exported_at: Date.now(),
      exported_iso: new Date().toISOString()
    };
    db.all("SELECT * FROM cards", (e, r) => { data.cards = r || []; });
    db.all("SELECT * FROM people", (e, r) => { data.people = r || []; });
    db.all("SELECT * FROM movements", (e, r) => { data.movements = r || []; });
    db.all("SELECT * FROM installment_plans", (e, r) => { data.installment_plans = r || []; });
    db.all("SELECT * FROM set_asides", (e, r) => { data.set_asides = r || []; });
    db.all("SELECT * FROM budgets", (e, r) => { data.budgets = r || []; });
    db.all("SELECT * FROM budget_items", (e, r) => { data.budget_items = r || []; });
    db.all("SELECT key, value FROM app_config", (e, r) => {
      const cfg = {};
      (r || []).forEach(row => { cfg[row.key] = row.value; });
      data.config = cfg;
      res.setHeader('Content-Type', 'application/json');
      res.setHeader('Content-Disposition', 'attachment; filename="cardmaster_backup.json"');
      res.json(data);
    });
  });
});

// Restaurar base de datos desde respaldo JSON plano
app.post('/api/database/restore', (req, res) => {
  let data = req.body;
  if (data.jsonData) {
    data = typeof data.jsonData === 'string' ? JSON.parse(data.jsonData) : data.jsonData;
  }
  if (!data || typeof data !== 'object') {
    return res.status(400).json({ error: 'Formato JSON inválido' });
  }

  const hasCards = Array.isArray(data.cards);
  const hasMovs = Array.isArray(data.movements);
  const hasPeople = Array.isArray(data.people);
  const hasSig = data.app === 'CardMaster' || data.format === 'cardmaster-json-backup';

  if (!hasSig && !hasCards && !hasMovs && !hasPeople) {
    return res.status(400).json({ error: 'El archivo JSON no corresponde a una base de datos o respaldo de CardMaster' });
  }

  db.serialize(() => {
    db.run("BEGIN TRANSACTION");
    db.run("DELETE FROM movements");
    db.run("DELETE FROM installment_plans");
    db.run("DELETE FROM set_asides");
    db.run("DELETE FROM budget_items");
    db.run("DELETE FROM budgets");
    db.run("DELETE FROM cards");
    db.run("DELETE FROM people");
    db.run("DELETE FROM app_config");

    if (Array.isArray(data.cards)) {
      data.cards.forEach(c => {
        db.run("INSERT INTO cards (id, name, credit_limit, cutoff_day, color, logo_base64, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
          [c.id, c.name, c.credit_limit, c.cutoff_day, c.color || '#1e293b', c.logo_base64 || null, c.updated_at || Date.now()]);
      });
    }

    if (Array.isArray(data.people)) {
      data.people.forEach(p => {
        db.run("INSERT INTO people (id, name, updated_at) VALUES (?, ?, ?)",
          [p.id, p.name, p.updated_at || Date.now()]);
      });
    }

    if (Array.isArray(data.movements)) {
      data.movements.forEach(m => {
        db.run("INSERT INTO movements (id, concept, amount, date, card_id, person_id, is_set_aside, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
          [m.id, m.concept, m.amount, m.date, m.card_id, m.person_id, m.is_set_aside || 0, m.updated_at || Date.now()]);
      });
    }

    if (Array.isArray(data.installment_plans)) {
      data.installment_plans.forEach(i => {
        db.run("INSERT INTO installment_plans (id, concept, total_amount, months, start_date, card_id, person_id, paid_months, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          [i.id, i.concept, i.total_amount, i.months, i.start_date, i.card_id, i.person_id, i.paid_months || 0, i.updated_at || Date.now()]);
      });
    }

    if (Array.isArray(data.set_asides)) {
      data.set_asides.forEach(s => {
        db.run("INSERT INTO set_asides (id, card_id, person_id, movement_id, amount, fund_type, note, date, is_paid, paid_amount, status, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          [s.id, s.card_id, s.person_id, s.movement_id || null, s.amount, s.fund_type || 'Efectivo', s.note || '', s.date, s.is_paid || 0, s.paid_amount || 0, s.status || 'apartado', s.updated_at || Date.now()]);
      });
    }

    if (Array.isArray(data.budgets)) {
      data.budgets.forEach(b => {
        db.run("INSERT INTO budgets (id, name, description, updated_at) VALUES (?, ?, ?, ?)",
          [b.id, b.name, b.description || '', b.updated_at || Date.now()]);
      });
    }

    if (Array.isArray(data.budget_items)) {
      data.budget_items.forEach(bi => {
        db.run("INSERT INTO budget_items (id, budget_id, concept, amount, type, tag, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
          [bi.id, bi.budget_id, bi.concept, bi.amount, bi.type || 'Fijo', bi.tag || 'General', bi.updated_at || Date.now()]);
      });
    }

    if (data.config && typeof data.config === 'object') {
      Object.entries(data.config).forEach(([k, v]) => {
        db.run("INSERT INTO app_config (key, value) VALUES (?, ?)", [k, String(v)]);
      });
    }

    db.run("COMMIT", (commitErr) => {
      if (commitErr) {
        return res.status(500).json({ error: 'Error confirmando restauración: ' + commitErr.message });
      }
      res.json({
        success: true,
        message: 'Base de datos restaurada exitosamente desde JSON',
        counts: {
          cards: (data.cards || []).length,
          movements: (data.movements || []).length
        }
      });
    });
  });
});

app.get('/api/database/empty-status', (req, res) => {
  db.get("SELECT COUNT(*) as c FROM cards", (e, r1) => {
    db.get("SELECT COUNT(*) as c FROM movements", (e2, r2) => {
      db.get("SELECT COUNT(*) as c FROM app_config WHERE key = 'master_pwd_hash'", (e3, r3) => {
        const isEmpty = Boolean((!r1 || r1.c === 0) && (!r2 || r2.c === 0) && (!r3 || r3.c === 0));
        res.json({ isEmpty });
      });
    });
  });
});

// Endpoints de configuración de tasa de incremento persistente
app.get('/api/config/inflation', (req, res) => {
  db.get("SELECT value FROM app_config WHERE key = 'inflation_rate'", (err, row) => {
    if (err) return res.status(500).json({ error: 'Error leyendo de base de datos' });
    res.json({ inflationRate: (row && row.value !== null) ? parseFloat(row.value) : 6.0 });
  });
});

app.post('/api/config/inflation', (req, res) => {
  const { rate } = req.body;
  const numVal = parseFloat(rate);
  const val = isNaN(numVal) ? '6.0' : String(numVal);
  
  db.serialize(() => {
    db.run("CREATE TABLE IF NOT EXISTS app_config (key TEXT PRIMARY KEY, value TEXT)");
    db.run("INSERT OR REPLACE INTO app_config (key, value) VALUES ('inflation_rate', ?)", [val], function(err) {
      if (err) {
        console.error("Error en SQLite al guardar inflation_rate:", err);
        return res.status(500).json({ error: 'Error guardando en tarjetas.db: ' + err.message });
      }
      res.json({ success: true, inflationRate: parseFloat(val) });
    });
  });
});


// Cambiar directamente ubicación de un apartado existente (Efectivo <-> Débito)
app.patch('/api/set-asides/:id/toggle-fund', (req, res) => {
  db.get("SELECT fund_type FROM set_asides WHERE id = ?", [req.params.id], (err, row) => {
    if (!row) return res.status(404).json({ error: 'Registro no encontrado' });
    const current = (row.fund_type === 'Débito' || row.fund_type === 'Debito') ? 'Débito' : 'Efectivo';
    const nextType = current === 'Efectivo' ? 'Débito' : 'Efectivo';
    db.run("UPDATE set_asides SET fund_type = ?, updated_at = ? WHERE id = ?", [nextType, Date.now(), req.params.id], (err2) => {
      if (err2) return res.status(500).json({ error: 'Error actualizando fondo' });
      res.json({ success: true, newFundType: nextType });
    });
  });
});

// Transferir monto parcial o total entre Efectivo y Débito para una tarjeta
app.post('/api/set-asides/transfer', (req, res) => {
  const { card_id, from_type, amount, note } = req.body;
  const numAmount = parseFloat(amount);
  if (isNaN(numAmount) || numAmount <= 0) {
    return res.status(400).json({ error: 'Monto inválido' });
  }

  const originType = (from_type === 'Débito' || from_type === 'Debito') ? 'Débito' : 'Efectivo';
  const destType = originType === 'Efectivo' ? 'Débito' : 'Efectivo';
  const now = Date.now();
  const today = new Date().toISOString().split('T')[0];

  db.get("SELECT id FROM people WHERE name = 'Personal'", (err, personRow) => {
    const personId = personRow ? personRow.id : 'personal';
    const noteText = note ? ` (${note})` : '';

    db.serialize(() => {
      // Registro 1: Reducción del fondo de origen
      const idOut = crypto.randomUUID();
      db.run(`INSERT INTO set_asides (id, card_id, person_id, amount, fund_type, note, date, updated_at)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
        [idOut, card_id, personId, -numAmount, originType, `Pase a ${destType}${noteText}`, today, now]);

      // Registro 2: Aumento en el fondo destino
      const idIn = crypto.randomUUID();
      db.run(`INSERT INTO set_asides (id, card_id, person_id, amount, fund_type, note, date, updated_at)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
        [idIn, card_id, personId, numAmount, destType, `Recepción de ${originType}${noteText}`, today, now + 1]);

      res.json({ success: true, transferred: numAmount, from: originType, to: destType });
    });
  });
});

const HOST = process.env.HOST || '0.0.0.0';

app.listen(PORT, HOST, () => {
  console.log(`=======================================================`);
  console.log(`🚀 CardMaster Backend listo en: http://${HOST === '0.0.0.0' ? 'localhost' : HOST}:${PORT}`);
  console.log(`📁 Base de datos activa en: ${DB_PATH}`);
  console.log(`=======================================================`);
});
