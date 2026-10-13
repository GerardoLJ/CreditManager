# 💳 CardMaster

> **Sistema Integral de Gestión de Tarjetas de Crédito, Meses Sin Intereses (MSI), Fondos Apartados y Presupuestos**  
> *Arquitectura Nativa Especializada y Multiplataforma — 100% Offline (Local-First)*

---

[![Android Native](https://img.shields.io/badge/Android-100%25%20Nativo%20(SQLite)-3ddc84?style=for-the-badge&logo=android&logoColor=white)](#-1-mobile-android--para-celulares-android-100-nativo)
[![Universal PC](https://img.shields.io/badge/PC%20Web--App-Windows%20%7C%20macOS%20%7C%20Linux-0ea5e9?style=for-the-badge&logo=googlechrome&logoColor=white)](#-2-web-app--para-cualquier-pc-windows-macos-linux)
[![Security PBKDF2](https://img.shields.io/badge/Security-PBKDF2--SHA256%20100k-10b981?style=for-the-badge&logo=shield&logoColor=white)](#-seguridad-y-bóveda-local)
[![Backup Interoperable](https://img.shields.io/badge/Backup-JSON%20Universal%20Interoperable-f59e0b?style=for-the-badge&logo=json&logoColor=white)](#-respaldos-e-interoperabilidad-entre-pc-y-android)

---

## 📂 Organización del Proyecto

El sistema está dividido en dos aplicaciones optimizadas y especializadas, cada una con su propia tecnología nativa para un rendimiento perfecto:

```text
CreditManager/
│
├── 📱 mobile-android/      ➔ Aplicación 100% Nativa para Android
│   ├── apk/CardMaster.apk # Instalador APK directo compilado y firmado
│   └── android/           # Código fuente Java + Android SQLite + Material Design
│
└── 💻 web-app/             ➔ Aplicación Universal para Cualquier PC
    ├── server.js          # Servidor Node.js + SQLite multiplataforma
    ├── index.html         # Interfaz web responsiva con gráficos y tarjetas
    ├── app.js             # Lógica completa de finanzas
    ├── iniciar-windows.bat # Inicio en 1 clic para Windows
    ├── iniciar-mac.command # Inicio en 1 clic para macOS
    ├── iniciar-linux.sh   # Inicio en 1 clic para Linux
    └── detener-*.sh/.bat  # Scripts para apagar el servidor
```

---

## 🚀 ¿Cómo usar CardMaster según tu dispositivo?

---

### 📱 1. `mobile-android/` — Para Celulares Android (100% Nativo)

Esta versión es una **aplicación nativa de Android** (no es una app web ni un envoltorio con WebViews):
- **Cero WebViews y Cero Servidores:** Sin dependencias de Node.js, puertos localhost o indicadores de conexión.
- **Base de Datos Interna Fija:** Los datos se gestionan mediante el motor interno de SQLite en `/data/data/com.cardmaster.finance/databases/cardmaster.db`.
- **Exportación Flexible sin Mover la Base de Datos:** Puedes exportar tu respaldo a **cualquier carpeta de tu teléfono** (Descargas, Google Drive, Tarjeta SD) utilizando el selector de documentos oficial de Android (SAF), garantizando que tu base de datos interna **nunca cambie de lugar**.
- **Instalación Limpia:** Al abrir la app recién instalada, inicia sin contraseñas preconfiguradas, permitiéndote crear tu contraseña o importar un respaldo en 1 paso.

1. Pasa el archivo `mobile-android/apk/CardMaster.apk` a tu teléfono (por cable, Drive o Telegram).
2. Tócalo para instalarlo (habilita *"Permitir desde esta fuente"* si Android lo solicita).
3. Abre **CardMaster** desde tu pantalla de inicio.

---

### 💻 2. `web-app/` — Para Cualquier PC (Windows, macOS, Linux)

Diseñado para funcionar en cualquier computadora de escritorio o laptop:

* **En Windows:** Haz doble clic en `iniciar-windows.bat`.
* **En macOS:** Haz doble clic en `iniciar-mac.command`.
* **En Linux:** Ejecuta `./iniciar-linux.sh`.
* **Sin Node.js:** Puedes abrir directamente `index.html` en tu navegador web favorito (Chrome, Edge, Safari, Firefox).

---

## 🔄 Respaldos e Interoperabilidad entre PC y Android

Ambas aplicaciones comparten el mismo estándar universal de respaldo JSON (`cardmaster-json-backup`), permitiendo migrar tus finanzas de la PC al celular y viceversa sin fricción:

1. **Exportar desde Android:** Entra a **Ajustes** ➔ **`💾 Exportar Respaldo a Cualquier Carpeta (.json)`** y guárdalo donde prefieras.
2. **Importar en PC:** Abre la Web-App en tu computadora, ve a **Ajustes** ➔ **`📥 Importar Respaldo (.json)`** y selecciona el archivo exportado de tu teléfono.
3. **De PC a Android:** El procedimiento funciona en sentido inverso exactamente igual, asegurando que tus tarjetas, gastos, MSI, apartados y presupuestos se sincronicen de manera inmediata.

---

## 🔒 Seguridad y Bóveda Local

Tanto en Android como en PC, tus datos están protegidos por derivación criptográfica de claves:
- Algoritmo: **PBKDF2 con HmacSHA256**
- Iteraciones: **100,000 rondas**
- Salting: Sal criptográfica aleatoria de **16 bytes** por instalación.
- Privacidad: Cero envío de datos a servidores externos o nube. Todos tus datos permanecen exclusivamente en tu dispositivo.
