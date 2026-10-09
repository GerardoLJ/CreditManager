#!/usr/bin/env python3
# ==============================================================
#  CardMaster - Aplicación Nativa de Escritorio para Linux Ubuntu
# ==============================================================

import os
import sys
import time
import signal
import subprocess
import urllib.request

DIR = os.path.dirname(os.path.abspath(__file__))
SERVER_URL = "http://127.0.0.1:3000"
ICON_PATH = os.path.join(DIR, "public", "icon-512.png")
LOG_PATH = os.path.join(DIR, "server.log")

def is_server_running():
    try:
        with urllib.request.urlopen(f"{SERVER_URL}/api/database/info", timeout=0.6) as res:
            return res.status == 200
    except Exception:
        return False

PID_PATH = os.path.join(DIR, "server.pid")

# Iniciar servidor Node.js si no está activo
server_proc = None
if not is_server_running():
    log_file = open(LOG_PATH, "a")
    server_proc = subprocess.Popen(
        ["node", "server.js"],
        cwd=DIR,
        stdout=log_file,
        stderr=log_file
    )
    try:
        with open(PID_PATH, "w") as f:
            f.write(str(server_proc.pid))
    except Exception:
        pass

    # Esperar hasta que el backend esté listo
    for _ in range(30):
        if is_server_running():
            break
        time.sleep(0.2)

def cleanup_server():
    global server_proc
    if server_proc:
        try:
            server_proc.terminate()
            server_proc.wait(timeout=1.5)
        except Exception:
            try:
                server_proc.kill()
            except Exception:
                pass
        server_proc = None
    if os.path.exists(PID_PATH):
        try:
            os.remove(PID_PATH)
        except Exception:
            pass

def sig_handler(sig, frame):
    cleanup_server()
    sys.exit(0)

signal.signal(signal.SIGINT, sig_handler)
signal.signal(signal.SIGTERM, sig_handler)

# Intentar abrir con ventana GTK WebKit2 nativa (100% escritorio sin navegador)
try:
    import gi
    gi.require_version('Gtk', '3.0')
    try:
        gi.require_version('WebKit2', '4.1')
    except ValueError:
        gi.require_version('WebKit2', '4.0')
    from gi.repository import Gtk, WebKit2, GdkPixbuf

    class CardMasterDesktopWindow(Gtk.Window):
        def __init__(self):
            super().__init__(title="CardMaster - Control Financiero")
            self.set_default_size(1280, 820)
            self.set_position(Gtk.WindowPosition.CENTER)

            if os.path.exists(ICON_PATH):
                try:
                    self.set_icon_from_file(ICON_PATH)
                except Exception:
                    pass

            self.webview = WebKit2.WebView()
            settings = self.webview.get_settings()
            settings.set_enable_javascript(True)
            settings.set_enable_webgl(True)
            settings.set_enable_developer_extras(False)
            
            # Cargar la app
            self.webview.load_uri(SERVER_URL)
            self.add(self.webview)
            self.connect("destroy", self.on_close)

        def on_close(self, widget):
            cleanup_server()
            Gtk.main_quit()

    app = CardMasterDesktopWindow()
    app.show_all()
    Gtk.main()

except Exception as e:
    # Fallback automático: Abrir como ventana de aplicación independiente en Brave, Chrome o navegador
    print(f"Iniciando modo ventana de aplicación: {e}")
    import shutil
    browsers = [
        ["brave-browser-stable", f"--app={SERVER_URL}"],
        ["brave-browser", f"--app={SERVER_URL}"],
        ["brave", f"--app={SERVER_URL}"],
        ["google-chrome", f"--app={SERVER_URL}"],
        ["google-chrome-stable", f"--app={SERVER_URL}"],
        ["chromium-browser", f"--app={SERVER_URL}"],
        ["chromium", f"--app={SERVER_URL}"],
        ["microsoft-edge", f"--app={SERVER_URL}"],
        ["xdg-open", SERVER_URL]
    ]
    launched = False
    for cmd in browsers:
        if shutil.which(cmd[0]):
            try:
                subprocess.Popen(cmd)
                launched = True
                break
            except Exception:
                continue
    if not launched:
        try:
            subprocess.Popen(["xdg-open", SERVER_URL])
        except Exception:
            pass
