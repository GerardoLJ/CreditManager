# 💳 CardMaster / CreditManager

> **Sistema Local de Gestión de Crédito, Amortización MSI, Fondos Apartados y Presupuestos**  
> *Arquitectura Multiplataforma Modular y Escalable — Proyectos 100% Autónomos e Independientes*

---

[![Architecture: Local-First](https://img.shields.io/badge/Architecture-Local--First-0ea5e9?style=for-the-badge&logo=sqlite&logoColor=white)](https://sqlite.org)
[![Security: PBKDF2 SHA-256](https://img.shields.io/badge/Security-PBKDF2--SHA256%20100k-10b981?style=for-the-badge&logo=shield&logoColor=white)](#-modelo-de-seguridad-y-criptograf%C3%ADa)
[![Platform: Linux Ubuntu](https://img.shields.io/badge/Desktop-Linux%20Ubuntu%20(GTK3)-e11d48?style=for-the-badge&logo=ubuntu&logoColor=white)](#-1-carpeta-desktop-ubuntu-escritorio-nativo)
[![Platform: Android Mobile](https://img.shields.io/badge/Mobile-Android%20(PWA%20%2F%20APK)-3ddc84?style=for-the-badge&logo=android&logoColor=white)](#-2-carpeta-mobile-android-dispositivos-m%C3%B3viles)
[![Privacy: Zero Cloud Telemetry](https://img.shields.io/badge/Privacy-100%25%20Zero--Cloud-8b5cf6?style=for-the-badge&logo=databricks&logoColor=white)](#-soberan%C3%ADa-de-datos)

---

## 🏛️ Arquitectura Modular y Escalable (Dos Mundos Independientes)

El proyecto está diseñado bajo un modelo de **alta cohesión y desacoplamiento total**. El código se organiza en **dos proyectos completamente autónomos e independientes**, permitiendo escalar cada plataforma por separado:

```text
CreditManager/
│
├── 🖥️ desktop-ubuntu/          # [PROYECTO 1] Aplicación Nativa para Linux Ubuntu
│   ├── 📄 desktop-app.py       # Ventana nativa GTK3 + WebKitGTK 4.1 sin navegador
│   ├── 📄 iniciar-ubuntu.sh    # Arranque en 1 clic
│   ├── 📄 detener-ubuntu.sh    # Apagado limpio de procesos
│   ├── 📄 CardMaster.desktop   # Acceso directo para escritorio y menú GNOME
│   ├── 📄 instalar-en-ubuntu.sh# Instalador en sistema operativo
│   ├── 📄 resetear-contrasena.sh# Herramienta de recuperación de clave
│   ├── 📄 server.js            # Servidor local y motor de datos
│   ├── 📄 tarjetas.db          # Base de datos física SQLite exclusiva de la PC
│   ├── 📁 public/              # Interfaz con Sidebar fija y Drag & Drop
│   └── 📄 README.md            # Manual exclusivo de la versión Ubuntu
│
├── 📱 mobile-android/           # [PROYECTO 2] Aplicación Autónoma para Android
│   ├── 📄 iniciar-android.sh   # Servidor móvil con detección de IP Wi-Fi
│   ├── 📄 detener-android.sh   # Detención de servidor móvil
│   ├── 📄 resetear-contrasena.sh# Herramienta de recuperación de clave móvil
│   ├── 📄 capacitor.config.json# Configuración para compilar a APK nativo
│   ├── 📄 server.js            # Servidor ligero móvil
│   ├── 📄 tarjetas.db          # Base de datos física SQLite exclusiva del celular
│   ├── 📁 public/              # PWA con menú tipo cajón (☰) y soporte táctil
│   └── 📄 README.md            # Manual exclusivo de la versión Android
│
├── 📄 iniciar.sh               # Lanzador interactivo general (menú de selección)
├── 📄 detener.sh               # Detención global de todos los servicios
├── 📄 Manual_Completo_CardMaster.pdf # Manual integral en PDF
└── 📄 README.md                # Este manual maestro
```

> [!IMPORTANT]
> **Garantía de Independencia Total:**  
> Cada carpeta contiene **todo lo necesario para funcionar por su cuenta** (código, dependencias, base de datos SQLite y scripts de arranque).  
> * Si decides **eliminar la carpeta `mobile-android/`**, la aplicación de Linux Ubuntu seguirá funcionando al 100%.  
> * Si decides **eliminar la carpeta `desktop-ubuntu/`** o llevar `mobile-android/` a otro equipo o celular, la aplicación de Android seguirá funcionando al 100%.  
> * Cada una posee su propio archivo físico `tarjetas.db`, garantizando que la información del teléfono y la de la computadora sean **dos mundos separados y no dependan el uno del otro**.

---

## 🌟 Novedades y Mejoras del Sistema

1. **Distribución Inteligente de Tarjetas:** El grid de tarjetas de crédito se adapta dinámicamente al tamaño de pantalla, eliminando espacios vacíos desproporcionados sin encimar los elementos.
2. **Reordenamiento con el Mouse (Drag & Drop):** Puedes arrastrar cualquier tarjeta con el mouse para colocarla en la posición que desees. El nuevo orden se guarda de forma permanente.
3. **Edición Completa de Movimientos:** Botón **✏️ Editar** en cada fila de gastos para corregir montos, conceptos, fechas, tarjetas o personas sin tener que borrar el registro.
4. **Diseño Unificado de Botones:** Se eliminaron los botones blancos o sin estilo; todos los botones cuentan con una estética cuidada, bordes suaves y estados hover acordes a la paleta corporativa.
5. **Listas Desplegables Estilizadas:** Todas las listas `<select>` ahora lucen modernas, con flechas SVG personalizadas y menús que respetan el modo Oscuro/Claro.
6. **Scroll Fluido en Todas las Páginas:** Contenedores con desplazamiento suave (`overflow-y: auto`) y barras de scroll discretas para que ninguna información quede oculta ni cortada.
7. **Formato Automático de Moneda:** Todos los campos de dinero separan los miles con comas (`,`) y los decimales con punto (`.`), por ejemplo `$15,000.00`.

---

## 🚀 Cómo Usar Cada Aplicación

---

### 🖥️ 1. Carpeta `desktop-ubuntu/` (Escritorio Nativo)
Diseñada para dar una experiencia de **software de escritorio 100% nativo** en Ubuntu, sin barras de URL, pestañas ni distracciones:

```bash
# Entrar a la carpeta y ejecutar
cd desktop-ubuntu
./iniciar-ubuntu.sh
```

**Para instalar el acceso directo en tu Escritorio de Ubuntu:**
```bash
cd desktop-ubuntu
./instalar-en-ubuntu.sh
```
*Aparecerá el icono oficial en tu Escritorio y en tus aplicaciones de Ubuntu para abrirlo con doble clic.*

---

### 📱 2. Carpeta `mobile-android/` (Dispositivos Móviles)
Diseñada para darte libertad total en tu teléfono móvil con **tres modalidades**:

```bash
cd mobile-android
./iniciar-android.sh
```

1. **Modalidad PWA (Recomendada y Rápida):**
   - Conecta tu celular a la misma red Wi-Fi.
   - Abre la URL mostrada en la terminal (ejemplo: `http://192.168.1.45:3000`) en Chrome o Firefox.
   - Pulsa los tres puntos del navegador y selecciona **"Instalar aplicación"** o **"Añadir a pantalla de inicio"**.
2. **Modalidad 100% en el Teléfono (Termux):**
   - Ejecuta CardMaster directamente dentro de tu celular Android usando la app gratuita **Termux** con Node.js, logrando independencia total (tu base de datos SQLite vive en tu celular sin requerir PC ni Wi-Fi). *Consulta la guía paso a paso para no programadores en [`mobile-android/README.md`](mobile-android/README.md).*
3. **Modalidad APK Nativo (Capacitor + Android Studio):**
   - La carpeta incluye `capacitor.config.json` para compilar un paquete instalador `.apk` tradicional mediante Android Studio y distribuirlo en cualquier dispositivo Android. *Consulta las instrucciones paso a paso detalladas en [`mobile-android/README.md`](mobile-android/README.md).*

---

### 🎛️ 3. Lanzador Maestro Interactivo (`iniciar.sh`)
Desde la raíz puedes ejecutar el menú de selección general:
```bash
./iniciar.sh
```
Te permitirá elegir si deseas iniciar la versión de Ubuntu, la de Android o instalar los accesos directos.

---

## 🔒 Modelo de Seguridad y Criptografía

* **Derivación Criptográfica PBKDF2:** 100,000 iteraciones SHA-256 sobre la contraseña maestra.
* **Cero Almacenamiento en Texto Plano:** La contraseña maestra jamás se almacena en disco.
* **Salting de Alta Entropía:** Cada base de datos (`tarjetas.db`) genera un salt pseudoaleatorio único de 16 bytes.
* **Acciones Críticas Protegidas:** Eliminar tarjetas, realizar cortes mensuales o cambiar claves exige revalidar la contraseña maestra.
* **Recuperación Local:** Si olvidas tu clave, el script `resetear-contrasena.sh` (presente en ambas carpetas) permite definir una nueva contraseña sin perder tarjetas ni movimientos.

---

## 🔄 Transferencia Voluntaria entre PC y Celular

Dado que son dos mundos independientes, no se mezclan automáticamente:
1. En el dispositivo de origen (PC o Celular), ve a **Ajustes > Sincronización**.
2. Pulsa **"Exportar Archivo de Sincronización (JSON)"**.
3. Pasa ese archivo al otro dispositivo e impórtalo. El sistema realizará una **fusión incremental (*merge*)** incorporando los registros nuevos sin borrar ni duplicar los existentes.

---

<div align="center">
  <sub>Arquitectura modular desarrollada bajo principios de soberanía de datos y escalabilidad.</sub><br>
  <sub><b>CardMaster © 2026 — Local-First Multi-Platform Architecture</b></sub>
</div>
