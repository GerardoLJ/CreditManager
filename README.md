# Credit Manager (Control de Tarjetas con SQLite en Disco / USB)

Esta aplicación utiliza **Node.js** y **SQLite (`better-sqlite3`)** para que todos tus datos se guarden en un archivo físico real llamado **`tarjetas.db`** directamente en esta carpeta.

## Requisitos
- [Node.js](https://nodejs.org/) instalado en tu equipo.

## Pasos para iniciar en tu computadora o laptop:

1. Descomprime los archivos en tu carpeta o memoria USB.
2. Abre tu terminal en esa carpeta.
3. Instala las dependencias (solo la primera vez):
   ```bash
   npm install
   ```
4. Inicia el servidor:
   ```bash
   node server.js
   ```
   *(o `npm start`)*
5. Abre en tu navegador favorito:
   ```
   http://localhost:3000
   ```

## Características incluidas:
- **Archivo físico real `tarjetas.db`:** Se crea automáticamente en la misma carpeta. Puedes inspeccionarlo en Beekeeper Studio, DBeaver o la consola de SQLite.
- **Módulo de Apartados / Dinero en mano:**
  - Registra cuándo una persona te entrega dinero antes de pagar la tarjeta.
  - Reduce la deuda pendiente de cobrar a la persona.
  - Acumula un fondo de apartados por tarjeta (*"Dinero listo para pagar"*).
- **Control de Tarjetas y MSI:** Amortización mensual inteligente que restaura crédito disponible sin borrar los planes al resetear movimientos ordinarios.
- **Ventana de pago personalizada:** Corte + 1 hasta Corte + 15 (15 días de ventana).
- **Drag & Drop** para logotipos de bancos e importación.
- **Presupuestos y proyecciones interanuales** con tasa global de incremento.
