/**
 * CardMaster - Motor de Persistencia y Almacenamiento Local-First (IndexedDB Engine)
 * Modelo 100% Offline y Multiplataforma (Linux, Windows, macOS, Android).
 *
 * Elimina la dependencia de archivos binarios SQLite (.db) y WebAssembly.
 * Todo el almacenamiento vive directamente en IndexedDB de forma transaccional,
 * y los respaldos se exportan e importan en formato JSON plano validado.
 */

(function() {
  'use strict';

  // =========================================================================
  // 1. UTILIDADES CRIPTOGRÁFICAS (Web Crypto API Nativa + Fallback JS)
  // =========================================================================

  function generateSaltHex(len = 16) {
    const arr = new Uint8Array(len);
    if (window.crypto && window.crypto.getRandomValues) {
      window.crypto.getRandomValues(arr);
    } else {
      for (let i = 0; i < len; i++) arr[i] = Math.floor(Math.random() * 256);
    }
    return Array.from(arr).map(b => b.toString(16).padStart(2, '0')).join('');
  }

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
        console.warn("[Crypto] WebCrypto aviso, usando fallback puro JS:", e);
      }
    }
    return pbkdf2_sync_pure(password, saltHex, 100000, 32);
  }

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

  async function verifyPassword(password, expectedHash, saltHex) {
    if (!password || !expectedHash || !saltHex) return false;
    const hash = await hashPasswordPbkdf2(password, saltHex);
    if (hash === expectedHash) return true;
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

  // =========================================================================
  // 2. CAPA DE PERSISTENCIA LOCAL: INDEXEDDB ENGINE (CardMasterDB)
  // =========================================================================

  const DB_NAME = 'CardMaster_LocalDB';
  const DB_VERSION = 1;
  const STORE_NAMES = [
    'cards',
    'people',
    'movements',
    'installment_plans',
    'set_asides',
    'budgets',
    'budget_items',
    'app_config'
  ];

  // Datos iniciales de arranque (totalmente editables y eliminables por el usuario)
  const SEED_DATA = {
    cards: [
      { id: "card-bbva-platinum", name: "BBVA Platinum", credit_limit: 509700, cutoff_day: 16, color: "#004481", logo_base64: null, display_order: 0, updated_at: 1791844528401 },
      { id: "card-stori", name: "Stori", credit_limit: 20000, cutoff_day: 12, color: "#00a86b", logo_base64: null, display_order: 1, updated_at: 1791844528401 },
      { id: "card-banamex-costco", name: "Banamex Costco", credit_limit: 58000, cutoff_day: 8, color: "#0284c7", logo_base64: null, display_order: 2, updated_at: 1791844528401 },
      { id: "card-hsbc-viva", name: "HSBC VIVA", credit_limit: 150000, cutoff_day: 11, color: "#db0011", logo_base64: null, display_order: 3, updated_at: 1791844528401 },
      { id: "card-nu", name: "NU", credit_limit: 4000, cutoff_day: 23, color: "#820ad1", logo_base64: null, display_order: 4, updated_at: 1791844528401 },
      { id: "card-rappi-card", name: "Rappi Card", credit_limit: 8000, cutoff_day: 21, color: "#ff441f", logo_base64: null, display_order: 5, updated_at: 1791844528401 }
    ],
    people: [
      { id: "person-personal", name: "Personal", updated_at: 1791844528401 },
      { id: "person-plj", name: "PLJ", updated_at: 1791844528401 },
      { id: "person-blj", name: "BLJ", updated_at: 1791844528401 },
      { id: "person-gjg", name: "GJG", updated_at: 1791844528401 },
      { id: "person-elh", name: "ELH", updated_at: 1791844528401 },
      { id: "person-jess", name: "Jess", updated_at: 1791844528401 },
      { id: "person-gaby", name: "Gaby", updated_at: 1791844528401 }
    ],
    movements: [],
    installment_plans: [],
    set_asides: [],
    budgets: [],
    budget_items: [],
    app_config: { inflation_rate: "4.5" }
  };

  // Caché síncrona en memoria para lectura/escritura instantánea y resiliente
  const memStore = {
    cards: [],
    people: [],
    movements: [],
    installment_plans: [],
    set_asides: [],
    budgets: [],
    budget_items: [],
    app_config: { inflation_rate: '4.5' }
  };

  function loadMemFromLocalStorage() {
    try {
      STORE_NAMES.forEach(s => {
        const val = localStorage.getItem('cm_store_' + s);
        if (val) {
          try { memStore[s] = JSON.parse(val); } catch (e) {}
        }
      });
      // Compatibilidad con keys previas si existen
      const oldCards = localStorage.getItem('cm_data_cards');
      if (oldCards && (!memStore.cards || memStore.cards.length === 0)) {
        try {
          STORE_NAMES.forEach(s => {
            const v = localStorage.getItem('cm_data_' + s);
            if (v) memStore[s] = JSON.parse(v);
          });
          const c = localStorage.getItem('cm_data_config');
          if (c) memStore.app_config = Object.assign(memStore.app_config || {}, JSON.parse(c));
        } catch (e) {}
      }

      // Si la base de datos local está limpia o sin inicializar, sembrar datos de inicio
      const seedApplied = localStorage.getItem('cm_seed_applied');
      if (!seedApplied && (!memStore.cards || memStore.cards.length === 0)) {
        memStore.cards = JSON.parse(JSON.stringify(SEED_DATA.cards));
        memStore.people = JSON.parse(JSON.stringify(SEED_DATA.people));
        memStore.movements = JSON.parse(JSON.stringify(SEED_DATA.movements));
        memStore.set_asides = JSON.parse(JSON.stringify(SEED_DATA.set_asides));
        memStore.installment_plans = [];
        memStore.budgets = [];
        memStore.budget_items = [];
        memStore.app_config = Object.assign({ inflation_rate: '4.5' }, memStore.app_config || {});
        localStorage.setItem('cm_seed_applied', '1');
        saveMemToLocalStorage();
      }

      // Garantizar que la persona 'Personal' exista si la lista está vacía
      if (!Array.isArray(memStore.people) || memStore.people.length === 0) {
        memStore.people = [{ id: 'p-personal-default', name: 'Personal', updated_at: Date.now() }];
      }
      if (!memStore.app_config || typeof memStore.app_config !== 'object') {
        memStore.app_config = { inflation_rate: '4.5' };
      }
    } catch (e) {
      console.warn('[Storage] Error cargando desde localStorage:', e);
    }
  }

  function saveMemToLocalStorage(storeName) {
    try {
      if (storeName) {
        localStorage.setItem('cm_store_' + storeName, JSON.stringify(memStore[storeName] || []));
      } else {
        STORE_NAMES.forEach(s => {
          localStorage.setItem('cm_store_' + s, JSON.stringify(memStore[s] || []));
        });
      }
    } catch (e) {}
  }

  // Cargar inmediatamente de forma síncrona
  loadMemFromLocalStorage();

  class IndexedDBManager {
    constructor() {
      this.db = null;
      this._initPromise = null;
      this._isReady = false;
    }

    async init() {
      if (this.db) return this.db;
      if (this._initPromise) return this._initPromise;

      this._initPromise = new Promise((resolve) => {
        if (!window.indexedDB) {
          console.warn('[CardMasterDB] IndexedDB no disponible en este entorno, operando en memoria y localStorage.');
          this._isReady = true;
          return resolve(null);
        }

        try {
          const req = indexedDB.open(DB_NAME, DB_VERSION);

          req.onupgradeneeded = (event) => {
            const db = event.target.result;
            STORE_NAMES.forEach(store => {
              if (!db.objectStoreNames.contains(store)) {
                if (store === 'app_config') {
                  db.createObjectStore(store, { keyPath: 'key' });
                } else {
                  db.createObjectStore(store, { keyPath: 'id' });
                }
              }
            });
          };

          req.onsuccess = (event) => {
            this.db = event.target.result;
            this._isReady = true;
            // Sincronizar bidireccionalmente con IndexedDB en segundo plano sin bloquear
            this._syncWithIndexedDB().catch(e => console.warn('[CardMasterDB] Sincronización IDB aviso:', e));
            resolve(this.db);
          };

          req.onerror = (event) => {
            console.warn('[CardMasterDB] Error abriendo IndexedDB, operando con almacenamiento seguro:', event.target.error);
            this._isReady = true;
            resolve(null);
          };

          req.onblocked = () => {
            console.warn('[CardMasterDB] Apertura de IndexedDB bloqueada temporalmente.');
            this._isReady = true;
            resolve(this.db);
          };
        } catch (err) {
          console.warn('[CardMasterDB] Excepción abriendo IndexedDB:', err);
          this._isReady = true;
          resolve(null);
        }
      });

      return this._initPromise;
    }

    async _syncWithIndexedDB() {
      if (!this.db) return;
      try {
        const tx = this.db.transaction(STORE_NAMES, 'readonly');
        const readPromises = STORE_NAMES.map(storeName => {
          return new Promise(res => {
            const store = tx.objectStore(storeName);
            const req = store.getAll();
            req.onsuccess = () => res({ storeName, items: req.result || [] });
            req.onerror = () => res({ storeName, items: [] });
          });
        });
        const results = await Promise.all(readPromises);
        let hasCardsOrMovements = false;
        results.forEach(({ storeName, items }) => {
          if ((storeName === 'cards' || storeName === 'movements') && items && items.length > 0) {
            hasCardsOrMovements = true;
          }
        });

        if (hasCardsOrMovements) {
          // IndexedDB tiene tarjetas o movimientos reales: hidratar memStore y localStorage
          results.forEach(({ storeName, items }) => {
            if (storeName === 'app_config') {
              const cfg = {};
              items.forEach(it => { cfg[it.key] = it.value; });
              memStore.app_config = Object.assign({}, memStore.app_config, cfg);
            } else {
              memStore[storeName] = items;
            }
          });
          saveMemToLocalStorage();
        } else {
          // IndexedDB está vacío o sin tarjetas: volcar memStore (con datos semilla) en IndexedDB
          const writeTx = this.db.transaction(STORE_NAMES, 'readwrite');
          STORE_NAMES.forEach(storeName => {
            const s = writeTx.objectStore(storeName);
            if (storeName === 'app_config') {
              Object.entries(memStore.app_config || {}).forEach(([k, v]) => {
                s.put({ key: k, value: String(v) });
              });
            } else {
              (memStore[storeName] || []).forEach(it => s.put(it));
            }
          });
        }
      } catch (e) {
        console.warn('[CardMasterDB] Error en sincronización inicial IDB:', e);
      }
    }

    async getAll(storeName) {
      this.init();
      if (storeName === 'app_config') {
        return Object.entries(memStore.app_config || {}).map(([key, value]) => ({ key, value }));
      }
      return (memStore[storeName] || []).slice();
    }

    async get(storeName, key) {
      this.init();
      if (storeName === 'app_config') {
        const val = (memStore.app_config || {})[key];
        return val !== undefined ? { key, value: val } : null;
      }
      const list = memStore[storeName] || [];
      return list.find(it => it.id === key) || null;
    }

    async put(storeName, item) {
      this.init();
      if (storeName === 'app_config') {
        if (!memStore.app_config) memStore.app_config = {};
        memStore.app_config[item.key] = item.value;
        saveMemToLocalStorage('app_config');
      } else {
        if (!memStore[storeName]) memStore[storeName] = [];
        const idx = memStore[storeName].findIndex(it => it.id === item.id);
        if (idx >= 0) {
          memStore[storeName][idx] = item;
        } else {
          memStore[storeName].push(item);
        }
        saveMemToLocalStorage(storeName);
      }

      if (this.db) {
        try {
          const tx = this.db.transaction(storeName, 'readwrite');
          tx.objectStore(storeName).put(item);
        } catch (e) {
          console.warn('[CardMasterDB] Error al persistir en IDB:', e);
        }
      }
      return item;
    }

    async delete(storeName, key) {
      this.init();
      if (storeName === 'app_config') {
        if (memStore.app_config) delete memStore.app_config[key];
        saveMemToLocalStorage('app_config');
      } else {
        if (memStore[storeName]) {
          memStore[storeName] = memStore[storeName].filter(it => it.id !== key);
          saveMemToLocalStorage(storeName);
        }
      }

      if (this.db) {
        try {
          const tx = this.db.transaction(storeName, 'readwrite');
          tx.objectStore(storeName).delete(key);
        } catch (e) {
          console.warn('[CardMasterDB] Error al eliminar en IDB:', e);
        }
      }
      return true;
    }

    async clear(storeName) {
      this.init();
      if (storeName === 'app_config') {
        memStore.app_config = {};
        saveMemToLocalStorage('app_config');
      } else {
        memStore[storeName] = [];
        saveMemToLocalStorage(storeName);
      }

      if (this.db) {
        try {
          const tx = this.db.transaction(storeName, 'readwrite');
          tx.objectStore(storeName).clear();
        } catch (e) {
          console.warn('[CardMasterDB] Error al limpiar en IDB:', e);
        }
      }
      return true;
    }

    async bulkPut(storeName, items) {
      this.init();
      if (!items || items.length === 0) return 0;
      items.forEach(item => {
        if (storeName === 'app_config') {
          if (!memStore.app_config) memStore.app_config = {};
          memStore.app_config[item.key] = item.value;
        } else {
          if (!memStore[storeName]) memStore[storeName] = [];
          const idx = memStore[storeName].findIndex(it => it.id === item.id);
          if (idx >= 0) memStore[storeName][idx] = item;
          else memStore[storeName].push(item);
        }
      });
      saveMemToLocalStorage(storeName);

      if (this.db) {
        try {
          const tx = this.db.transaction(storeName, 'readwrite');
          const s = tx.objectStore(storeName);
          items.forEach(it => s.put(it));
        } catch (e) {
          console.warn('[CardMasterDB] Error bulkPut en IDB:', e);
        }
      }
      return items.length;
    }

    async getConfig() {
      return Object.assign({}, memStore.app_config || {});
    }

    async setConfig(obj) {
      this.init();
      if (!memStore.app_config) memStore.app_config = {};
      Object.entries(obj).forEach(([k, v]) => {
        memStore.app_config[k] = String(v);
      });
      saveMemToLocalStorage('app_config');

      if (this.db) {
        try {
          const tx = this.db.transaction('app_config', 'readwrite');
          const s = tx.objectStore('app_config');
          Object.entries(obj).forEach(([k, v]) => {
            s.put({ key: k, value: String(v) });
          });
        } catch (e) {
          console.warn('[CardMasterDB] Error setConfig en IDB:', e);
        }
      }
      return true;
    }

    async isDatabaseEmpty() {
      const cards = memStore.cards || [];
      const movs = memStore.movements || [];
      const cfg = memStore.app_config || {};
      return (cards.length === 0 && movs.length === 0 && !cfg.master_pwd_hash);
    }

    async getDatabaseInfo() {
      const cards = memStore.cards || [];
      const people = memStore.people || [];
      const movs = memStore.movements || [];
      const plans = memStore.installment_plans || [];
      const setAsides = memStore.set_asides || [];
      const cfg = memStore.app_config || {};
      const isEmpty = (cards.length === 0 && movs.length === 0 && !cfg.master_pwd_hash);

      let sizeFormatted = 'Almacenamiento Local (IndexedDB Seguro)';
      try {
        if (navigator.storage && navigator.storage.estimate) {
          const est = await navigator.storage.estimate();
          if (est.usage) {
            sizeFormatted = (est.usage / 1024).toFixed(1) + ' KB en disco (IndexedDB)';
          }
        }
      } catch (e) {}

      return {
        exists: true,
        isEmpty: isEmpty,
        storageType: 'IndexedDB',
        dbPath: 'IndexedDB (Almacenamiento Local Offline)',
        sizeFormatted: sizeFormatted,
        isConfigured: Boolean(cfg.master_pwd_hash && cfg.master_pwd_salt),
        counts: {
          cards: cards.length,
          people: people.length,
          movements: movs.length,
          installment_plans: plans.length,
          set_asides: setAsides.length
        },
        isDocker: false
      };
    }

    async exportDump() {
      const now = Date.now();
      return {
        app: 'CardMaster',
        version: '2.0',
        format: 'cardmaster-json-backup',
        schema_version: 1,
        exported_at: now,
        exported_iso: new Date(now).toISOString(),
        cards: (memStore.cards || []).slice(),
        people: (memStore.people || []).slice(),
        movements: (memStore.movements || []).slice(),
        installment_plans: (memStore.installment_plans || []).slice(),
        set_asides: (memStore.set_asides || []).slice(),
        budgets: (memStore.budgets || []).slice(),
        budget_items: (memStore.budget_items || []).slice(),
        config: Object.assign({}, memStore.app_config || {})
      };
    }

    validateSchema(data) {
      if (!data || typeof data !== 'object') {
        return { valid: false, error: 'El archivo no contiene un JSON estructurado válido.' };
      }

      const hasSignature = data.app === 'CardMaster' || data.format === 'cardmaster-json-backup';
      const hasCards = Array.isArray(data.cards);
      const hasMovements = Array.isArray(data.movements);
      const hasPeople = Array.isArray(data.people);
      const hasConfig = data.config && typeof data.config === 'object';

      if (!hasSignature && !hasCards && !hasMovements && !hasPeople && !hasConfig) {
        return { valid: false, error: 'El archivo no corresponde a una base de datos o respaldo de CardMaster.' };
      }

      const arrayKeys = ['cards', 'people', 'movements', 'installment_plans', 'set_asides', 'budgets', 'budget_items'];
      for (const k of arrayKeys) {
        if (data[k] !== undefined && !Array.isArray(data[k])) {
          return { valid: false, error: `La sección "${k}" no tiene un formato de lista válido.` };
        }
      }

      if (hasCards) {
        for (let i = 0; i < data.cards.length; i++) {
          const c = data.cards[i];
          if (!c || typeof c !== 'object' || !c.name) {
            return { valid: false, error: `Tarjeta en posición ${i + 1} no tiene un nombre válido.` };
          }
        }
      }

      if (hasMovements) {
        for (let i = 0; i < data.movements.length; i++) {
          const m = data.movements[i];
          if (!m || typeof m !== 'object' || m.concept === undefined || m.amount === undefined) {
            return { valid: false, error: `Movimiento en posición ${i + 1} requiere concepto y monto válidos.` };
          }
        }
      }

      return { valid: true };
    }

    async importDump(data, { overwrite = true } = {}) {
      const validation = this.validateSchema(data);
      if (!validation.valid) {
        throw new Error(validation.error);
      }

      if (overwrite) {
        STORE_NAMES.forEach(s => {
          if (s === 'app_config') memStore.app_config = {};
          else memStore[s] = [];
        });
      }

      if (Array.isArray(data.cards)) memStore.cards = data.cards.slice();
      if (Array.isArray(data.people)) memStore.people = data.people.slice();
      if (Array.isArray(data.movements)) memStore.movements = data.movements.slice();
      if (Array.isArray(data.installment_plans)) memStore.installment_plans = data.installment_plans.slice();
      if (Array.isArray(data.set_asides)) memStore.set_asides = data.set_asides.slice();
      if (Array.isArray(data.budgets)) memStore.budgets = data.budgets.slice();
      if (Array.isArray(data.budget_items)) memStore.budget_items = data.budget_items.slice();
      if (data.config && typeof data.config === 'object') {
        memStore.app_config = Object.assign({}, data.config);
      }

      // Asegurar persona Personal e inflación por defecto
      if (!memStore.people || memStore.people.length === 0 || !memStore.people.some(p => p.name === 'Personal')) {
        if (!memStore.people) memStore.people = [];
        memStore.people.unshift({ id: getUUID(), name: 'Personal', updated_at: Date.now() });
      }
      if (!memStore.app_config) memStore.app_config = {};
      if (!memStore.app_config.inflation_rate) memStore.app_config.inflation_rate = '10';

      saveMemToLocalStorage();

      this.init();
      if (this.db) {
        try {
          const tx = this.db.transaction(STORE_NAMES, 'readwrite');
          if (overwrite) {
            STORE_NAMES.forEach(s => tx.objectStore(s).clear());
          }
          STORE_NAMES.forEach(s => {
            const store = tx.objectStore(s);
            if (s === 'app_config') {
              Object.entries(memStore.app_config).forEach(([k, v]) => store.put({ key: k, value: String(v) }));
            } else {
              (memStore[s] || []).forEach(it => store.put(it));
            }
          });
        } catch (e) {
          console.warn('[CardMasterDB] Error persistiendo dump en IDB:', e);
        }
      }

      return {
        success: true,
        counts: {
          cards: (memStore.cards || []).length,
          people: (memStore.people || []).length,
          movements: (memStore.movements || []).length,
          installment_plans: (memStore.installment_plans || []).length,
          set_asides: (memStore.set_asides || []).length,
          budgets: (memStore.budgets || []).length,
          budget_items: (memStore.budget_items || []).length
        }
      };
    }

    async factoryReset() {
      STORE_NAMES.forEach(s => {
        if (s === 'app_config') memStore.app_config = { inflation_rate: '10' };
        else if (s === 'people') memStore.people = [{ id: getUUID(), name: 'Personal', updated_at: Date.now() }];
        else memStore[s] = [];
      });

      try {
        const keys = Object.keys(localStorage);
        keys.forEach(k => {
          if (k.startsWith('cm_') || k.startsWith('cardmaster_store_')) localStorage.removeItem(k);
        });
      } catch (e) {}

      saveMemToLocalStorage();

      this.init();
      if (this.db) {
        try {
          const tx = this.db.transaction(STORE_NAMES, 'readwrite');
          STORE_NAMES.forEach(s => tx.objectStore(s).clear());
          tx.objectStore('people').put({ id: getUUID(), name: 'Personal', updated_at: Date.now() });
          tx.objectStore('app_config').put({ key: 'inflation_rate', value: '10' });
        } catch (e) {
          console.warn('[CardMasterDB] Error al resetear IDB:', e);
        }
      }
      return true;
    }
  }

  // Instancia única compartida
  const dbInstance = new IndexedDBManager();
  window.CardMasterDB = dbInstance;

  // =========================================================================
  // 5. FUNCIONES MULTIPLATAFORMA DE EXPORTACIÓN E IMPORTACIÓN DE ARCHIVOS
  // =========================================================================

  /**
   * Exporta toda la base de datos a un archivo JSON plano usando Web Share API,
   * File System Access API o Blob Download Link.
   */
  async function exportDatabaseFile() {
    const dump = await dbInstance.exportDump();
    const jsonStr = JSON.stringify(dump, null, 2);
    const nowStr = new Date().toISOString().slice(0, 10);
    const fileName = `cardmaster_backup_${nowStr}.json`;

    // 1. Puente Android SAF nativo (Selector oficial de Android: ACTION_CREATE_DOCUMENT)
    // Permite al usuario guardar el respaldo en CUALQUIER lugar de su celular (Descargas, Documentos, Drive, SD)
    // SIN cambiar ni mover jamás la base de datos interna de la app.
    if (window.AndroidNativeStorage && typeof window.AndroidNativeStorage.exportJson === 'function') {
      return new Promise((resolve) => {
        window.onAndroidExportCompleted = function(res) {
          if (typeof res === 'string') {
            try { res = JSON.parse(res); } catch (e) {}
          }
          resolve(res || { success: true, method: 'android-saf', fileName });
        };
        try {
          window.AndroidNativeStorage.exportJson(jsonStr, fileName);
        } catch (e) {
          console.warn('[Export] Error en puente nativo Android SAF:', e);
        }
      });
    }

    // 2. Web Share API (Ideal para compartir por WhatsApp / Drive / Correo si no está en puente nativo)
    try {
      if (navigator.share && navigator.canShare && typeof File !== 'undefined') {
        const file = new File([jsonStr], fileName, { type: 'application/json' });
        if (navigator.canShare({ files: [file] })) {
          await navigator.share({
            title: 'Respaldo CardMaster',
            text: `Respaldo financiero de CardMaster (${nowStr})`,
            files: [file]
          });
          return { success: true, method: 'share', fileName };
        }
      }
    } catch (shareErr) {
      if (shareErr.name === 'AbortError') {
        return { success: false, cancelled: true };
      }
      console.warn('[Export] Web Share API cancelada o no admitida, usando método alternativo:', shareErr);
    }

    // 3. File System Access API (Chromium en Desktop)
    if (window.showSaveFilePicker) {
      try {
        const handle = await window.showSaveFilePicker({
          suggestedName: fileName,
          types: [{
            description: 'Respaldo JSON de CardMaster (*.json)',
            accept: { 'application/json': ['.json'] }
          }]
        });
        const writable = await handle.createWritable();
        await writable.write(jsonStr);
        await writable.close();
        return { success: true, method: 'file-picker', fileName: handle.name };
      } catch (pickerErr) {
        if (pickerErr.name === 'AbortError') {
          return { success: false, cancelled: true };
        }
        console.warn('[Export] showSaveFilePicker no disponible o cancelado:', pickerErr);
      }
    }

    // 4. Fallback universal garantizado: Blob + enlace <a>
    const blob = new Blob([jsonStr], { type: 'application/json;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      URL.revokeObjectURL(url);
      a.remove();
    }, 1000);

    return { success: true, method: 'download', fileName };
  }

  /**
   * Importa y valida un archivo o string JSON en IndexedDB.
   */
  async function importDatabaseFile(fileOrData, options = { overwrite: true }) {
    let jsonStr = '';

    if (typeof fileOrData === 'string') {
      jsonStr = fileOrData;
    } else if (fileOrData instanceof File || fileOrData instanceof Blob) {
      if (fileOrData.text) {
        jsonStr = await fileOrData.text();
      } else {
        jsonStr = await new Promise((resolve, reject) => {
          const reader = new FileReader();
          reader.onload = () => resolve(reader.result);
          reader.onerror = () => reject(reader.error);
          reader.readAsText(fileOrData, 'utf-8');
        });
      }
    } else if (typeof fileOrData === 'object' && fileOrData !== null) {
      return await dbInstance.importDump(fileOrData, options);
    } else {
      throw new Error('Formato de entrada no admitido para la importación.');
    }

    let parsed = null;
    try {
      parsed = JSON.parse(jsonStr);
    } catch (e) {
      throw new Error('El archivo seleccionado no tiene formato JSON válido: ' + e.message);
    }

    return await dbInstance.importDump(parsed, options);
  }

  window.exportDatabaseFile = exportDatabaseFile;
  window.importDatabaseFile = importDatabaseFile;

  // =========================================================================
  // 6. ENRUTADOR ASÍNCRONO DE LA API LOCAL (COMPATIBLE CON NAVEGADORES Y WEBVIEWS)
  // =========================================================================

  async function handleLocalApi(method, url, body) {
    let path = url;
    try {
      if (url.startsWith('http://') || url.startsWith('https://')) {
        path = new URL(url).pathname;
      }
    } catch (e) {}
    path = path.split('?')[0];

    // --- 1. AUTENTICACIÓN ---
    if (path === '/api/auth/status') {
      try {
        const cfg = await dbInstance.getConfig();
        return { isConfigured: Boolean(cfg && cfg.master_pwd_hash && cfg.master_pwd_salt) };
      } catch (e) {
        return { isConfigured: false };
      }
    }

    if (path === '/api/auth/setup') {
      const { password } = body;
      if (!password || !password.trim()) {
        return { status: 400, error: 'La contraseña no puede estar vacía' };
      }
      const salt = generateSaltHex(16);
      const hash = await hashPasswordPbkdf2(password, salt);
      await dbInstance.setConfig({ master_pwd_hash: hash, master_pwd_salt: salt });
      return { success: true };
    }

    if (path === '/api/auth/login') {
      const { password } = body;
      const cfg = await dbInstance.getConfig();
      if (!cfg.master_pwd_hash || !cfg.master_pwd_salt) {
        return { status: 400, error: 'No configurado' };
      }
      const ok = await verifyPassword(password, cfg.master_pwd_hash, cfg.master_pwd_salt);
      if (ok) {
        const standardHash = await hashPasswordPbkdf2(password, cfg.master_pwd_salt);
        if (cfg.master_pwd_hash !== standardHash) {
          await dbInstance.setConfig({ master_pwd_hash: standardHash });
        }
        return { success: true };
      }
      return { status: 401, error: 'Contraseña incorrecta' };
    }

    if (path === '/api/auth/change-pwd') {
      const { oldPassword, newPassword } = body;
      const cfg = await dbInstance.getConfig();
      const ok = await verifyPassword(oldPassword, cfg.master_pwd_hash, cfg.master_pwd_salt);
      if (!ok) {
        return { status: 401, error: 'Contraseña actual incorrecta' };
      }
      const newSalt = generateSaltHex(16);
      const newHash = await hashPasswordPbkdf2(newPassword, newSalt);
      await dbInstance.setConfig({ master_pwd_hash: newHash, master_pwd_salt: newSalt });
      return { success: true };
    }

    // --- 2. TARJETAS ---
    if (path === '/api/cards') {
      if (method === 'GET') {
        const cards = await dbInstance.getAll('cards');
        return cards.sort((a, b) => a.name.localeCompare(b.name));
      }
      if (method === 'POST') {
        const cardId = body.id || getUUID();
        const cardObj = {
          id: cardId,
          name: body.name,
          credit_limit: parseFloat(body.credit_limit) || 0,
          cutoff_day: parseInt(body.cutoff_day) || 1,
          color: body.color || '#1e293b',
          logo_base64: body.logo_base64 || null,
          updated_at: Date.now()
        };
        await dbInstance.put('cards', cardObj);
        return { success: true, id: cardId };
      }
    }

    if (path.match(/^\/api\/cards\/([^/]+)$/) && (method === 'PUT' || method === 'POST')) {
      const cardId = path.split('/')[3];
      const existing = await dbInstance.get('cards', cardId) || {};
      const cardObj = {
        ...existing,
        id: cardId,
        name: body.name !== undefined ? body.name : existing.name,
        credit_limit: body.credit_limit !== undefined ? (parseFloat(body.credit_limit) || 0) : existing.credit_limit,
        cutoff_day: body.cutoff_day !== undefined ? (parseInt(body.cutoff_day) || 1) : existing.cutoff_day,
        color: body.color || existing.color || '#1e293b',
        logo_base64: body.logo_base64 !== undefined ? body.logo_base64 : existing.logo_base64,
        updated_at: Date.now()
      };
      await dbInstance.put('cards', cardObj);
      return { success: true, id: cardId };
    }

    if (path.match(/^\/api\/cards\/([^/]+)\/delete$/) || (path.match(/^\/api\/cards\/([^/]+)$/) && method === 'DELETE')) {
      const cardId = path.split('/')[3];
      const { password } = body || {};
      const cfg = await dbInstance.getConfig();
      if (password) {
        const ok = await verifyPassword(password, cfg.master_pwd_hash, cfg.master_pwd_salt);
        if (!ok) {
          return { status: 401, error: 'Contraseña incorrecta. No se eliminó la tarjeta.' };
        }
      }

      await dbInstance.delete('cards', cardId);

      // Limpiar movimientos asociados en cascada
      const movs = await dbInstance.getAll('movements');
      for (const m of movs) {
        if (m.card_id === cardId) await dbInstance.delete('movements', m.id);
      }
      const plans = await dbInstance.getAll('installment_plans');
      for (const p of plans) {
        if (p.card_id === cardId) await dbInstance.delete('installment_plans', p.id);
      }
      const asides = await dbInstance.getAll('set_asides');
      for (const s of asides) {
        if (s.card_id === cardId) await dbInstance.delete('set_asides', s.id);
      }

      return { success: true };
    }

    if (path === '/api/cards/reset') {
      const { password } = body;
      const cfg = await dbInstance.getConfig();
      const ok = await verifyPassword(password, cfg.master_pwd_hash, cfg.master_pwd_salt);
      if (!ok) {
        return { status: 401, error: 'Contraseña incorrecta' };
      }
      await dbInstance.clear('movements');
      await dbInstance.clear('set_asides');
      return { success: true };
    }

    if (path === '/api/system/factory-reset') {
      const { password } = body;
      const cfg = await dbInstance.getConfig();
      if (!cfg.master_pwd_hash || !cfg.master_pwd_salt) {
        return { status: 400, error: 'La base de datos no está configurada' };
      }
      const ok = await verifyPassword(password, cfg.master_pwd_hash, cfg.master_pwd_salt);
      if (!ok) {
        return { status: 401, error: 'Contraseña incorrecta. No se pudo restaurar de fábrica.' };
      }
      await dbInstance.factoryReset();
      return { success: true, message: 'Base de datos restaurada de fábrica con éxito' };
    }

    // --- 3. PERSONAS ---
    if (path === '/api/people') {
      if (method === 'GET') {
        const people = await dbInstance.getAll('people');
        return people.sort((a, b) => a.name.localeCompare(b.name));
      }
      if (method === 'POST') {
        const people = await dbInstance.getAll('people');
        const nameTrimmed = (body.name || '').trim();
        const parentId = body.parent_id || null;
        if (people.some(p => p.name.toLowerCase() === nameTrimmed.toLowerCase() && (p.parent_id || null) === parentId)) {
          return { status: 400, error: 'Ya existe esa persona o subetiqueta' };
        }
        const id = getUUID();
        await dbInstance.put('people', { id, name: nameTrimmed, parent_id: parentId, updated_at: Date.now() });
        return { success: true, id };
      }
    }

    if (path.startsWith('/api/people/')) {
      const pId = path.split('/')[3];
      if (method === 'PUT') {
        const existing = await dbInstance.get('people', pId);
        if (existing) {
          existing.name = (body.name || existing.name).trim();
          if (body.parent_id !== undefined) existing.parent_id = body.parent_id || null;
          existing.updated_at = Date.now();
          await dbInstance.put('people', existing);
          return { success: true, id: pId };
        }
        return { status: 404, error: 'Persona no encontrada' };
      }
      // Eliminar también subpersonas si las tiene
      const allPeople = await dbInstance.getAll('people');
      for (const p of allPeople) {
        if (p.parent_id === pId) {
          await dbInstance.delete('people', p.id);
        }
      }
      await dbInstance.delete('people', pId);
      return { success: true };
    }

    // --- 4. MOVIMIENTOS ---
    if (path === '/api/movements') {
      if (method === 'GET') {
        const [movs, cards, people, asides] = await Promise.all([
          dbInstance.getAll('movements'),
          dbInstance.getAll('cards'),
          dbInstance.getAll('people'),
          dbInstance.getAll('set_asides')
        ]);

        const cardsMap = Object.fromEntries(cards.map(c => [c.id, c.name]));
        const peopleMap = Object.fromEntries(people.map(p => [p.id, p.name]));

        // Reconciliación FIFO de apartados hacia movimientos (agrupados por persona y tarjeta)
        const sortedMovs = [...movs].sort((a, b) => (a.date || '').localeCompare(b.date || ''));
        const directAsideMap = {};
        const directPaidMap = {};
        const unallocatedByPersonAndCard = {};
        const unallocatedPaidByPersonAndCard = {};

        asides.forEach(s => {
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

        const enriched = sortedMovs.map(m => {
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
            sub_person_id: m.sub_person_id || null,
            card_name: cardsMap[m.card_id] || 'Desconocida',
            person_name: peopleMap[m.person_id] || 'Desconocido',
            sub_person_name: m.sub_person_id ? (peopleMap[m.sub_person_id] || '') : '',
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
          await dbInstance.put('installment_plans', {
            id: msiId,
            concept: body.concept,
            total_amount: parseFloat(body.amount) || 0,
            months: parseInt(body.msi_months) || 12,
            start_date: body.date,
            card_id: body.card_id,
            person_id: body.person_id,
            sub_person_id: body.sub_person_id || null,
            paid_months: 0,
            updated_at: now
          });
          return { success: true, msiId };
        } else {
          const movId = getUUID();
          await dbInstance.put('movements', {
            id: movId,
            concept: body.concept,
            amount: parseFloat(body.amount) || 0,
            date: body.date,
            card_id: body.card_id,
            person_id: body.person_id,
            sub_person_id: body.sub_person_id || null,
            is_set_aside: 0,
            updated_at: now
          });
          return { success: true, id: movId };
        }
      }
    }

    if (path.match(/^\/api\/movements\/([^/]+)$/) && (method === 'PUT' || method === 'POST')) {
      const movId = path.split('/')[3];
      const existing = await dbInstance.get('movements', movId);
      if (existing) {
        const now = Date.now();
        const updated = {
          ...existing,
          concept: body.concept,
          amount: parseFloat(body.amount) || 0,
          date: body.date,
          card_id: body.card_id,
          person_id: body.person_id,
          sub_person_id: body.sub_person_id !== undefined ? body.sub_person_id : (existing.sub_person_id || null),
          updated_at: now
        };
        await dbInstance.put('movements', updated);
        // Sincronizar apartados directos si existen
        try {
          const asides = await dbInstance.getAll('set_asides');
          for (const s of asides) {
            if (String(s.movement_id) === String(movId)) {
              s.card_id = updated.card_id;
              s.person_id = updated.person_id;
              s.updated_at = now;
              await dbInstance.put('set_asides', s);
            }
          }
        } catch (e) {}
        return { success: true, changes: 1 };
      }
      return { status: 404, error: 'Movimiento no encontrado' };
    }

    if (path.match(/^\/api\/movements\/([^/]+)\/update$/)) {
      const movId = path.split('/')[3];
      const existing = await dbInstance.get('movements', movId);
      if (existing) {
        const now = Date.now();
        const updated = {
          ...existing,
          concept: body.concept,
          amount: parseFloat(body.amount) || 0,
          date: body.date,
          card_id: body.card_id,
          person_id: body.person_id,
          sub_person_id: body.sub_person_id !== undefined ? body.sub_person_id : (existing.sub_person_id || null),
          updated_at: now
        };
        await dbInstance.put('movements', updated);
        try {
          const asides = await dbInstance.getAll('set_asides');
          for (const s of asides) {
            if (String(s.movement_id) === String(movId)) {
              s.card_id = updated.card_id;
              s.person_id = updated.person_id;
              s.updated_at = now;
              await dbInstance.put('set_asides', s);
            }
          }
        } catch (e) {}
        return { success: true, changes: 1 };
      }
      return { status: 404, error: 'Movimiento no encontrado' };
    }

    if (path.match(/^\/api\/movements\/([^/]+)$/) && method === 'DELETE') {
      const movId = path.split('/')[3];
      await dbInstance.delete('movements', movId);

      const asides = await dbInstance.getAll('set_asides');
      for (const s of asides) {
        if (s.movement_id === movId) await dbInstance.delete('set_asides', s.id);
      }
      return { success: true };
    }

    // --- 5. APARTADOS (SET-ASIDES) ---
    if (path === '/api/set-asides') {
      if (method === 'GET') {
        const [asides, cards, people] = await Promise.all([
          dbInstance.getAll('set_asides'),
          dbInstance.getAll('cards'),
          dbInstance.getAll('people')
        ]);

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

        await dbInstance.put('set_asides', {
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

        if (body.movement_id) {
          const mov = await dbInstance.get('movements', body.movement_id);
          if (mov) {
            mov.is_set_aside = 1;
            mov.updated_at = now;
            await dbInstance.put('movements', mov);
          }
        }
        return { success: true, id };
      }
    }

    if (path.match(/^\/api\/set-asides\/([^/]+)$/) && method === 'PUT') {
      const asideId = path.split('/')[3];
      const existing = await dbInstance.get('set_asides', asideId);
      if (existing) {
        const numAmount = parseFloat(body.amount) || 0;
        const numPaid = parseFloat(body.paid_amount) || 0;
        const paidFlag = body.is_paid ? 1 : (numPaid >= numAmount && numAmount > 0 ? 1 : 0);
        const statusVal = body.status || (paidFlag ? 'pagado' : (numPaid > 0 ? 'pago_parcial' : 'apartado'));
        const fundType = (body.fund_type === 'Débito' || body.fund_type === 'Debito') ? 'Débito' : 'Efectivo';

        const updated = {
          ...existing,
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
        await dbInstance.put('set_asides', updated);
        return { success: true, id: asideId };
      }
      return { status: 404, error: 'Apartado no encontrado' };
    }

    if (path.match(/^\/api\/set-asides\/([^/]+)$/) && method === 'DELETE') {
      const asideId = path.split('/')[3];
      const target = await dbInstance.get('set_asides', asideId);
      await dbInstance.delete('set_asides', asideId);

      if (target && target.movement_id) {
        const allAsides = await dbInstance.getAll('set_asides');
        const hasLeft = allAsides.some(s => s.movement_id === target.movement_id);
        const mov = await dbInstance.get('movements', target.movement_id);
        if (mov) {
          mov.is_set_aside = hasLeft ? 1 : 0;
          mov.updated_at = Date.now();
          await dbInstance.put('movements', mov);
        }
      }
      return { success: true };
    }

    if (path.match(/^\/api\/set-asides\/([^/]+)\/pay$/)) {
      const asideId = path.split('/')[3];
      const aside = await dbInstance.get('set_asides', asideId);
      if (aside) {
        const targetAmount = (body.paid_amount !== undefined && body.paid_amount !== null && !isNaN(parseFloat(body.paid_amount)))
          ? Math.min(aside.amount, parseFloat(body.paid_amount))
          : aside.amount;
        const isFull = targetAmount >= (aside.amount - 0.001);

        if (isFull) {
          // Liquidación 100%: borrar movimiento y borrar apartado para liberar crédito por completo
          if (aside.movement_id) {
            await dbInstance.delete('movements', aside.movement_id);
          } else {
            const movs = await dbInstance.getAll('movements');
            const match = movs.find(m => String(m.card_id) === String(aside.card_id) && String(m.person_id) === String(aside.person_id) && Math.abs(m.amount - aside.amount) < 0.01);
            if (match) await dbInstance.delete('movements', match.id);
          }
          await dbInstance.delete('set_asides', asideId);
          return { success: true, fully_settled: true, amount_paid: targetAmount };
        } else {
          // Pago parcial
          const newAmount = Math.max(0, aside.amount - targetAmount);
          if (aside.movement_id) {
            const mov = await dbInstance.get('movements', aside.movement_id);
            if (mov) {
              mov.amount = Math.max(0, mov.amount - targetAmount);
              mov.updated_at = Date.now();
              await dbInstance.put('movements', mov);
            }
          }
          aside.amount = newAmount;
          aside.updated_at = Date.now();
          await dbInstance.put('set_asides', aside);
          return { success: true, fully_settled: false, remaining: newAmount, amount_paid: targetAmount };
        }
      }
      return { status: 404, error: 'Apartado no encontrado' };
    }

    if (path.match(/^\/api\/set-asides\/([^/]+)\/unpay$/)) {
      const asideId = path.split('/')[3];
      const aside = await dbInstance.get('set_asides', asideId);
      if (aside) {
        aside.paid_amount = 0;
        aside.is_paid = 0;
        aside.status = 'apartado';
        aside.updated_at = Date.now();
        await dbInstance.put('set_asides', aside);
        return { success: true };
      }
      return { status: 404, error: 'Apartado no encontrado' };
    }

    if (path.match(/^\/api\/set-asides\/([^/]+)\/toggle-fund$/)) {
      const asideId = path.split('/')[3];
      const aside = await dbInstance.get('set_asides', asideId);
      if (aside) {
        const cur = aside.fund_type === 'Débito' ? 'Débito' : 'Efectivo';
        const next = cur === 'Efectivo' ? 'Débito' : 'Efectivo';
        aside.fund_type = next;
        aside.updated_at = Date.now();
        await dbInstance.put('set_asides', aside);
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

      const people = await dbInstance.getAll('people');
      const personId = people.length > 0 ? people[0].id : 'personal';
      const noteText = note ? ` (${note})` : '';

      await dbInstance.put('set_asides', {
        id: getUUID(),
        card_id,
        person_id: personId,
        amount: -numAmount,
        fund_type: originType,
        note: `Pase a ${destType}${noteText}`,
        date: today,
        updated_at: now
      });

      await dbInstance.put('set_asides', {
        id: getUUID(),
        card_id,
        person_id: personId,
        amount: numAmount,
        fund_type: destType,
        note: `Recepción de ${originType}${noteText}`,
        date: today,
        updated_at: now + 1
      });

      return { success: true, transferred: numAmount, from: originType, to: destType };
    }

    // --- 6. MSI (MESES SIN INTERESES) ---
    if (path.startsWith('/api/msi/')) {
      const parts = path.split('/');
      const cardId = parts[3];

      if (parts[4] === 'advance') {
        const planId = cardId;
        const plan = await dbInstance.get('installment_plans', planId);
        if (plan) {
          plan.paid_months = (plan.paid_months || 0) + 1;
          plan.updated_at = Date.now();
          await dbInstance.put('installment_plans', plan);
          return { success: true };
        }
        return { status: 404, error: 'Plan no encontrado' };
      }

      if (method === 'DELETE') {
        const planId = cardId;
        await dbInstance.delete('installment_plans', planId);
        return { success: true };
      }

      if (method === 'GET') {
        const plans = await dbInstance.getAll('installment_plans');
        const cardPlans = plans.filter(p => p.card_id === cardId);
        const people = await dbInstance.getAll('people');
        const peopleMap = Object.fromEntries(people.map(p => [p.id, p.name]));
        const enriched = cardPlans.map(p => ({
          ...p,
          person_name: peopleMap[p.person_id] || 'Desconocido'
        }));
        return enriched.sort((a, b) => (b.start_date || '').localeCompare(a.start_date || ''));
      }
    }

    // --- 7. PRESUPUESTOS ---
    if (path === '/api/budgets') {
      if (method === 'GET') {
        const [budgets, items] = await Promise.all([
          dbInstance.getAll('budgets'),
          dbInstance.getAll('budget_items')
        ]);
        return { budgets, items };
      }
      if (method === 'POST') {
        const id = getUUID();
        await dbInstance.put('budgets', {
          id,
          name: body.name,
          description: body.description || '',
          updated_at: Date.now()
        });
        return { success: true, id };
      }
    }

    if (path.startsWith('/api/budgets/') && method === 'DELETE') {
      const bId = path.split('/')[3];
      await dbInstance.delete('budgets', bId);

      const items = await dbInstance.getAll('budget_items');
      for (const bi of items) {
        if (bi.budget_id === bId) await dbInstance.delete('budget_items', bi.id);
      }
      return { success: true };
    }

    if (path === '/api/budget-items') {
      if (method === 'POST') {
        const id = getUUID();
        await dbInstance.put('budget_items', {
          id,
          budget_id: body.budget_id,
          concept: body.concept,
          amount: parseFloat(body.amount) || 0,
          type: body.type || 'Fijo',
          tag: body.tag || 'General',
          updated_at: Date.now()
        });
        return { success: true, id };
      }
    }

    if (path.startsWith('/api/budget-items/') && method === 'DELETE') {
      const biId = path.split('/')[3];
      await dbInstance.delete('budget_items', biId);
      return { success: true };
    }

    // --- 8. CONFIGURACIÓN E INFLACIÓN ---
    if (path === '/api/config/inflation') {
      if (method === 'GET') {
        const cfg = await dbInstance.getConfig();
        return { inflationRate: parseFloat(cfg.inflation_rate) || 6.0 };
      }
      if (method === 'POST') {
        const rate = parseFloat(body.rate) || 6.0;
        await dbInstance.setConfig({ inflation_rate: rate });
        return { success: true, inflationRate: rate };
      }
    }

    // --- 9. SINCRONIZACIÓN Y EXPORTACIÓN JSON ---
    if (path === '/api/sync/export' || (path === '/api/database/download' && method === 'GET')) {
      const dump = await dbInstance.exportDump();
      return dump;
    }

    if (path === '/api/sync/import') {
      try {
        const res = await dbInstance.importDump(body, { overwrite: false });
        return { success: true, processed: res.processed };
      } catch (err) {
        return { status: 400, error: err.message };
      }
    }

    // --- 10. INFORMACIÓN DE ALMACENAMIENTO ---
    if (path === '/api/database/info') {
      return await dbInstance.getDatabaseInfo();
    }

    if (path === '/api/database/empty-status') {
      return { isEmpty: await dbInstance.isDatabaseEmpty() };
    }

    // --- 11. RESTAURACIÓN COMPLETA DESDE JSON ---
    if (path === '/api/database/restore' && method === 'POST') {
      try {
        let payload = body;
        if (body.jsonData) {
          payload = typeof body.jsonData === 'string' ? JSON.parse(body.jsonData) : body.jsonData;
        } else if (body.dbBase64) {
          // Manejar si alguien envía base64 de texto JSON
          try {
            const clean = body.dbBase64.replace(/^data:.*?;base64,/, '');
            const decoded = atob(clean);
            payload = JSON.parse(decoded);
          } catch (e) {
            return { status: 400, error: 'El archivo recibido no es un JSON válido. SQLite binario ya no es compatible.' };
          }
        }

        const res = await dbInstance.importDump(payload, { overwrite: true });
        return { success: true, message: 'Base de datos restaurada exitosamente', ...res };
      } catch (err) {
        console.error('[CardMasterDB] Error restaurando:', err);
        return { status: 400, error: err.message };
      }
    }

    return { status: 404, error: 'Ruta no encontrada en motor local' };
  }

  // =========================================================================
  // 7. INTERCEPTOR MAESTRO DE FETCH (LOCAL-FIRST OFFLINE)
  // =========================================================================

  const _realFetch = window.fetch;

  window.fetch = async function(input, init) {
    let url = typeof input === 'string' ? input : (input && input.url ? input.url : '');
    const method = (init && init.method ? init.method.toUpperCase() : 'GET');

    let pathname = url;
    try {
      if (url.startsWith('http://') || url.startsWith('https://')) {
        pathname = new URL(url).pathname;
      }
    } catch (e) {}

    // Comprobar si hay servidor remoto configurado explícitamente por el usuario
    const customRemoteUrl = localStorage.getItem('cardmaster_server_url') || '';
    const hasCustomRemote = Boolean(customRemoteUrl && !customRemoteUrl.includes('localhost') && !customRemoteUrl.includes('127.0.0.1'));

    if (hasCustomRemote && pathname.startsWith('/api/')) {
      const fullUrl = customRemoteUrl.replace(/\/+$/, '') + pathname;
      try {
        return await _realFetch.call(this, fullUrl, init);
      } catch (networkErr) {
        console.warn('[Fetch] Servidor remoto no accesible, usando motor Local-First...', networkErr);
      }
    }

    if (pathname.startsWith('/api/')) {
      // Si estamos en localhost:3000 con el backend de desarrollo activo, intentar primero
      if (typeof window !== 'undefined' && window.location && window.location.port === '3000' && typeof _realFetch === 'function') {
        try {
          return await _realFetch.call(this, input, init);
        } catch (e) {
          // Continuar al motor local
        }
      }

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
        const status = (result && result.status) ? result.status : 200;
        const responseBlob = new Blob([JSON.stringify(result)], { type: 'application/json' });

        return new Response(responseBlob, {
          status: status,
          statusText: status === 200 ? 'OK' : 'Error',
          headers: {
            'Content-Type': 'application/json',
            'Content-Disposition': (url === '/api/database/download' || url === '/api/sync/export')
              ? 'attachment; filename="cardmaster_backup.json"'
              : undefined
          }
        });
      } catch (err) {
        console.error('[LocalEngine] Error ejecutando petición local:', err);
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { 'Content-Type': 'application/json' }
        });
      }
    }

    return _realFetch.call(this, input, init);
  };

  // Exponer objeto global para depuración
  window.CardMasterEngine = {
    isLocal: true,
    DB: dbInstance,
    exportDatabaseFile,
    importDatabaseFile
  };

  console.log('✨ [CardMaster] Motor Local-First (IndexedDB Engine) inicializado con éxito.');
})();
