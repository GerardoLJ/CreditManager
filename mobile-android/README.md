# 📱 Guía Maestra para Usar CardMaster en tu Celular Android
> **Manual Paso a Paso para No Programadores**  
> *Aprende a usar CardMaster en tu teléfono sin complicaciones técnicas.*

---

Esta guía fue creada para que **cualquier persona, sin importar si nunca ha programado**, pueda tener sus tarjetas y finanzas personales directamente en su teléfono móvil Android.

---

## 🧭 ¿Qué método debo elegir?

Tienes **3 formas diferentes** de tener CardMaster en tu celular. Elige la que mejor se adapte a lo que necesitas:

| Método | ¿Qué necesitas descargar? | ¿Requiere la PC encendida? | Nivel de dificultad | Ideal para... |
| :--- | :--- | :--- | :--- | :--- |
| **Opción 1: Vía Wi-Fi (PWA)** | Nada nuevo (solo Google Chrome) | Sí (ambos en el mismo Wi-Fi) | 🟢 Muy Fácil (2 min) | Usar en casa o en tu oficina. |
| **Opción 2: 100% en el Celular (Termux)** | App gratuita **Termux** | ❌ No (tu celular es 100% autónomo) | 🟡 Intermedio (Paso a paso) | Llevar tus finanzas a la calle, viajes y centros comerciales. |
| **Opción 3: App Nativa (.APK con Capacitor)** | Compilar en tu PC con Android Studio | ❌ No | 🔵 Avanzado (Una sola vez) | Quienes quieren un instalador `.apk` tradicional para su teléfono. |

---

## 🌟 Opción 1: La Vía Más Rápida en Casa (Sin Descargar Nada)

Si estás en casa y quieres ver la app en tu teléfono sin instalar nada nuevo:

### Paso 1: En tu computadora
1. Abre tu terminal en Linux Ubuntu y ejecuta:
   ```bash
   cd /home/personal/Web-Personal/CreditManager/mobile-android
   ./iniciar-android.sh
   ```
2. La terminal detectará tu red Wi-Fi y te dará una dirección similar a esta:  
   `👉 http://192.168.1.45:3000` *(los números cambiarán según tu módem)*.

### Paso 2: En tu celular
1. Conecta tu celular a la **misma red Wi-Fi** de tu casa.
2. Abre **Google Chrome** en el celular.
3. En la barra de direcciones de arriba, escribe la dirección que te dio la PC (ejemplo: `http://192.168.1.45:3000`) y presiona entrar.

### Paso 3: Guardar como App en la pantalla
1. En Google Chrome, toca los **3 puntos verticales** (arriba a la derecha).
2. Toca **"Instalar aplicación"** o **"Añadir a la pantalla principal"**.
3. Confirma tocando **"Instalar"**.
4. ¡Listo! Se creará un icono de **CardMaster** en tu pantalla de inicio que abre la app a pantalla completa sin barras de navegador.

---

## 📲 Opción 2: Modalidad 100% en el Teléfono (Termux) — Explicación Profunda

Esta opción convierte tu teléfono en un **sistema completamente autónomo y privado**. No necesitas tener tu computadora prendida, no necesitas internet para usarla en el día a día y tu base de datos SQLite vive únicamente dentro de la memoria de tu celular.

### ❓ ¿Qué es Termux y cómo funciona de forma sencilla?
Imagina que **Termux** es como una pequeña ventana de comandos para tu teléfono Android. Aunque parezca algo técnico con una pantalla negra y texto, lo único que hace es permitir que tu teléfono ejecute el "motor" de CardMaster directamente en tu bolsillo.

---

### ⚠️ ¡ADVERTENCIA CRÍTICA: NO descargues Termux de Google Play Store!
> [!CAUTION]
> **No uses la tienda Google Play Store para descargar Termux.**  
> La versión de Google Play Store fue abandonada en el año 2020 debido a cambios en las políticas de Google. Si la descargas de ahí, fallará de inmediato mostrándote errores rojos que dicen `404 Not Found` o que los servidores están rotos.

**Descarga Termux únicamente desde su fuente oficial y gratuita en F-Droid o GitHub:**

