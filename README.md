# 💳 CardMaster

> **Sistema Integral de Gestión de Tarjetas de Crédito, Meses Sin Intereses (MSI), Fondos Apartados y Presupuestos**  
> *Arquitectura Multiplataforma Modular y 100% Autónoma — Sin Dependencia de la Nube (Local-First)*

---

[![Architecture: Local-First](https://img.shields.io/badge/Architecture-Local--First-0ea5e9?style=for-the-badge&logo=sqlite&logoColor=white)](https://sqlite.org)
[![Security: PBKDF2 SHA-256](https://img.shields.io/badge/Security-PBKDF2--SHA256%20100k-10b981?style=for-the-badge&logo=shield&logoColor=white)](#-seguridad-y-contraseña-maestra)
[![Platform: Linux Ubuntu](https://img.shields.io/badge/PC-Linux%20Ubuntu-e11d48?style=for-the-badge&logo=ubuntu&logoColor=white)](#-1-desktop-ubuntu--para-pc-linux--ubuntu)
[![Platform: Android](https://img.shields.io/badge/Mobile-Android%20(APK)-3ddc84?style=for-the-badge&logo=android&logoColor=white)](#-2-mobile-android--para-celulares-android)
[![Platform: Web Universal](https://img.shields.io/badge/Universal-Windows%20%7C%20Mac%20%7C%20iOS-38bdf8?style=for-the-badge&logo=googlechrome&logoColor=white)](#-3-web-app--para-windows-macos-iphoneipad-y-otros)

---

## 📂 Organización del Proyecto

El repositorio está organizado en **3 plataformas completamente independientes y autosuficientes**. Cada carpeta contiene su propio código, base de datos SQLite y mecanismos de arranque, de modo que si mueves o eliminas una carpeta, las demás continúan funcionando a la perfección:

```text
CreditManager/
│
├── 🖥️ desktop-ubuntu/     ➔ Para computadoras Linux / Ubuntu (Ventana Nativa de Escritorio)
│   ├── desktop-app.py     # Aplicación nativa GTK3 sin barras de navegador
│   ├── iniciar-ubuntu.sh  # Script de inicio en 1 clic
│   ├── instalar-en-ubuntu.sh # Crea el acceso directo en el Escritorio y menú de apps
│   ├── tarjetas.db        # Base de datos física SQLite de la PC
│   └── README.md          # Manual paso a paso para Ubuntu
│
├── 📱 mobile-android/      ➔ Para celulares Android (Instalador APK listo para usar)
│   ├── apk/CardMaster.apk # Instalador .APK directo (sin terminales ni servidores)
│   ├── android/           # Proyecto nativo Capacitor / Gradle
│   ├── public/            # Motor SQLite autónomo integrado (sql-asm.js)
│   └── README.md          # Tutorial detallado para Android
│
└── 🌐 web-app/             ➔ Para Windows, macOS, iOS (iPhone/iPad) y navegadores web
    ├── index.html         # ¡DOBLE CLIC para usar al instante sin instalar nada!
    ├── app.js             # Lógica completa de tarjetas, MSI y apartados
    ├── local-engine.js    # Motor autónomo con datos precargados
    ├── iniciar-windows.bat # Doble clic para iniciar en Windows con servidor local
    ├── iniciar-mac.command # Doble clic para iniciar en macOS
    ├── tarjetas.db        # Base de datos física SQLite
    └── README.md          # Guía visual para no programadores
```

---

## 🚀 ¿Cómo usar CardMaster según tu dispositivo?

---

### 🖥️ 1. `desktop-ubuntu/` — Para PC Linux / Ubuntu

Diseñado para funcionar como una **aplicación nativa de escritorio** con ventana independiente:

1. **Abrir la app:**
   ```bash
   cd desktop-ubuntu
   ./iniciar-ubuntu.sh
   ```
2. **Crear acceso directo en tu Escritorio:**
   ```bash
   cd desktop-ubuntu
   ./instalar-en-ubuntu.sh
   ```
   *Podrás abrirla haciendo doble clic en el icono de CardMaster en tu Escritorio o buscándola en tus aplicaciones de Ubuntu.*

---

### 📱 2. `mobile-android/` — Para Celulares Android

Diseñado para usar directamente en tu teléfono móvil con **1 toque y sin terminales ni servidores encendidos**:

1. **Descarga el instalador directo en tu celular:**  
   👉 [Descargar CardMaster.apk](https://github.com/GerardoLJ/CreditManager/raw/main/mobile-android/apk/CardMaster.apk)
2. Tócalo para instalarlo en Android.
3. ¡Listo! Abre la app **CardMaster** desde tu menú de aplicaciones. Funciona 100% desconectado y offline.

---

### 🌐 3. `web-app/` — Para Windows, macOS, iPhone/iPad y otros

Diseñado para que cualquier persona que **no sea programador** pueda utilizar CardMaster sin depender de una PC encendida:

* **💻 En Cualquier Computadora (Windows, Mac o Linux):**  
  Solo entra a la carpeta `web-app` y haz **doble clic en `index.html`**. ¡Se abre directamente en tu navegador con todas tus tarjetas listas para usar sin tocar ninguna terminal ni instalar nada!
* **📱 En iPhone / iPad (iOS):**  
  ¡Funciona de forma **100% independiente sin tener tu PC encendida**! Solo abre tu navegador Safari con el enlace de GitHub Pages (`https://gerardolj.github.io/CreditManager/`), pulsa **Compartir (⬆️)** ➔ **"Añadir a pantalla de inicio"** y tendrás la App instalada en tu iPhone.
* **🎛️ Servidor Local Opcional:** Si deseas ejecutarlo con servidor en red local, haz doble clic en `iniciar-windows.bat` (Windows) o `iniciar-mac.command` (Mac).

---

## 💾 Sincronización entre Dispositivos (`tarjetas.db`)

Para pasar tus tarjetas y movimientos de un dispositivo a otro:

1. Ve a la sección **Ajustes** en tu dispositivo de origen.
2. Descarga tu archivo **`tarjetas.db`**.
3. En tu otro dispositivo (celular o PC), entra a **Ajustes** (o en la pantalla de bienvenida tocando **`📂 Cargar / Reemplazar tarjetas.db`**), selecciona el archivo y se sobreescribirá y actualizará todo automáticamente en 1 segundo.

---

## 🔒 Seguridad y Contraseña Maestra

* **Cifrado Robusto:** Derivación criptográfica mediante **PBKDF2** con 100,000 iteraciones SHA-256.
* **Compatibilidad Total:** El algoritmo calcula exactamente la misma clave en PC (Node.js) y en Celular/Web (Web Crypto API).
* **Protección de Acciones:** Eliminar tarjetas, realizar cortes mensuales o resetear datos requiere confirmar tu contraseña maestra.
* **Privacidad Absoluta:** Todos tus datos residen únicamente en tus dispositivos, sin servidores en la nube ni telemetría externa.

---

<div align="center">
  <sub>CardMaster © 2026 — Control Financiero Multiplataforma Local-First</sub>
</div>
