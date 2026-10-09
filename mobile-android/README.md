# 📱 Guía para Usar CardMaster en tu Celular (Paso a Paso para No Programadores)

Esta guía está escrita para que **cualquier persona, sin saber nada de programación**, pueda abrir y usar CardMaster en su teléfono Android fácilmente.

---

## ❓ ¿Qué tengo que descargar en mi celular?

Depende de cómo quieras usarlo. Elige la opción que más te guste:

### 🌟 Opción 1: La más fácil del mundo (NO necesitas descargar nada nuevo)
* **¿Qué necesitas descargar?** ¡Nada! Usas el navegador que ya viene en tu teléfono (**Google Chrome** o **Firefox**).
* **¿Cómo funciona?** Tu computadora actúa como el "cerebro" y tu celular se conecta a ella por la red Wi-Fi de tu casa. Una vez conectado, el celular guarda un icono en tu pantalla como si fuera una app descargada de la tienda.

---

### 📲 Opción 2: Para usar el celular fuera de casa (Sin que tu computadora esté prendida)
* **¿Qué necesitas descargar?** Una aplicación gratuita llamada **Termux** (disponible gratis en la tienda F-Droid o en internet).
* **¿Para qué sirve?** Hace que tu celular tenga su propio "motor" independiente, así puedes registrar gastos en la calle, en el centro comercial o de viaje sin que tu PC esté encendida.

---

## 🚀 Paso a Paso: Opción 1 (Usar en tu celular en 2 minutos)

Sigue estos 4 pasos sencillos:

### Paso 1: En tu computadora
1. Abre tu terminal en la PC y escribe:
   ```bash
   cd /home/personal/Web-Personal/CreditManager/mobile-android
   ./iniciar-android.sh
   ```
2. La pantalla te mostrará una dirección de internet parecida a esta:
   `👉 http://192.168.1.45:3000` *(el número exacto depende de tu casa)*.

---

### Paso 2: En tu celular
1. Asegúrate de que tu celular esté conectado al **mismo Wi-Fi** de tu casa.
2. Abre la app de **Google Chrome** en tu teléfono.
3. En la barra donde escribes las páginas web (arriba), escribe la dirección que te dio la computadora (ejemplo: `http://192.168.1.45:3000`) y presiona entrar.

---

### Paso 3: Guardar el icono en tu celular como una App real
Para no tener que escribir esa dirección nunca más:
1. En Google Chrome en tu celular, toca los **tres puntitos verticales** (arriba a la derecha).
2. Toca la opción que dice **"Instalar aplicación"** o **"Añadir a la pantalla principal"**.
3. Confirma tocando **"Instalar"**.
4. ¡Listo! En la pantalla de tu celular aparecerá el icono de **CardMaster**. Cuando lo toques, se abrirá a pantalla completa, sin barras de internet, exactamente como WhatsApp o cualquier app de tu banco.

---

### Paso 4: Detener el servicio en la PC cuando no lo uses
Cuando termines, en tu computadora simplemente escribe:
```bash
cd /home/personal/Web-Personal/CreditManager/mobile-android
./detener-android.sh
```

---

## 📲 Paso a Paso: Opción 2 (Celular 100% Independiente con Termux)

Si quieres llevar tus finanzas en tu celular a todas partes sin depender de tu PC:

1. **Descarga Termux en tu celular:**
   - Entra desde el navegador de tu celular a [https://f-droid.org/packages/com.termux/](https://f-droid.org/packages/com.termux/) y descarga el archivo APK de Termux.
2. **Abre Termux y escribe estos 2 comandos:**
   ```bash
   pkg update -y
   pkg install nodejs git -y
   ```
3. **Pasa la carpeta `mobile-android` a tu celular o descárgala de tu GitHub:**
   ```bash
   git clone https://github.com/GerardoLJ/CreditManager.git
   cd CreditManager/mobile-android
   npm install
   npm start
   ```
4. **Abrir la app en tu celular:**
   Abre Chrome en tu celular y entra a: `http://localhost:3000`.  
   Toca los tres puntos de Chrome y dale a **"Instalar aplicación"**. Toda tu información se guardará en la memoria interna de tu celular.

---

## 💳 Cómo empezar a usar la aplicación en tu celular

1. **Crear tu Contraseña por primera vez:**
   La primera vez que abras CardMaster, te pedirá inventar una **Contraseña Maestra**. Escribe una que recuerdes bien y confírmala.
2. **Agregar tus Tarjetas de Crédito:**
   Toca el botón azul **"+ Nueva Tarjeta"**. Ponle nombre (ejemplo: *BBVA Azul* o *Nu*), tu límite de crédito (ejemplo: *15,000*) y el día del mes en que corta tu tarjeta.
3. **Registrar tus Gastos:**
   Toca la pestaña **Movimientos** y pulsa **"+ Nuevo Gasto"**. Escribe qué compraste, el monto y si fue a Meses Sin Intereses.
4. **Si te equivocas en un gasto:**
   En la lista de movimientos verás un botón con un lápiz **✏️**. Tócalo para corregir el monto o el nombre sin tener que borrar nada.
5. **Separar dinero (Fondos Apartados):**
   Si alguien te pagó una deuda o tú guardaste dinero para pagar el banco, toca **"+ Apartar Dinero"**. El dinero se marcará como "listo para pagar" y no te lo gastarás por error.

---

## 🆘 Preguntas Frecuentes y Ayuda

### ¿Dónde se guarda la información de mi celular?
Se guarda en un archivo llamado **`tarjetas.db`** dentro de la carpeta `mobile-android`. Esa información es solo de tu celular y no se mezcla con tu computadora.

### ¿Qué pasa si se me olvida la contraseña maestra en el celular?
No te preocupes, no perderás tus tarjetas ni tus gastos. Solo ejecuta este script:
```bash
./resetear-contrasena.sh
```
Eso borrará únicamente la contraseña olvidada. Cuando vuelvas a entrar, la app te pedirá inventar una nueva contraseña y todos tus datos seguirán ahí intactos.

### ¿Cómo paso mis datos del celular a la computadora si algún día quiero?
1. En tu celular entra a la pestaña **Ajustes**.
2. Toca **"Exportar Archivo de Sincronización (JSON)"** y guárdalo.
3. Mándate ese archivo por correo o WhatsApp Web a tu computadora.
4. En tu computadora ve a Ajustes y arrastra ese archivo para combinar tus gastos sin borrar nada.