1. Abre el navegador de tu celular (Chrome o Firefox).
2. Entra a este enlace oficial de F-Droid:  
   🔗 **[https://f-droid.org/packages/com.termux/](https://f-droid.org/packages/com.termux/)**  
   *(O descarga directa desde GitHub Releases: [https://github.com/termux/termux-app/releases](https://github.com/termux/termux-app/releases))*.
3. En la página de F-Droid, desliza hacia abajo hasta la sección de **"Versiones" (Versions)** y toca en **"Descargar APK" (Download APK)**.
4. Cuando termine de descargarse, toca el archivo descargado para instalarlo.  
   *(Si tu celular te dice "Por motivos de seguridad no puedes instalar aplicaciones desconocidas", toca en **Configuración**, activa el botón **Permitir desde esta fuente** y vuelve a tocar **Instalar**)*.

---

### 🛠️ Configuración Inicial en Termux (Paso a Paso — Solo se hace 1 vez)

Abre la aplicación **Termux** en tu teléfono. Verás una pantalla negra con texto. Solo debes copiar y pegar los siguientes comandos uno por uno:

#### 1. Actualizar el sistema de Termux
Escribe (o mantén presionado para pegar) lo siguiente y presiona la tecla **Enter** (la flecha azul o retorno de tu teclado):
```bash
pkg update -y
```
> [!NOTE]
> Durante la actualización, es normal que la pantalla se detenga y te haga preguntas con opciones como `[default=N]` o `[Y/n]`.  
> **No tienes que escribir nada:** simplemente presiona la tecla **Enter** en tu teclado cada vez que se detenga para aceptar la opción por defecto.

#### 2. Instalar Node.js y Git
Escribe y da Enter:
```bash
pkg install nodejs git -y
```
* **¿Qué hace esto?** `git` sirve para descargar el código del proyecto, y `nodejs` es el motor que hace funcionar CardMaster.

#### 3. Descargar el proyecto de GitHub a tu teléfono
Escribe y da Enter:
```bash
git clone https://github.com/GerardoLJ/CreditManager.git
```
*Se descargará una copia limpia del proyecto en la memoria de tu celular.*

#### 4. Entrar a la carpeta e instalar los paquetes
Escribe estos dos comandos en orden:
```bash
cd CreditManager/mobile-android
npm install
```
*Esperar unos segundos a que termine de descargar los paquetes internos de la app.*

#### 5. ¡Encender CardMaster en tu teléfono!
Escribe:
```bash
node server.js
```
Verás un mensaje que confirma que ya está funcionando:
```text
=====================================================
  💳 SERVIDOR CARDMASTER INICIADO CON ÉXITO
=====================================================
  👉 Modo Local:      http://localhost:3000
=====================================================
```

---

### 📱 Cómo Abrir y Ver la App en tu Celular

1. Deja la aplicación Termux abierta (no la cierres forzadamente; simplemente ve al menú principal de tu teléfono presionando el botón "Home").
2. Abre **Google Chrome** en tu teléfono.
3. En la barra de direcciones de arriba escribe:
   ```text
   http://localhost:3000
   ```
4. Verás la pantalla de inicio de CardMaster.
5. **Crea el icono directo en tu pantalla de inicio:**
   - Toca los **tres puntos verticales** de Chrome (arriba a la derecha).
   - Elige **"Instalar aplicación"** o **"Añadir a la pantalla principal"**.
   - Confirma tocando **"Instalar"**.

¡Listo! A partir de este momento tienes un icono en la pantalla de tu celular que abre CardMaster como una aplicación nativa.

---

### ⏱️ Tu Rutina Diaria (Cómo usarlo todos los días en 5 segundos)

No necesitas repetir toda la instalación todos los días. Tu rutina diaria es ultra sencilla:

#### Para apagarlo cuando termines tu día:
1. Abre Termux.
2. En la barra de teclas especiales que Termux tiene justo arriba de tu teclado, toca el botón **`Ctrl`** y luego la letra **`c`** en tu teclado (`Ctrl + C`).
3. El servidor se detendrá.

#### Para encenderlo al día siguiente:
1. Abre Termux.
2. Escribe solamente esto y presiona Enter:
   ```bash
   cd CreditManager/mobile-android && node server.js
   ```
3. Sal al menú de tu teléfono y toca el icono de **CardMaster**. ¡Tus datos estarán listos para registrar gastos!

#### 💡 Truco Pro: Encenderlo con una sola palabra (`iniciar`)
Si quieres hacer tu vida aún más fácil, ejecuta este comando en Termux **una sola vez**:
```bash
echo "alias iniciar='cd ~/CreditManager/mobile-android && node server.js'" >> ~/.bashrc && source ~/.bashrc
```
A partir de ahora, cada vez que abras Termux, solo escribe la palabra:
```bash
iniciar
```
...y presiona Enter. ¡CardMaster se encenderá de inmediato!

---

## 📦 Opción 3: Modalidad APK Nativo (Capacitor + Android Studio) — Explicación Profunda

Esta opción es para quienes desean compilar un **archivo instalador tradicional `.apk`** (como los instaladores de cualquier app de Android) utilizando la herramienta **Capacitor** y **Android Studio** desde su computadora.

La carpeta `mobile-android/` ya viene lista con el archivo de configuración `capacitor.config.json`:
* **Nombre de la App:** `CardMaster`
* **Identificador de Paquete:** `com.cardmaster.finance`
* **Directorio de la Interfaz:** `public`

---

### 📋 Requisitos Previos en tu Computadora (PC con Ubuntu)

1. **Tener Node.js instalado:** Ya está instalado en tu computadora.
2. **Tener Android Studio instalado:**
   - Si aún no lo tienes, puedes instalarlo gratis en Ubuntu abriendo la terminal y ejecutando:
     ```bash
     sudo snap install android-studio --classic
     ```
   - Abre Android Studio por primera vez y completa el asistente básico ("Next, Next, Finish") para que descargue las herramientas del SDK de Android.

---

### 🛠️ Paso a Paso para Generar el Archivo `.apk`

Sigue estos pasos en la terminal de tu computadora:

#### Paso 1: Entrar a la carpeta de Android
```bash
cd /home/personal/Web-Personal/CreditManager/mobile-android
```

#### Paso 2: Instalar los paquetes oficiales de Capacitor
```bash
npm install @capacitor/core @capacitor/cli @capacitor/android
```
*(Esto descarga las herramientas necesarias para empaquetar aplicaciones en Android).*

#### Paso 3: Agregar la plataforma Android al proyecto
```bash
npx cap add android
```
*(Capacitor creará automáticamente una carpeta llamada `android/` con todo el código nativo de Java/Kotlin listo).*

#### Paso 4: Sincronizar los archivos de la app
```bash
npx cap sync
```
*(Esto copia la carpeta `public/` con toda la interfaz visual, iconos y pantallas dentro del proyecto de Android).*

#### Paso 5: Abrir el proyecto en Android Studio
```bash
npx cap open android
```
*(Este comando abrirá automáticamente el programa Android Studio con el proyecto de CardMaster cargado).*

---

### 🖥️ Instrucciones Visuales Dentro de Android Studio

Una vez que Android Studio se abra en tu pantalla:

1. **Espera la carga inicial:** En la parte inferior verás una barra que dice *"Gradle Build"* o *"Syncing"*. Espera de 1 a 2 minutos a que termine de indexar.
2. **Generar el APK:** En la barra de menús superior de Android Studio, haz clic en:  
   👉 **Build** ➔ **Build Bundle(s) / APK(s)** ➔ **Build APK(s)**.
3. **Espera la compilación:** Abajo a la derecha verás una pequeña barra de progreso. La computadora tardará unos momentos en ensamblar la aplicación.
4. **Localizar el archivo `.apk`:**  
   Cuando termine, aparecerá una notificación emergente abajo a la derecha diciendo:  
   `APK(s) generated successfully for 1 module: 'app'`  
   Junto a ese mensaje hay una palabra azul que dice **locate**. Haz clic en **locate**.
5. **Tu archivo listo:** Se abrirá el explorador de archivos mostrando tu archivo llamado:  
   **`app-debug.apk`**  
   *(La ruta exacta donde se guardó es: `/home/personal/Web-Personal/CreditManager/mobile-android/android/app/build/outputs/apk/debug/app-debug.apk`)*.

---

### 📲 Cómo Pasar e Instalar el `.apk` en tu Celular

1. **Enviar el archivo a tu celular:**
   - **Opción A (Cable USB):** Conecta el celular a la PC con el cable, selecciona "Transferencia de archivos" y pega el archivo `app-debug.apk` en la carpeta **Descargas** de tu teléfono.
   - **Opción B (Sin cables):** Sube el archivo `app-debug.apk` a tu **Google Drive**, envíatelo por **Telegram** o por correo electrónico y descárgalo en tu teléfono.
2. **Instalar en el celular:**
   - En tu teléfono, abre la app "Archivos" o "Descargas".
   - Toca el archivo **`app-debug.apk`**.
   - Si tu celular te muestra una advertencia de seguridad: *"Por motivos de seguridad, tu teléfono no tiene permitido instalar apps desconocidas de esta fuente"*:
     - Toca en **Configuración / Ajustes**.
     - Activa la casilla **"Permitir desde esta fuente"**.
     - Vuelve atrás y toca **Instalar**.
3. ¡Felicidades! Ahora tienes **CardMaster** instalado como cualquier aplicación del sistema en tu teléfono.

---

## 💳 Primeros Pasos Usando CardMaster

Una vez que tengas la aplicación abierta en tu teléfono por cualquiera de los 3 métodos:

1. **Crear tu Contraseña Maestra:**
   La primera vez que entres, el sistema te pedirá definir una Contraseña Maestra personal. Escribe una que recuerdes bien y confírmala.
2. **Agregar tus Tarjetas de Crédito:**
   En la pantalla principal, toca el botón azul **"+ Nueva Tarjeta"**. Ingresa el nombre (ej. *BBVA Azul*, *Nu*, *RappiCard*), tu límite de crédito y el día de corte.
3. **Registrar Gastos:**
   Toca la pestaña **Movimientos** y pulsa **"+ Nuevo Gasto"**. Puedes indicar si fue una compra normal o a Meses Sin Intereses (MSI).
4. **Editar Movimientos si te equivocaste:**
   En la lista de movimientos, cada registro tiene un botón con un lápiz **✏️**. Tócalo para cambiar el monto, el concepto o la fecha sin tener que borrar el registro.
5. **Apartar Dinero para Pagar:**
   En la pestaña **Fondos Apartados**, pulsa **"+ Apartar Dinero"** para separar el dinero que ya tienes listo para liquidar tus tarjetas.
6. **Mover Tarjetas de Lugar:**
   Puedes organizar las tarjetas en el orden que más te guste arrastrándolas con el dedo. El orden se guarda automáticamente.

---

## 🆘 Preguntas Frecuentes y Solución de Problemas

### 1. ¿Dónde se guardan mis tarjetas y mis gastos?
Toda tu información se guarda en un archivo físico privado llamado **`tarjetas.db`** dentro de la carpeta `mobile-android`. Esa información reside exclusivamente en tu dispositivo, sin servidores externos, garantizando tu total privacidad.

### 2. ¿Qué pasa si olvido mi contraseña en el celular?
No perderás tus datos ni tus tarjetas. Para restablecer tu contraseña:
- Si usas **Termux en el celular**:
  ```bash
  cd ~/CreditManager/mobile-android
  ./resetear-contrasena.sh
  ```
- Si usas la PC conectada por Wi-Fi:
  ```bash
  cd /home/personal/Web-Personal/CreditManager/mobile-android
  ./resetear-contrasena.sh
  ```
Esto limpiará únicamente la contraseña olvidada. Cuando vuelvas a entrar a la app, te pedirá inventar una nueva contraseña y todos tus gastos seguirán ahí intactos.

### 3. ¿Cómo paso mis datos de la PC al celular (o viceversa)?
Recuerda que la PC y el celular son dos mundos independientes que no se mezclan automáticamente:
1. En el dispositivo de origen, entra a la pestaña **Ajustes**.
2. Toca **"Exportar Archivo de Sincronización (JSON)"** y guarda el archivo.
3. Envía ese archivo al otro dispositivo (por correo, WhatsApp Web o cable).
4. En el otro dispositivo, ve a **Ajustes** y selecciona ese archivo para importarlo. El sistema fusionará tus datos sin borrar nada.

---

<div align="center">
  <sub>Desarrollado para máxima privacidad, autonomía financiera y facilidad de uso.</sub><br>
  <sub><b>CardMaster Móvil © 2026</b></sub>
</div>
