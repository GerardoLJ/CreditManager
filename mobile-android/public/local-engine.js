/**
 * CardMaster - Motor de Almacenamiento Autónomo Local (Embedded Local Engine)
 * Permite que la app funcione 100% independiente en el celular sin servidores,
 * sin Termux y sin necesidad de tener la PC encendida.
 */

(function() {
  'use strict';

  // --- UTILIDADES CRIPTOGRÁFICAS (Web Crypto API Nativa) ---
  function generateSaltHex(len = 16) {
    const arr = new Uint8Array(len);
    window.crypto.getRandomValues(arr);
    return Array.from(arr).map(b => b.toString(16).padStart(2, '0')).join('');
  }

  // --- FALLBACK CRIPTOGRÁFICO PURO JAVASCRIPT (Garantiza funcionamiento en file:// o navegadores sin HTTPS) ---
  function sha256_bytes_pure(bytes) {
    function rightRotate(value, amount) { return (value >>> amount) | (value << (32 - amount)); }
    const mathPow = Math.pow, maxWord = mathPow(2, 32);
    const words = [];
    const bitLength = bytes.length * 8;
    let hash = [];
    const k = [];
    let primeCounter = 0;
    const isComposite = {};
    for (let candidate = 2; primeCounter < 64; candidate++) {
      if (!isComposite[candidate]) {
        for (let i = 0; i < 313; i += candidate) isComposite[i] = candidate;
        hash[primeCounter] = (mathPow(candidate, 0.5) * maxWord) | 0;
        k[primeCounter++] = (mathPow(candidate, 1 / 3) * maxWord) | 0;
      }
    }
    const padded = new Uint8Array(Math.ceil((bytes.length + 9) / 64) * 64);
    padded.set(bytes);
    padded[bytes.length] = 0x80;
    const view = new DataView(padded.buffer);
    view.setUint32(padded.length - 4, bitLength, false);
    for (let i = 0; i < padded.length; i += 4) words.push(view.getUint32(i, false));
    for (let j = 0; j < words.length; j += 16) {
      const w = words.slice(j, j + 16);
      const oldHash = hash.slice();
      for (let i = 0; i < 64; i++) {
        const w15 = w[i - 15], w2 = w[i - 2];
        const a = hash[0], e = hash[4];
        const temp1 = (hash[7] + (rightRotate(e, 6) ^ rightRotate(e, 11) ^ rightRotate(e, 25)) + ((e & hash[5]) ^ (~e & hash[6])) + k[i] +
          (w[i] = i < 16 ? w[i] : (w[i - 16] + (rightRotate(w15, 7) ^ rightRotate(w15, 18) ^ (w15 >>> 3)) + w[i - 7] + (rightRotate(w2, 17) ^ rightRotate(w2, 19) ^ (w2 >>> 10))) | 0)) | 0;
        const temp2 = ((rightRotate(a, 2) ^ rightRotate(a, 13) ^ rightRotate(a, 22)) + ((a & hash[1]) ^ (a & hash[2]) ^ (hash[1] & hash[2]))) | 0;
        hash = [(temp1 + temp2) | 0].concat(hash.slice(0, 7));
        hash[4] = (hash[4] + temp1) | 0;
      }
      for (let i = 0; i < 8; i++) hash[i] = (hash[i] + oldHash[i]) | 0;
    }
    const result = new Uint8Array(32);
    const outView = new DataView(result.buffer);
    for (let i = 0; i < 8; i++) outView.setUint32(i * 4, hash[i], false);
    return result;
  }

  function hmac_sha256_pure(key, message) {
    let k = key;
    if (k.length > 64) k = sha256_bytes_pure(k);
    const kPadO = new Uint8Array(64);
    const kPadI = new Uint8Array(64);
    for (let i = 0; i < 64; i++) {
      const b = i < k.length ? k[i] : 0;
      kPadO[i] = b ^ 0x5c;
      kPadI[i] = b ^ 0x36;
    }
    const innerMsg = new Uint8Array(64 + message.length);
    innerMsg.set(kPadI);
    innerMsg.set(message, 64);
    const innerHash = sha256_bytes_pure(innerMsg);
    const outerMsg = new Uint8Array(64 + 32);
    outerMsg.set(kPadO);
    outerMsg.set(innerHash, 64);
    return sha256_bytes_pure(outerMsg);
  }

  function pbkdf2_sync_pure(password, salt, iterations = 100000, keyLen = 32) {
    const enc = new TextEncoder();
    const pwdBytes = enc.encode(password);
    const saltBytes = enc.encode(salt);
    const numBlocks = Math.ceil(keyLen / 32);
    const result = new Uint8Array(numBlocks * 32);
    for (let b = 1; b <= numBlocks; b++) {
      const initialMsg = new Uint8Array(saltBytes.length + 4);
      initialMsg.set(saltBytes);
      initialMsg[saltBytes.length] = (b >>> 24) & 255;
      initialMsg[saltBytes.length + 1] = (b >>> 16) & 255;
      initialMsg[saltBytes.length + 2] = (b >>> 8) & 255;
      initialMsg[saltBytes.length + 3] = b & 255;
      let u = hmac_sha256_pure(pwdBytes, initialMsg);
      const block = new Uint8Array(u);
      for (let i = 1; i < iterations; i++) {
        u = hmac_sha256_pure(pwdBytes, u);
        for (let j = 0; j < 32; j++) block[j] ^= u[j];
      }
      result.set(block, (b - 1) * 32);
    }
    return Array.from(result.subarray(0, keyLen)).map(x => x.toString(16).padStart(2, "0")).join("");
  }

  // PBKDF2 compatible 100% con Node.js (crypto.pbkdf2Sync con salt string)
  async function hashPasswordPbkdf2(password, saltHex) {
    if (window.crypto && window.crypto.subtle) {
      try {
        const enc = new TextEncoder();
        const keyMaterial = await window.crypto.subtle.importKey(
          'raw',
          enc.encode(password),
          { name: 'PBKDF2' },
          false,
          ['deriveBits']
        );
        const saltBytes = enc.encode(saltHex);
        const derivedBits = await window.crypto.subtle.deriveBits(
          {
            name: 'PBKDF2',
            salt: saltBytes,
            iterations: 100000,
            hash: 'SHA-256'
          },
          keyMaterial,
          256
        );
        return Array.from(new Uint8Array(derivedBits)).map(b => b.toString(16).padStart(2, '0')).join('');
      } catch (e) {
        console.warn("Aviso WebCrypto, usando fallback JS:", e);
      }
    }
    return pbkdf2_sync_pure(password, saltHex, 100000, 32);
  }

  // Soporte retrocompatible por si alguna versión anterior de mobile guardó sal binaria
  async function hashPasswordPbkdf2HexLegacy(password, saltHex) {
    try {
      const enc = new TextEncoder();
      const keyMaterial = await window.crypto.subtle.importKey(
        'raw',
        enc.encode(password),
        { name: 'PBKDF2' },
        false,
        ['deriveBits']
      );
      const saltBytes = new Uint8Array(saltHex.match(/.{1,2}/g).map(byte => parseInt(byte, 16)));
      const derivedBits = await window.crypto.subtle.deriveBits(
        {
          name: 'PBKDF2',
          salt: saltBytes,
          iterations: 100000,
          hash: 'SHA-256'
        },
        keyMaterial,
        256
      );
      return Array.from(new Uint8Array(derivedBits)).map(b => b.toString(16).padStart(2, '0')).join('');
    } catch (e) {
      return null;
    }
  }

  // Función universal de verificación de contraseña
  async function verifyPassword(password, expectedHash, saltHex) {
    if (!password || !expectedHash || !saltHex) return false;
    // 1. Probar primero con sal UTF-8 (el estándar de PC y SQLite tarjetas.db)
    const hash = await hashPasswordPbkdf2(password, saltHex);
    if (hash === expectedHash) return true;
    // 2. Probar fallback con sal binaria
    const legacyHash = await hashPasswordPbkdf2HexLegacy(password, saltHex);
    if (legacyHash === expectedHash) return true;
    return false;
  }

  function getUUID() {
    if (window.crypto && window.crypto.randomUUID) {
      return window.crypto.randomUUID();
    }
    return 'cm-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9);
  }

  // --- ALMACÉN DE DATOS EN LOCALSTORAGE ---
  const DB = {
    get(key, defaultVal = []) {
      try {
        const val = localStorage.getItem('cm_data_' + key);
        return val ? JSON.parse(val) : defaultVal;
      } catch (e) {
        return defaultVal;
      }
    },
    set(key, val) {
      try {
        localStorage.setItem('cm_data_' + key, JSON.stringify(val));
      } catch (e) {
        console.error('Error guardando en almacenamiento local:', e);
      }
    },
    getConfig() {
      return this.get('config', {});
    },
    setConfig(obj) {
      const cur = this.getConfig();
      this.set('config', { ...cur, ...obj });
    }
  };

  // Inicializar almacenamiento local en estado limpio de fábrica (sin registros personales ni contraseñas)
  function initDefaults() {
    const isInit = localStorage.getItem('cm_data_initialized');
    if (!isInit) {
      DB.set('cards', []);
      DB.set('people', [{ id: getUUID(), name: 'Personal', updated_at: Date.now() }]);
      DB.set('movements', []);
      DB.set('installment_plans', []);
      DB.set('set_asides', []);
      DB.set('budgets', []);
      DB.set('budget_items', []);
      DB.setConfig({ inflation_rate: '10' });
      localStorage.setItem('cm_data_initialized', '1');
    }
  }
  initDefaults();

  function uint8ToBase64(bytes) {
    let binary = '';
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
  }

  async function generateSqliteBinary() {
    const initFn = window.initSqlJs || (typeof initSqlJs !== 'undefined' ? initSqlJs : null);
    if (!initFn) return null;
    const SQL = await initFn();
    const db = new SQL.Database();

    db.run(`CREATE TABLE IF NOT EXISTS cards (id TEXT PRIMARY KEY, name TEXT NOT NULL, credit_limit REAL NOT NULL, cutoff_day INTEGER NOT NULL, color TEXT, logo_base64 TEXT, updated_at INTEGER)`);
    db.run(`CREATE TABLE IF NOT EXISTS people (id TEXT PRIMARY KEY, name TEXT NOT NULL UNIQUE, updated_at INTEGER)`);
    db.run(`CREATE TABLE IF NOT EXISTS movements (id TEXT PRIMARY KEY, concept TEXT NOT NULL, amount REAL NOT NULL, date TEXT NOT NULL, card_id TEXT NOT NULL, person_id TEXT NOT NULL, is_set_aside INTEGER DEFAULT 0, updated_at INTEGER)`);
    db.run(`CREATE TABLE IF NOT EXISTS installment_plans (id TEXT PRIMARY KEY, concept TEXT NOT NULL, total_amount REAL NOT NULL, months INTEGER NOT NULL, start_date TEXT NOT NULL, card_id TEXT NOT NULL, person_id TEXT NOT NULL, paid_months INTEGER DEFAULT 0, updated_at INTEGER)`);
    db.run(`CREATE TABLE IF NOT EXISTS set_asides (id TEXT PRIMARY KEY, card_id TEXT NOT NULL, person_id TEXT NOT NULL, movement_id TEXT, amount REAL NOT NULL, fund_type TEXT DEFAULT 'Efectivo', note TEXT, date TEXT NOT NULL, updated_at INTEGER)`);
    db.run(`CREATE TABLE IF NOT EXISTS budgets (id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT, updated_at INTEGER)`);
    db.run(`CREATE TABLE IF NOT EXISTS budget_items (id TEXT PRIMARY KEY, budget_id TEXT NOT NULL, concept TEXT NOT NULL, amount REAL NOT NULL, type TEXT NOT NULL, tag TEXT, updated_at INTEGER)`);
    db.run(`CREATE TABLE IF NOT EXISTS app_config (key TEXT PRIMARY KEY, value TEXT)`);

    const cfg = DB.getConfig();
    Object.entries(cfg).forEach(([k, v]) => {
      db.run(`INSERT OR REPLACE INTO app_config VALUES (?, ?)`, [k, String(v)]);
    });

    DB.get('cards').forEach(c => {
      db.run(`INSERT OR REPLACE INTO cards VALUES (?, ?, ?, ?, ?, ?, ?)`, [c.id, c.name, c.credit_limit, c.cutoff_day, c.color || '#1e293b', c.logo_base64 || null, c.updated_at || Date.now()]);
    });

    DB.get('people').forEach(p => {
      db.run(`INSERT OR REPLACE INTO people VALUES (?, ?, ?)`, [p.id, p.name, p.updated_at || Date.now()]);
    });

    DB.get('movements').forEach(m => {
      db.run(`INSERT OR REPLACE INTO movements VALUES (?, ?, ?, ?, ?, ?, ?, ?)`, [m.id, m.concept, m.amount, m.date, m.card_id, m.person_id, m.is_set_aside || 0, m.updated_at || Date.now()]);
    });

    DB.get('installment_plans').forEach(i => {
      db.run(`INSERT OR REPLACE INTO installment_plans VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`, [i.id, i.concept, i.total_amount, i.months, i.start_date, i.card_id, i.person_id, i.paid_months || 0, i.updated_at || Date.now()]);
    });

    DB.get('set_asides').forEach(s => {
      db.run(`INSERT OR REPLACE INTO set_asides VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`, [s.id, s.card_id, s.person_id, s.movement_id || null, s.amount, s.fund_type || 'Efectivo', s.note || '', s.date, s.updated_at || Date.now()]);
    });

    DB.get('budgets').forEach(b => {
      db.run(`INSERT OR REPLACE INTO budgets VALUES (?, ?, ?, ?)`, [b.id, b.name, b.description || '', b.updated_at || Date.now()]);
    });

    DB.get('budget_items').forEach(bi => {
      db.run(`INSERT OR REPLACE INTO budget_items VALUES (?, ?, ?, ?, ?, ?, ?)`, [bi.id, bi.budget_id, bi.concept, bi.amount, bi.type, bi.tag, bi.updated_at || Date.now()]);
    });

    const binary = db.export();
    db.close();
    return binary;
  }

  async function syncPhysicalStorage() {
    try {
      const binary = await generateSqliteBinary();
      if (!binary) return;

      // 1. Android Native Storage Bridge (Capacitor)
      if (window.AndroidNativeStorage && typeof window.AndroidNativeStorage.saveDbFile === 'function') {
        const b64 = uint8ToBase64(binary);
        const savedPath = window.AndroidNativeStorage.saveDbFile(b64, 'tarjetas.db');
        console.log('💾 [AndroidNativeStorage] Base de datos guardada en archivo persistente:', savedPath);
      }

      // 2. File System Access API (Desktop / Navegadores modernos)
      if (window.activeDbFileHandle && typeof window.activeDbFileHandle.createWritable === 'function') {
        try {
          const writable = await window.activeDbFileHandle.createWritable();
          await writable.write(binary);
          await writable.close();
          console.log('💾 [FileSystemHandle] Base de datos guardada en archivo físico de disco');
        } catch (handleErr) {
          console.warn('Aviso escribiendo en FileSystemHandle:', handleErr);
        }
      }
    } catch (e) {
      console.warn('Aviso en syncPhysicalStorage:', e);
    }
  }

  // --- SIMULADOR DE API DE CARDMASTER ---
  async function handleLocalApi(method, url, body) {
    const res = await _handleLocalApiInner(method, url, body);
    const mutatingMethods = ['POST', 'PUT', 'DELETE', 'PATCH'];
    const path = url.split('?')[0];
    if (mutatingMethods.includes(method) && !path.startsWith('/api/auth/login') && !path.startsWith('/api/auth/status') && !path.startsWith('/api/auth/check')) {
      syncPhysicalStorage().catch(e => console.warn('Aviso sincronizando almacenamiento físico:', e));
    }
    return res;
  }

  async function _handleLocalApiInner(method, url, body) {
    const path = url.split('?')[0];

    // 1. AUTENTICACIÓN
    if (path === '/api/auth/status') {
      const cfg = DB.getConfig();
      return { isConfigured: Boolean(cfg.master_pwd_hash && cfg.master_pwd_salt) };
    }

    if (path === '/api/auth/setup') {
      const { password } = body;
      const salt = generateSaltHex(16);
      const hash = await hashPasswordPbkdf2(password, salt);
      DB.setConfig({ master_pwd_hash: hash, master_pwd_salt: salt });
      return { success: true };
    }

    if (path === '/api/auth/login') {
      const { password } = body;
      const cfg = DB.getConfig();
      if (!cfg.master_pwd_hash || !cfg.master_pwd_salt) {
        return { status: 400, error: 'No configurado' };
      }
      const ok = await verifyPassword(password, cfg.master_pwd_hash, cfg.master_pwd_salt);
      if (ok) {
        // Migrar automáticamente si venía de un formato legacy al estándar UTF-8 de PC
        const standardHash = await hashPasswordPbkdf2(password, cfg.master_pwd_salt);
        if (cfg.master_pwd_hash !== standardHash) {
          DB.setConfig({ master_pwd_hash: standardHash });
        }
        return { success: true };
      }
      return { status: 401, error: 'Contraseña incorrecta' };
    }

    if (path === '/api/auth/change-pwd') {
      const { oldPassword, newPassword } = body;
      const cfg = DB.getConfig();
      const ok = await verifyPassword(oldPassword, cfg.master_pwd_hash, cfg.master_pwd_salt);
      if (!ok) {
        return { status: 401, error: 'Contraseña actual incorrecta' };
      }
      const newSalt = generateSaltHex(16);
      const newHash = await hashPasswordPbkdf2(newPassword, newSalt);
      DB.setConfig({ master_pwd_hash: newHash, master_pwd_salt: newSalt });
      return { success: true };
    }

    // 2. TARJETAS
    if (path === '/api/cards') {
      if (method === 'GET') {
        const cards = DB.get('cards');
        return cards.sort((a, b) => a.name.localeCompare(b.name));
      }
      if (method === 'POST') {
        const cards = DB.get('cards');
        const cardId = body.id || getUUID();
        const existingIdx = cards.findIndex(c => c.id === cardId);
        const cardObj = {
          id: cardId,
          name: body.name,
          credit_limit: parseFloat(body.credit_limit) || 0,
          cutoff_day: parseInt(body.cutoff_day) || 1,
          color: body.color || '#1e293b',
          logo_base64: body.logo_base64 || null,
          updated_at: Date.now()
        };
        if (existingIdx >= 0) {
          cards[existingIdx] = cardObj;
        } else {
          cards.push(cardObj);
        }
        DB.set('cards', cards);
        return { success: true, id: cardId };
      }
    }

    if (path.match(/^\/api\/cards\/([^/]+)\/delete$/)) {
      const cardId = path.split('/')[3];
      const { password } = body;
      const cfg = DB.getConfig();
      const ok = await verifyPassword(password, cfg.master_pwd_hash, cfg.master_pwd_salt);
      if (!ok) {
        return { status: 401, error: 'Contraseña incorrecta. No se eliminó la tarjeta.' };
      }
      DB.set('cards', DB.get('cards').filter(c => c.id !== cardId));
      DB.set('movements', DB.get('movements').filter(m => m.card_id !== cardId));
      DB.set('installment_plans', DB.get('installment_plans').filter(i => i.card_id !== cardId));
      DB.set('set_asides', DB.get('set_asides').filter(s => s.card_id !== cardId));
      return { success: true };
    }

    if (path === '/api/cards/reset') {
      const { password } = body;
      const cfg = DB.getConfig();
      const ok = await verifyPassword(password, cfg.master_pwd_hash, cfg.master_pwd_salt);
      if (!ok) {
        return { status: 401, error: 'Contraseña incorrecta' };
      }
      DB.set('movements', []);
      DB.set('set_asides', []);
      return { success: true };
    }

    if (path === '/api/system/factory-reset') {
      const { password } = body;
      const cfg = DB.getConfig();
      if (!cfg.master_pwd_hash || !cfg.master_pwd_salt) {
        return { status: 400, error: 'La base de datos no está configurada' };
      }
      const ok = await verifyPassword(password, cfg.master_pwd_hash, cfg.master_pwd_salt);
      if (!ok) {
        return { status: 401, error: 'Contraseña incorrecta. No se pudo restaurar de fábrica.' };
      }
      DB.set('cards', []);
      DB.set('movements', []);
      DB.set('installment_plans', []);
      DB.set('set_asides', []);
      DB.set('budgets', []);
      DB.set('budget_items', []);
      DB.set('people', [{ id: getUUID(), name: 'Personal', updated_at: Date.now() }]);
      DB.set('config', { inflation_rate: '10' });
      try { localStorage.removeItem('cm_data_initialized'); } catch (e) {}
      return { success: true, message: 'Base de datos restaurada de fábrica con éxito' };
    }

    // 3. PERSONAS
    if (path === '/api/people') {
      if (method === 'GET') {
        const people = DB.get('people');
        return people.sort((a, b) => a.name.localeCompare(b.name));
      }
      if (method === 'POST') {
        const people = DB.get('people');
        const nameTrimmed = (body.name || '').trim();
        if (people.some(p => p.name.toLowerCase() === nameTrimmed.toLowerCase())) {
          return { status: 400, error: 'Ya existe esa persona' };
        }
        const id = getUUID();
        people.push({ id, name: nameTrimmed, updated_at: Date.now() });
        DB.set('people', people);
        return { success: true, id };
      }
    }

    if (path.startsWith('/api/people/')) {
      const pId = path.split('/')[3];
      DB.set('people', DB.get('people').filter(p => p.id !== pId));
      return { success: true };
    }

    // 4. MOVIMIENTOS
    if (path === '/api/movements') {
      if (method === 'GET') {
        const movs = DB.get('movements');
        const cards = DB.get('cards');
        const people = DB.get('people');
        const asides = DB.get('set_asides') || [];
        const cardsMap = Object.fromEntries(cards.map(c => [c.id, c.name]));
        const peopleMap = Object.fromEntries(people.map(p => [p.id, p.name]));

        const enriched = movs.map(m => {
          const movAsides = asides.filter(s => String(s.movement_id) === String(m.id));
          const totalSetAside = movAsides.reduce((acc, s) => acc + (parseFloat(s.amount) || 0), 0);
          const totalPaid = movAsides.reduce((acc, s) => {
            const pAmt = parseFloat(s.paid_amount);
            if (!isNaN(pAmt) && pAmt > 0) return acc + pAmt;
            if (s.is_paid) return acc + (parseFloat(s.amount) || 0);
            return acc;
          }, 0);

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
            card_name: cardsMap[m.card_id] || 'Desconocida',
            person_name: peopleMap[m.person_id] || 'Desconocido',
            total_set_aside: totalSetAside,
            total_paid: totalPaid,
            status
          };
        });
        return enriched.sort((a, b) => (b.date || '').localeCompare(a.date || ''));
      }
      if (method === 'POST') {
        const now = Date.now();
        if (body.is_msi) {
          const msiId = getUUID();
          const plans = DB.get('installment_plans');
          plans.push({
            id: msiId,
            concept: body.concept,
            total_amount: parseFloat(body.amount) || 0,
            months: parseInt(body.msi_months) || 12,
            start_date: body.date,
            card_id: body.card_id,
            person_id: body.person_id,
            paid_months: 0,
            updated_at: now
          });
          DB.set('installment_plans', plans);
          return { success: true, msiId };
        } else {
          const movId = getUUID();
          const movs = DB.get('movements');
          movs.push({
            id: movId,
            concept: body.concept,
            amount: parseFloat(body.amount) || 0,
            date: body.date,
            card_id: body.card_id,
            person_id: body.person_id,
            is_set_aside: 0,
            updated_at: now
          });
          DB.set('movements', movs);
          return { success: true, id: movId };
        }
      }
    }

    if (path.match(/^\/api\/movements\/([^/]+)$/) && (method === 'PUT' || method === 'POST')) {
      const movId = path.split('/')[3];
      const movs = DB.get('movements');
      const idx = movs.findIndex(m => m.id === movId);
      if (idx >= 0) {
        movs[idx] = {
          ...movs[idx],
          concept: body.concept,
          amount: parseFloat(body.amount) || 0,
          date: body.date,
          card_id: body.card_id,
          person_id: body.person_id,
          updated_at: Date.now()
        };
        DB.set('movements', movs);
        return { success: true, changes: 1 };
      }
      return { status: 404, error: 'Movimiento no encontrado' };
    }

    if (path.match(/^\/api\/movements\/([^/]+)\/update$/)) {
      const movId = path.split('/')[3];
      const movs = DB.get('movements');
      const idx = movs.findIndex(m => m.id === movId);
      if (idx >= 0) {
        movs[idx] = {
          ...movs[idx],
          concept: body.concept,
          amount: parseFloat(body.amount) || 0,
          date: body.date,
          card_id: body.card_id,
          person_id: body.person_id,
          updated_at: Date.now()
        };
        DB.set('movements', movs);
        return { success: true, changes: 1 };
      }
      return { status: 404, error: 'Movimiento no encontrado' };
    }

    if (path.match(/^\/api\/movements\/([^/]+)$/) && method === 'DELETE') {
      const movId = path.split('/')[3];
      DB.set('movements', DB.get('movements').filter(m => m.id !== movId));
      DB.set('set_asides', DB.get('set_asides').filter(s => s.movement_id !== movId));
      return { success: true };
    }

    // 5. APARTADOS (SET-ASIDES)
    if (path === '/api/set-asides') {
      if (method === 'GET') {
        const asides = DB.get('set_asides');
        const cards = DB.get('cards');
        const people = DB.get('people');
        const cardsMap = Object.fromEntries(cards.map(c => [String(c.id), c.name]));
        const peopleMap = Object.fromEntries(people.map(p => [String(p.id), p.name]));

        const enriched = asides.map(s => {
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
            card_name: cardsMap[String(s.card_id)] || 'Desconocida',
            person_name: peopleMap[String(s.person_id)] || 'Desconocido',
            paid_amount: paidAmount,
            is_paid: s.is_paid ? 1 : 0,
            status
          };
        });
        return enriched.sort((a, b) => (b.date || '').localeCompare(a.date || ''));
      }
      if (method === 'POST') {
        const id = getUUID();
        const now = Date.now();
        const fundType = (body.fund_type === 'Débito' || body.fund_type === 'Debito') ? 'Débito' : 'Efectivo';
        const numAmount = parseFloat(body.amount) || 0;
        const numPaid = parseFloat(body.paid_amount) || 0;
        const paidFlag = body.is_paid ? 1 : (numPaid >= numAmount && numAmount > 0 ? 1 : 0);
        const statusVal = body.status || (paidFlag ? 'pagado' : (numPaid > 0 ? 'pago_parcial' : 'apartado'));

        const asides = DB.get('set_asides');
        asides.push({
          id,
          card_id: String(body.card_id),
          person_id: String(body.person_id),
          movement_id: body.movement_id || null,
          amount: numAmount,
          fund_type: fundType,
          note: body.note || '',
          date: body.date || new Date().toISOString().split('T')[0],
          is_paid: paidFlag,
          paid_amount: numPaid,
          status: statusVal,
          updated_at: now
        });
        DB.set('set_asides', asides);

        if (body.movement_id) {
          const movs = DB.get('movements');
          const mIdx = movs.findIndex(m => m.id === body.movement_id);
          if (mIdx >= 0) {
            movs[mIdx].is_set_aside = 1;
            movs[mIdx].updated_at = now;
            DB.set('movements', movs);
          }
        }
        return { success: true, id };
      }
    }

    if (path.match(/^\/api\/set-asides\/([^/]+)$/) && method === 'PUT') {
      const asideId = path.split('/')[3];
      const asides = DB.get('set_asides');
      const idx = asides.findIndex(s => String(s.id) === String(asideId));
      if (idx >= 0) {
        const numAmount = parseFloat(body.amount) || 0;
        const numPaid = parseFloat(body.paid_amount) || 0;
        const paidFlag = body.is_paid ? 1 : (numPaid >= numAmount && numAmount > 0 ? 1 : 0);
        const statusVal = body.status || (paidFlag ? 'pagado' : (numPaid > 0 ? 'pago_parcial' : 'apartado'));
        const fundType = (body.fund_type === 'Débito' || body.fund_type === 'Debito') ? 'Débito' : 'Efectivo';

        asides[idx] = {
          ...asides[idx],
          card_id: String(body.card_id),
          person_id: String(body.person_id),
          movement_id: body.movement_id || null,
          amount: numAmount,
          fund_type: fundType,
          note: body.note || '',
          date: body.date || new Date().toISOString().split('T')[0],
          is_paid: paidFlag,
          paid_amount: numPaid,
          status: statusVal,
          updated_at: Date.now()
        };
        DB.set('set_asides', asides);
        return { success: true, id: asideId };
      }
      return { status: 404, error: 'Apartado no encontrado' };
    }

    if (path.match(/^\/api\/set-asides\/([^/]+)$/) && method === 'DELETE') {
      const asideId = path.split('/')[3];
      const asides = DB.get('set_asides');
      const target = asides.find(s => s.id === asideId);
      const remainingAsides = asides.filter(s => s.id !== asideId);
      DB.set('set_asides', remainingAsides);

      if (target && target.movement_id) {
        const hasLeft = remainingAsides.some(s => s.movement_id === target.movement_id);
        const movs = DB.get('movements');
        const mIdx = movs.findIndex(m => m.id === target.movement_id);
        if (mIdx >= 0) {
          movs[mIdx].is_set_aside = hasLeft ? 1 : 0;
          movs[mIdx].updated_at = Date.now();
          DB.set('movements', movs);
        }
      }
      return { success: true };
    }

    if (path.match(/^\/api\/set-asides\/([^/]+)\/pay$/)) {
      const asideId = path.split('/')[3];
      const asides = DB.get('set_asides');
      const idx = asides.findIndex(s => String(s.id) === String(asideId));
      if (idx >= 0) {
        const row = asides[idx];
        const targetAmount = (body.paid_amount !== undefined && body.paid_amount !== null && !isNaN(parseFloat(body.paid_amount)))
          ? Math.min(row.amount, parseFloat(body.paid_amount))
          : row.amount;
        const isFull = targetAmount >= (row.amount - 0.001);

        const movs = DB.get('movements');

        if (isFull) {
          // Liquidación 100%: borrar movimiento y borrar apartado para liberar crédito por completo
          if (row.movement_id) {
            const mIdx = movs.findIndex(m => String(m.id) === String(row.movement_id));
            if (mIdx >= 0) movs.splice(mIdx, 1);
          } else {
            const mIdx = movs.findIndex(m => String(m.card_id) === String(row.card_id) && String(m.person_id) === String(row.person_id) && Math.abs(m.amount - row.amount) < 0.01);
            if (mIdx >= 0) movs.splice(mIdx, 1);
          }
          DB.set('movements', movs);

          // Borrar apartado liquidado
          asides.splice(idx, 1);
          DB.set('set_asides', asides);

          await syncPhysicalStorage();
          return { success: true, fully_settled: true, amount_paid: targetAmount };
        } else {
          // Pago parcial: reducir del movimiento y del apartado
          const newAmount = Math.max(0, row.amount - targetAmount);
          if (row.movement_id) {
            const mIdx = movs.findIndex(m => String(m.id) === String(row.movement_id));
            if (mIdx >= 0) {
              movs[mIdx].amount = Math.max(0, movs[mIdx].amount - targetAmount);
              movs[mIdx].updated_at = Date.now();
            }
          } else {
            const mIdx = movs.findIndex(m => String(m.card_id) === String(row.card_id) && String(m.person_id) === String(row.person_id));
            if (mIdx >= 0) {
              movs[mIdx].amount = Math.max(0, movs[mIdx].amount - targetAmount);
              movs[mIdx].updated_at = Date.now();
            }
          }
          DB.set('movements', movs);

          asides[idx].amount = newAmount;
          asides[idx].updated_at = Date.now();
          DB.set('set_asides', asides);

          await syncPhysicalStorage();
          return { success: true, fully_settled: false, remaining: newAmount, amount_paid: targetAmount };
        }
      }
      return { status: 404, error: 'Apartado no encontrado' };
    }

    if (path.match(/^\/api\/set-asides\/([^/]+)\/unpay$/)) {
      const asideId = path.split('/')[3];
      const asides = DB.get('set_asides');
      const idx = asides.findIndex(s => s.id === asideId);
      if (idx >= 0) {
        asides[idx].paid_amount = 0;
        asides[idx].is_paid = 0;
        asides[idx].status = 'apartado';
        asides[idx].updated_at = Date.now();
        DB.set('set_asides', asides);
        return { success: true };
      }
      return { status: 404, error: 'Apartado no encontrado' };
    }

    if (path.match(/^\/api\/set-asides\/([^/]+)\/toggle-fund$/)) {
      const asideId = path.split('/')[3];
      const asides = DB.get('set_asides');
      const idx = asides.findIndex(s => s.id === asideId);
      if (idx >= 0) {
        const cur = asides[idx].fund_type === 'Débito' ? 'Débito' : 'Efectivo';
        const next = cur === 'Efectivo' ? 'Débito' : 'Efectivo';
        asides[idx].fund_type = next;
        asides[idx].updated_at = Date.now();
        DB.set('set_asides', asides);
        return { success: true, newFundType: next };
      }
      return { status: 404, error: 'Registro no encontrado' };
    }

    if (path === '/api/set-asides/transfer') {
      const { card_id, from_type, amount, note } = body;
      const numAmount = parseFloat(amount);
      if (isNaN(numAmount) || numAmount <= 0) {
        return { status: 400, error: 'Monto inválido' };
      }
      const originType = (from_type === 'Débito' || from_type === 'Debito') ? 'Débito' : 'Efectivo';
      const destType = originType === 'Efectivo' ? 'Débito' : 'Efectivo';
      const now = Date.now();
      const today = new Date().toISOString().split('T')[0];

      const people = DB.get('people');
      const personId = people.length > 0 ? people[0].id : 'personal';
      const noteText = note ? ` (${note})` : '';

      const asides = DB.get('set_asides');
      asides.push({
        id: getUUID(),
        card_id,
        person_id: personId,
        amount: -numAmount,
        fund_type: originType,
        note: `Pase a ${destType}${noteText}`,
        date: today,
        updated_at: now
      });
      asides.push({
        id: getUUID(),
        card_id,
        person_id: personId,
        amount: numAmount,
        fund_type: destType,
        note: `Recepción de ${originType}${noteText}`,
        date: today,
        updated_at: now + 1
      });
      DB.set('set_asides', asides);
      return { success: true, transferred: numAmount, from: originType, to: destType };
    }

    // 6. MSI
    if (path.startsWith('/api/msi/')) {
      const parts = path.split('/');
      const cardId = parts[3];
      if (parts[4] === 'advance') {
        const planId = cardId;
        const plans = DB.get('installment_plans');
        const idx = plans.findIndex(p => p.id === planId);
        if (idx >= 0) {
          plans[idx].paid_months = (plans[idx].paid_months || 0) + 1;
          plans[idx].updated_at = Date.now();
          DB.set('installment_plans', plans);
          return { success: true };
        }
        return { status: 404, error: 'Plan no encontrado' };
      }
      if (method === 'DELETE') {
        const planId = cardId;
        DB.set('installment_plans', DB.get('installment_plans').filter(p => p.id !== planId));
        return { success: true };
      }
      if (method === 'GET') {
        const plans = DB.get('installment_plans').filter(p => p.card_id === cardId);
        const people = DB.get('people');
        const peopleMap = Object.fromEntries(people.map(p => [p.id, p.name]));
        const enriched = plans.map(p => ({
          ...p,
          person_name: peopleMap[p.person_id] || 'Desconocido'
        }));
        return enriched.sort((a, b) => (b.start_date || '').localeCompare(a.start_date || ''));
      }
    }

    // 7. PRESUPUESTOS
    if (path === '/api/budgets') {
      if (method === 'GET') {
        return {
          budgets: DB.get('budgets'),
          items: DB.get('budget_items')
        };
      }
      if (method === 'POST') {
        const id = getUUID();
        const budgets = DB.get('budgets');
        budgets.push({
          id,
          name: body.name,
          description: body.description || '',
          updated_at: Date.now()
        });
        DB.set('budgets', budgets);
        return { success: true, id };
      }
    }

    if (path.startsWith('/api/budgets/') && method === 'DELETE') {
      const bId = path.split('/')[3];
      DB.set('budgets', DB.get('budgets').filter(b => b.id !== bId));
      DB.set('budget_items', DB.get('budget_items').filter(bi => bi.budget_id !== bId));
      return { success: true };
    }

    if (path === '/api/budget-items') {
      if (method === 'POST') {
        const id = getUUID();
        const items = DB.get('budget_items');
        items.push({
          id,
          budget_id: body.budget_id,
          concept: body.concept,
          amount: parseFloat(body.amount) || 0,
          type: body.type || 'Fijo',
          tag: body.tag || 'General',
          updated_at: Date.now()
        });
        DB.set('budget_items', items);
        return { success: true, id };
      }
    }

    if (path.startsWith('/api/budget-items/') && method === 'DELETE') {
      const biId = path.split('/')[3];
      DB.set('budget_items', DB.get('budget_items').filter(bi => bi.id !== biId));
      return { success: true };
    }

    // 8. CONFIGURACIÓN E INFLACIÓN
    if (path === '/api/config/inflation') {
      if (method === 'GET') {
        const cfg = DB.getConfig();
        return { inflationRate: parseFloat(cfg.inflation_rate) || 6.0 };
      }
      if (method === 'POST') {
        const rate = parseFloat(body.rate) || 6.0;
        DB.setConfig({ inflation_rate: rate });
        return { success: true, inflationRate: rate };
      }
    }

    // 9. SINCRONIZACIÓN JSON (EXPORTACIÓN / IMPORTACIÓN)
    if (path === '/api/sync/export') {
      return {
        exported_at: Date.now(),
        cards: DB.get('cards'),
        people: DB.get('people'),
        movements: DB.get('movements'),
        installment_plans: DB.get('installment_plans'),
        set_asides: DB.get('set_asides'),
        budgets: DB.get('budgets'),
        budget_items: DB.get('budget_items')
      };
    }

    if (path === '/api/sync/import') {
      let count = 0;
      if (body.cards) {
        const cur = DB.get('cards');
        body.cards.forEach(c => {
          const idx = cur.findIndex(x => x.id === c.id);
          if (idx >= 0) cur[idx] = c; else cur.push(c);
          count++;
        });
        DB.set('cards', cur);
      }
      if (body.people) {
        const cur = DB.get('people');
        body.people.forEach(p => {
          if (!cur.some(x => x.id === p.id)) { cur.push(p); count++; }
        });
        DB.set('people', cur);
      }
      if (body.movements) {
        const cur = DB.get('movements');
        body.movements.forEach(m => {
          if (!cur.some(x => x.id === m.id)) { cur.push(m); count++; }
        });
        DB.set('movements', cur);
      }
      if (body.set_asides) {
        const cur = DB.get('set_asides');
        body.set_asides.forEach(s => {
          if (!cur.some(x => x.id === s.id)) { cur.push(s); count++; }
        });
        DB.set('set_asides', cur);
      }
      return { success: true, processed: count };
    }

    // 10. INFORMACIÓN DE BASE DE DATOS
    if (path === '/api/database/info') {
      let activePath = 'Base de datos privada del dispositivo';
      let statusDesc = 'Almacenamiento Local (Memoria del Teléfono)';
      if (window.AndroidNativeStorage && typeof window.AndroidNativeStorage.getDbFilePath === 'function') {
        activePath = window.AndroidNativeStorage.getDbFilePath('tarjetas.db') || activePath;
        statusDesc = 'Disco Físico Android (tarjetas.db)';
      } else if (window.activeDbFileHandle && window.activeDbFileHandle.name) {
        activePath = 'Archivo vinculado: ' + window.activeDbFileHandle.name;
        statusDesc = 'Disco Local Vinculado';
      }
      return {
        exists: true,
        sizeFormatted: statusDesc,
        dbPath: activePath,
        isDocker: false
      };
    }

    // 11. RESTAURACIÓN DE ARCHIVO FÍSICO SQLite (tarjetas.db)
    if (path === '/api/database/restore' && method === 'POST') {
      try {
        const { dbBase64 } = body;
        if (!dbBase64) return { status: 400, error: 'No se envió contenido del archivo' };

        const base64Data = dbBase64.replace(/^data:.*?;base64,/, '');
        const binaryString = atob(base64Data);
        const len = binaryString.length;
        if (len < 16) {
          return { status: 400, error: 'El archivo es demasiado pequeño para ser una base de datos SQLite' };
        }
        const bytes = new Uint8Array(len);
        for (let i = 0; i < len; i++) {
          bytes[i] = binaryString.charCodeAt(i);
        }

        // Validar cabecera SQLite format 3
        const header = new TextDecoder().decode(bytes.subarray(0, 15));
        if (header !== 'SQLite format 3') {
          return { status: 400, error: 'El archivo no tiene el encabezado válido de SQLite (SQLite format 3)' };
        }

        const initFn = window.initSqlJs || (typeof initSqlJs !== 'undefined' ? initSqlJs : null);
        if (!initFn) {
          return { status: 500, error: 'Motor SQL (sql-asm.js) no cargado' };
        }

        const SQL = await initFn();
        const db = new SQL.Database(bytes);

        // 1. Extraer app_config
        try {
          const cfgRows = db.exec("SELECT key, value FROM app_config");
          if (cfgRows.length && cfgRows[0].values) {
            const cfgObj = {};
            cfgRows[0].values.forEach(([k, v]) => { cfgObj[k] = v; });
            DB.setConfig(cfgObj);
          }
        } catch (e) { console.warn("Aviso app_config:", e); }

        // 2. Extraer cards
        try {
          const res = db.exec("SELECT * FROM cards");
          if (res.length && res[0].values) {
            const cols = res[0].columns;
            DB.set('cards', res[0].values.map(r => {
              const o = {}; cols.forEach((c, idx) => { o[c] = r[idx]; }); return o;
            }));
          } else { DB.set('cards', []); }
        } catch (e) { DB.set('cards', []); }

        // 3. Extraer people
        try {
          const res = db.exec("SELECT * FROM people");
          if (res.length && res[0].values) {
            const cols = res[0].columns;
            DB.set('people', res[0].values.map(r => {
              const o = {}; cols.forEach((c, idx) => { o[c] = r[idx]; }); return o;
            }));
          } else { DB.set('people', []); }
        } catch (e) { DB.set('people', []); }

        // 4. Extraer movements
        try {
          const res = db.exec("SELECT * FROM movements");
          if (res.length && res[0].values) {
            const cols = res[0].columns;
            DB.set('movements', res[0].values.map(r => {
              const o = {}; cols.forEach((c, idx) => { o[c] = r[idx]; }); return o;
            }));
          } else { DB.set('movements', []); }
        } catch (e) { DB.set('movements', []); }

        // 5. Extraer installment_plans
        try {
          const res = db.exec("SELECT * FROM installment_plans");
          if (res.length && res[0].values) {
            const cols = res[0].columns;
            DB.set('installment_plans', res[0].values.map(r => {
              const o = {}; cols.forEach((c, idx) => { o[c] = r[idx]; }); return o;
            }));
          } else { DB.set('installment_plans', []); }
        } catch (e) { DB.set('installment_plans', []); }

        // 6. Extraer set_asides
        try {
          const res = db.exec("SELECT * FROM set_asides");
          if (res.length && res[0].values) {
            const cols = res[0].columns;
            DB.set('set_asides', res[0].values.map(r => {
              const o = {}; cols.forEach((c, idx) => { o[c] = r[idx]; }); return o;
            }));
          } else { DB.set('set_asides', []); }
        } catch (e) { DB.set('set_asides', []); }

        // 7. Extraer budgets
        try {
          const res = db.exec("SELECT * FROM budgets");
          if (res.length && res[0].values) {
            const cols = res[0].columns;
            DB.set('budgets', res[0].values.map(r => {
              const o = {}; cols.forEach((c, idx) => { o[c] = r[idx]; }); return o;
            }));
          } else { DB.set('budgets', []); }
        } catch (e) { DB.set('budgets', []); }

        // 8. Extraer budget_items
        try {
          const res = db.exec("SELECT * FROM budget_items");
          if (res.length && res[0].values) {
            const cols = res[0].columns;
            DB.set('budget_items', res[0].values.map(r => {
              const o = {}; cols.forEach((c, idx) => { o[c] = r[idx]; }); return o;
            }));
          } else { DB.set('budget_items', []); }
        } catch (e) { DB.set('budget_items', []); }

        db.close();

        await syncPhysicalStorage();

        return { success: true, message: 'Base de datos tarjetas.db restaurada exitosamente' };
      } catch (err) {
        console.error('Error restaurando tarjetas.db:', err);
        return { status: 500, error: 'Error procesando archivo: ' + err.message };
      }
    }

    // 12. DESCARGA / EXPORTACIÓN DE BASE DE DATOS FÍSICA SQLite (.db)
    if (path === '/api/database/download' && method === 'GET') {
      try {
        const binary = await generateSqliteBinary();
        if (!binary) return { status: 500, error: 'Error generando binario SQLite' };
        return { isBinaryBlob: true, binaryData: binary };
      } catch (err) {
        return { status: 500, error: 'Error exportando base de datos: ' + err.message };
      }
    }

    return { status: 404, error: 'Ruta no encontrada en motor local' };
  }

  // --- INTERCEPTOR MAESTRO DE FETCH ---
  const _realFetch = window.fetch;
  window.fetch = async function(input, init) {
    let url = typeof input === 'string' ? input : (input && input.url ? input.url : '');
    const method = (init && init.method ? init.method.toUpperCase() : 'GET');

    // Detectar si el usuario configuró una IP remota manualmente
    const customRemoteUrl = localStorage.getItem('cardmaster_server_url') || '';
    const hasCustomRemote = Boolean(customRemoteUrl && !customRemoteUrl.includes('localhost') && !customRemoteUrl.includes('127.0.0.1'));

    // Si hay servidor remoto manual configurado, mandar por red real a esa IP
    if (hasCustomRemote && url.startsWith('/api/')) {
      const fullUrl = customRemoteUrl.replace(/\/+$/, '') + url;
      try {
        return await _realFetch.call(this, fullUrl, init);
      } catch (networkErr) {
        console.warn('Fallo conectando al servidor remoto, intentando motor local...', networkErr);
      }
    }

    // Si es una petición a la API y estamos en el APK o modo local autónomo:
    if (url.startsWith('/api/')) {
      // Intentar primero el servidor real si estamos en localhost con node corriendo (ej. en PC)
      if (window.location.port === '3000') {
        try {
          return await _realFetch.call(this, input, init);
        } catch (e) {
          // Si falla, continuar al motor local
        }
      }

      // Si estamos en file:// intentar conectar directamente a http://127.0.0.1:3000 si Node está corriendo
      if (window.location.protocol === 'file:' || !window.location.origin.startsWith('http')) {
        try {
          const resp = await _realFetch.call(this, 'http://127.0.0.1:3000' + url, {
            ...init,
            mode: 'cors'
          });
          return resp;
        } catch (e) {
          // Si el servidor de Node en 3000 no responde, continuar con el motor local
        }
      }

      // Ejecutar con el motor local autónomo integrado
      let bodyData = {};
      if (init && init.body) {
        try {
          bodyData = typeof init.body === 'string' ? JSON.parse(init.body) : init.body;
        } catch (e) {
          bodyData = {};
        }
      }

      try {
        const result = await handleLocalApi(method, url, bodyData);

        if (result && result.isBinaryBlob) {
          return new Response(new Blob([result.binaryData], { type: 'application/x-sqlite3' }), {
            status: 200,
            statusText: 'OK',
            headers: {
              'Content-Type': 'application/x-sqlite3',
              'Content-Disposition': 'attachment; filename="tarjetas.db"'
            }
          });
        }

        const status = (result && result.status) ? result.status : 200;
        const responseBlob = new Blob([JSON.stringify(result)], { type: 'application/json' });
        return new Response(responseBlob, {
          status: status,
          statusText: status === 200 ? 'OK' : 'Error',
          headers: { 'Content-Type': 'application/json' }
        });
      } catch (err) {
        console.error('Error en motor local:', err);
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { 'Content-Type': 'application/json' }
        });
      }
    }

    // Peticiones estándar de archivos estáticos o red normal
    return _realFetch.call(this, input, init);
  };

  // Auto-cargar base de datos física persistente desde Android si existe en disco
  if (typeof window !== 'undefined') {
    setTimeout(async () => {
      if (window.AndroidNativeStorage && typeof window.AndroidNativeStorage.hasDbFile === 'function') {
        try {
          if (window.AndroidNativeStorage.hasDbFile('tarjetas.db')) {
            const b64 = window.AndroidNativeStorage.loadDbFile('tarjetas.db');
            if (b64 && b64.length > 20) {
              console.log('📱 Auto-sincronizando tarjetas.db persistente en Android...');
              await handleLocalApi('POST', '/api/database/restore', { dbBase64: b64 });
            }
          }
        } catch (e) {
          console.warn('Aviso auto-cargando almacenamiento Android:', e);
        }
      }
    }, 100);
  }

  // Exponer API pública en window para depuración y estado
  window.CardMasterEngine = {
    isLocal: true,
    DB: DB
  };

  console.log('✨ Motor Autónomo Local de CardMaster inicializado con éxito');
})();

