#!/usr/bin/env bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "Restableciendo contraseña de CardMaster..."
node -e "
const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');

const DATA_DIR = process.env.DATA_DIR ? path.resolve(process.env.DATA_DIR) : __dirname;
const DB_PATH = process.env.DB_PATH ? path.resolve(process.env.DB_PATH) : path.join(DATA_DIR, 'tarjetas.db');

if (!fs.existsSync(DB_PATH)) {
  console.log('No se encontró el archivo tarjetas.db en:', DB_PATH);
  process.exit(1);
}

const db = new sqlite3.Database(DB_PATH);
db.run('DELETE FROM app_config WHERE key IN (\'master_pwd_hash\', \'master_pwd_salt\')', function(err) {
  if (err) {
    console.error('Error al restablecer contraseña:', err.message);
  } else {
    console.log('✅ Contraseña restablecida correctamente.');
    console.log('👉 Ahora recarga la página en tu navegador para crear una nueva contraseña desde cero.');
  }
  db.close();
});
"

