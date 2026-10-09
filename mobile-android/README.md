# 📱 CardMaster — Edición Móvil para Android

Esta carpeta contiene la **aplicación autónoma para dispositivos Android**, diseñada para operar de forma independiente a la versión de escritorio.

> **Independencia Total:** Esta carpeta es 100% autónoma y tiene su propia base de datos física (`tarjetas.db`). Puedes moverla a otro equipo o eliminar la carpeta de Linux Ubuntu sin afectar en absoluto esta aplicación para Android.

---

## 🌟 Tres Formas de Usar CardMaster en Android

---

### 🚀 Método 1: Progressive Web App (PWA) — Instalación Directa (Recomendado)
Es la forma más rápida y sin necesidad de compilar código:

1. **Inicia el servicio móvil:**
   ```bash
   ./iniciar-android.sh
   ```
2. La terminal te mostrará tu dirección de red local (ejemplo: `http://192.168.1.45:3000`).
3. Conecta tu celular a la misma red Wi-Fi y abre esa dirección en **Google Chrome**, **Firefox** o **Brave**.
4. En el menú de opciones del navegador (los tres puntos verticales):
   - Selecciona **"Instalar aplicación"** o **"Añadir a la pantalla de inicio"**.
5. ¡Listo! Se creará el icono de **CardMaster** en tu cajón de aplicaciones de Android, abriendo a pantalla completa como una app nativa sin barra de direcciones.

*Para detener el servicio:*
```bash
./detener-android.sh
```

---

### 📲 Método 2: Ejecución 100% Autónoma en el Celular (Con Termux - Sin Computadora)
Si deseas que tu celular sea **100% independiente de tu computadora** y funcione incluso cuando estés en la calle sin Wi-Fi:

1. Instala **Termux** en tu teléfono Android (desde F-Droid o GitHub).
2. Dentro de Termux en tu teléfono, instala Node.js:
   ```bash
   pkg update
   pkg install nodejs
   ```
3. Copia esta carpeta `mobile-android` a tu teléfono (o clónala en Termux).
4. Inicia el servidor dentro de Termux:
   ```bash
   cd mobile-android
   npm start
   ```
5. En el navegador de tu celular abre `http://localhost:3000` e instálala en tu pantalla de inicio. Toda la base de datos vivirá 100% en la memoria de tu teléfono.

---

### 📦 Método 3: Compilar un APK Instalable (Con Capacitor y Android Studio)
Esta carpeta ya incluye la configuración de [`capacitor.config.json`](file:///home/personal/Web-Personal/CreditManager/mobile-android/capacitor.config.json) para generar un archivo `.apk` binario tradicional:

1. Instala Capacitor en esta carpeta:
   ```bash
   npm install @capacitor/core @capacitor/cli @capacitor/android
   ```
2. Inicializa el proyecto nativo de Android:
   ```bash
   npx cap add android
   ```
3. Sincroniza los archivos de la app:
   ```bash
   npx cap sync
   ```
4. Abre el proyecto en Android Studio:
   ```bash
   npx cap open android
   ```
5. En Android Studio, ve a **Build > Build Bundle(s) / APK(s) > Build APK(s)** para generar tu archivo `.apk` final instalable.

---

## 📁 Estructura Interna de esta Carpeta

```text
mobile-android/
├── 📄 iniciar-android.sh      # Inicia el servidor y muestra la IP Wi-Fi para tu teléfono
├── 📄 detener-android.sh      # Detiene el servidor móvil
├── 📄 resetear-contrasena.sh  # Utilidad para resetear contraseña maestra de este móvil
├── 📄 server.js               # Backend Express + SQLite optimizado para Android
├── 📄 tarjetas.db             # Base de datos física independiente para el celular
├── 📄 capacitor.config.json   # Configuración de empaquetado para APK nativo
├── 📁 public/                 # Archivos frontend móviles:
│   ├── 📄 index.html          # Vista responsiva con menú cajón (☰)
│   ├── 📄 styles.css          # Estilos optimizados para pantallas táctiles
│   ├── 📄 app.js              # Lógica de cliente y almacenamiento
│   ├── 📄 manifest.json       # Manifiesto PWA para Android
│   ├── 📄 service-worker.js   # Soporte offline
│   ├── 🖼️ icon-192.png        # Icono resolución estándar
│   └── 🖼️ icon-512.png        # Icono alta resolución
├── 📁 node_modules/           # Módulos y dependencias locales
├── 📄 package.json            # Configuración npm
└── 📄 README.md               # Este manual
```

---

## 🔒 Seguridad y Privacidad
* **Base de Datos Exclusiva:** Los movimientos registrados aquí se guardan en el archivo `tarjetas.db` de esta carpeta, completamente separados de los de tu computadora.
* **Bóveda PBKDF2:** Tu contraseña maestra de celular se procesa con 100,000 iteraciones SHA-256 y salt criptográfico único.
* **Recuperación:** Si olvidas la contraseña de tu celular, ejecuta `./resetear-contrasena.sh` dentro de esta carpeta para definir una nueva sin perder tus datos.

---

## 🔄 Pasar Información a tu Computadora
Si deseas respaldar tus datos del celular en tu computadora:
1. En la app del celular ve a **Ajustes > Sincronización**.
2. Toca **"Exportar Archivo de Sincronización (JSON)"**.
3. Envía ese archivo a tu PC e impórtalo en la versión de escritorio.

