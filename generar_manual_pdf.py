#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generador del Manual Integral de Arquitectura, Operación y Seguridad de CardMaster.
Produce el archivo Manual_Completo_CardMaster.pdf con calidad vectorial editorial mediante PyCairo.
"""

import math
import cairo
import os

OUTPUT_PDF = "Manual_Completo_CardMaster.pdf"

PAGE_W = 612
PAGE_H = 792
MARGIN_L = 46
MARGIN_R = 46
CONTENT_W = PAGE_W - MARGIN_L - MARGIN_R
TOTAL_PAGES = 7

# Paleta Cromática
C_DARK_NAVY    = (0.06, 0.09, 0.16)   # #0f172a
C_SURFACE_DARK = (0.12, 0.16, 0.23)   # #1e293b
C_BRAND_BLUE   = (0.14, 0.38, 0.92)   # #2563eb
C_TEXT_MAIN    = (0.20, 0.25, 0.35)   # #334155
C_TEXT_MUTED   = (0.40, 0.45, 0.55)   # #64748b
C_TEXT_WHITE   = (0.98, 0.98, 1.00)   # #f8fafc
C_BORDER       = (0.85, 0.88, 0.92)   # #cbd5e1
C_EMERALD      = (0.02, 0.59, 0.41)   # #059669
C_AMBER        = (0.85, 0.47, 0.02)   # #d97706
C_ROSE         = (0.88, 0.11, 0.28)   # #e11d48
C_BG_BOX       = (0.96, 0.97, 0.99)   # #f8fafc

def wrap_text(ctx, text, max_w):
    words = text.split()
    lines = []
    curr = []
    for w in words:
        test = " ".join(curr + [w])
        if ctx.text_extents(test)[2] <= max_w:
            curr.append(w)
        else:
            if curr:
                lines.append(" ".join(curr))
            curr = [w]
    if curr:
        lines.append(" ".join(curr))
    return lines

def draw_rounded_rect(ctx, x, y, w, h, r):
    ctx.new_sub_path()
    ctx.arc(x + w - r, y + r, r, -math.pi/2, 0)
    ctx.arc(x + w - r, y + h - r, r, 0, math.pi/2)
    ctx.arc(x + r, y + h - r, r, math.pi, 3*math.pi/2)
    ctx.arc(x + r, y + r, r, math.pi, 3*math.pi/2)
    ctx.close_path()

def draw_header(ctx, section_title, page_num):
    if page_num == 1:
        return
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_BOLD)
    ctx.set_font_size(7.5)
    ctx.set_source_rgb(*C_TEXT_MUTED)
    ctx.move_to(MARGIN_L, 34)
    ctx.show_text("CARDMASTER — MANUAL INTEGRAL DE ARQUITECTURA Y OPERACIÓN")
    
    sec_w = ctx.text_extents(section_title)[2]
    ctx.move_to(PAGE_W - MARGIN_R - sec_w, 34)
    ctx.show_text(section_title)
    
    ctx.set_source_rgb(*C_BORDER)
    ctx.set_line_width(0.8)
    ctx.move_to(MARGIN_L, 40)
    ctx.line_to(PAGE_W - MARGIN_R, 40)
    ctx.stroke()

def draw_footer(ctx, page_num):
    if page_num == 1:
        return
    ctx.set_source_rgb(*C_BORDER)
    ctx.set_line_width(0.8)
    ctx.move_to(MARGIN_L, PAGE_H - 34)
    ctx.line_to(PAGE_W - MARGIN_R, PAGE_H - 34)
    ctx.stroke()
    
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_NORMAL)
    ctx.set_font_size(7.5)
    ctx.set_source_rgb(*C_TEXT_MUTED)
    ctx.move_to(MARGIN_L, PAGE_H - 22)
    ctx.show_text("Soberanía de Datos • Base de Datos Local SQLite • Confidencial")
    
    page_str = f"Página {page_num} de {TOTAL_PAGES}"
    p_w = ctx.text_extents(page_str)[2]
    ctx.move_to(PAGE_W - MARGIN_R - p_w, PAGE_H - 22)
    ctx.show_text(page_str)

def draw_h1(ctx, text, y, icon=""):
    ctx.set_source_rgb(*C_BRAND_BLUE)
    draw_rounded_rect(ctx, MARGIN_L, y - 13, 4, 18, 2)
    ctx.fill()
    
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_BOLD)
    ctx.set_font_size(13.5)
    ctx.set_source_rgb(*C_DARK_NAVY)
    full_t = f"{icon}  {text}" if icon else text
    ctx.move_to(MARGIN_L + 12, y)
    ctx.show_text(full_t)
    return y + 18

def draw_h2(ctx, text, y):
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_BOLD)
    ctx.set_font_size(10.2)
    ctx.set_source_rgb(*C_SURFACE_DARK)
    ctx.move_to(MARGIN_L, y)
    ctx.show_text(text)
    return y + 14

def draw_p(ctx, text, y, max_w=CONTENT_W, font_size=8.7, line_h=12.4, color=C_TEXT_MAIN):
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_NORMAL)
    ctx.set_font_size(font_size)
    ctx.set_source_rgb(*color)
    lines = wrap_text(ctx, text, max_w)
    curr_y = y
    for line in lines:
        ctx.move_to(MARGIN_L, curr_y)
        ctx.show_text(line)
        curr_y += line_h
    return curr_y + 4

def draw_bullet(ctx, bold_prefix, text, y, max_w=CONTENT_W, font_size=8.6, line_h=12.2):
    ctx.set_source_rgb(*C_BRAND_BLUE)
    ctx.arc(MARGIN_L + 4, y - 3, 2.2, 0, 2*math.pi)
    ctx.fill()
    
    full_text = f"{bold_prefix}: {text}" if bold_prefix else text
    lines = wrap_text(ctx, full_text, max_w - 14)
    curr_y = y
    first = True
    for line in lines:
        if first and bold_prefix:
            ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_BOLD)
            ctx.set_font_size(font_size)
            ctx.set_source_rgb(*C_DARK_NAVY)
            p_text = f"{bold_prefix}: "
            ctx.move_to(MARGIN_L + 14, curr_y)
            ctx.show_text(p_text)
            p_w = ctx.text_extents(p_text)[2]
            
            rest = line[len(p_text):] if line.startswith(p_text) else ""
            if rest:
                ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_NORMAL)
                ctx.set_font_size(font_size)
                ctx.set_source_rgb(*C_TEXT_MAIN)
                ctx.move_to(MARGIN_L + 14 + p_w, curr_y)
                ctx.show_text(rest)
            first = False
        else:
            ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_NORMAL)
            ctx.set_font_size(font_size)
            ctx.set_source_rgb(*C_TEXT_MAIN)
            ctx.move_to(MARGIN_L + 14, curr_y)
            ctx.show_text(line)
        curr_y += line_h
    return curr_y + 3

def draw_callout(ctx, title, body, y, style="info", max_w=CONTENT_W):
    theme_colors = {
        "info":    (C_BRAND_BLUE, (0.94, 0.97, 1.00)),
        "success": (C_EMERALD,    (0.93, 0.98, 0.95)),
        "warning": (C_AMBER,      (0.99, 0.98, 0.93)),
        "danger":  (C_ROSE,       (0.99, 0.94, 0.95))
    }
    accent, bg = theme_colors.get(style, (C_BRAND_BLUE, (0.95, 0.96, 0.98)))
    
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_NORMAL)
    ctx.set_font_size(8.3)
    lines = wrap_text(ctx, body, max_w - 24)
    box_h = 24 + len(lines) * 11.6
    
    ctx.set_source_rgb(*bg)
    draw_rounded_rect(ctx, MARGIN_L, y, max_w, box_h, 6)
    ctx.fill()
    
    ctx.set_source_rgb(*accent)
    draw_rounded_rect(ctx, MARGIN_L, y, 4, box_h, 2)
    ctx.fill()
    
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_BOLD)
    ctx.set_font_size(8.7)
    ctx.set_source_rgb(*accent)
    ctx.move_to(MARGIN_L + 12, y + 13)
    ctx.show_text(title)
    
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_NORMAL)
    ctx.set_font_size(8.2)
    ctx.set_source_rgb(*C_TEXT_MAIN)
    curr_y = y + 25
    for line in lines:
        ctx.move_to(MARGIN_L + 12, curr_y)
        ctx.show_text(line)
        curr_y += 11.6
        
    return y + box_h + 8

def draw_table(ctx, headers, rows, col_widths, y):
    h_height = 18
    r_height = 16
    table_w = sum(col_widths)
    
    ctx.set_source_rgb(*C_SURFACE_DARK)
    draw_rounded_rect(ctx, MARGIN_L, y, table_w, h_height, 3)
    ctx.fill()
    
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_BOLD)
    ctx.set_font_size(8.0)
    ctx.set_source_rgb(*C_TEXT_WHITE)
    curr_x = MARGIN_L + 8
    for i, h in enumerate(headers):
        ctx.move_to(curr_x, y + 12)
        ctx.show_text(h)
        curr_x += col_widths[i]
        
    curr_y = y + h_height
    for row_idx, row in enumerate(rows):
        if row_idx % 2 == 1:
            ctx.set_source_rgb(0.96, 0.97, 0.99)
            ctx.rectangle(MARGIN_L, curr_y, table_w, r_height)
            ctx.fill()
            
        ctx.set_source_rgb(*C_BORDER)
        ctx.set_line_width(0.4)
        ctx.move_to(MARGIN_L, curr_y + r_height)
        ctx.line_to(MARGIN_L + table_w, curr_y + r_height)
        ctx.stroke()
        
        ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_NORMAL)
        ctx.set_font_size(7.8)
        ctx.set_source_rgb(*C_TEXT_MAIN)
        curr_x = MARGIN_L + 8
        for i, cell in enumerate(row):
            ctx.move_to(curr_x, curr_y + 11.5)
            ctx.show_text(cell[:col_widths[i]//6])
            curr_x += col_widths[i]
        curr_y += r_height
        
    return curr_y + 8

# ==============================================================
# GENERACIÓN DE PÁGINAS
# ==============================================================

surface = cairo.PDFSurface(OUTPUT_PDF, PAGE_W, PAGE_H)
ctx = cairo.Context(surface)

# --------------------------------------------------------------
# PÁGINA 1: PORTADA EJECUTIVA
# --------------------------------------------------------------
# Hero Background
ctx.set_source_rgb(*C_DARK_NAVY)
draw_rounded_rect(ctx, MARGIN_L, 46, CONTENT_W, 230, 12)
ctx.fill()

# Decorative accent bar
ctx.set_source_rgb(*C_BRAND_BLUE)
draw_rounded_rect(ctx, MARGIN_L + 24, 70, 36, 4, 2)
ctx.fill()

ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_BOLD)
ctx.set_font_size(24)
ctx.set_source_rgb(*C_TEXT_WHITE)
ctx.move_to(MARGIN_L + 24, 110)
ctx.show_text("CARDMASTER / CREDITMANAGER")

ctx.set_font_size(12)
ctx.set_source_rgb(0.70, 0.80, 0.98)
ctx.move_to(MARGIN_L + 24, 132)
ctx.show_text("Sistema Local de Gestión de Crédito, MSI, Apartados y Presupuestos")

ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_NORMAL)
ctx.set_font_size(9.2)
ctx.set_source_rgb(0.75, 0.82, 0.92)
ctx.move_to(MARGIN_L + 24, 154)
ctx.show_text("Manual Integral de Arquitectura, Operación, Seguridad y Despliegue Multiplataforma")

# Badges en Hero
badges = [
    ("Desktop Ubuntu (GTK3)", C_ROSE),
    ("Android (PWA / APK)", C_EMERALD),
    ("SQLite 3 Embebido", C_BRAND_BLUE),
    ("PBKDF2 SHA-256 (100k)", C_AMBER)
]
bx = MARGIN_L + 24
for btext, bcol in badges:
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_BOLD)
    ctx.set_font_size(7.8)
    bw = ctx.text_extents(btext)[2] + 16
    ctx.set_source_rgb(*bcol)
    draw_rounded_rect(ctx, bx, 200, bw, 20, 10)
    ctx.fill()
    ctx.set_source_rgb(*C_TEXT_WHITE)
    ctx.move_to(bx + 8, 213.5)
    ctx.show_text(btext)
    bx += bw + 10

# Ficha Técnica en Tarjeta
card_y = 296
ctx.set_source_rgb(*C_BG_BOX)
draw_rounded_rect(ctx, MARGIN_L, card_y, CONTENT_W, 200, 10)
ctx.fill()
ctx.set_source_rgb(*C_BORDER)
ctx.set_line_width(0.8)
draw_rounded_rect(ctx, MARGIN_L, card_y, CONTENT_W, 200, 10)
ctx.stroke()

ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_BOLD)
ctx.set_font_size(10.5)
ctx.set_source_rgb(*C_DARK_NAVY)
ctx.move_to(MARGIN_L + 20, card_y + 26)
ctx.show_text("FICHA TÉCNICA Y ESPECIFICACIONES DEL SISTEMA")

items_ficha = [
    ("Paradigma Arquitectónico", "Local-First Software con Soberanía Absoluta (Cero Servidores en la Nube)"),
    ("Estructura de Despliegue", "Proyectos 100% Desacoplados: 'desktop-ubuntu/' y 'mobile-android/'"),
    ("Entorno de Escritorio", "Linux Ubuntu Nativo en Ventana GTK3 / WebKitGTK 4.1 (Sin navegador web)"),
    ("Entorno Móvil", "Android PWA (App de Pantalla Completa), Termux Local o Empaquetado APK"),
    ("Motor de Almacenamiento", "Bases de datos físicas SQLite 3 independientes ('tarjetas.db' en cada carpeta)"),
    ("Bóveda de Seguridad", "Derivación PBKDF2 con SHA-256 (100,000 iteraciones) + Salt aleatorio único"),
    ("Versión del Documento", "Edición Modular 2.5 — Octubre 2026")
]
fy = card_y + 48
for k, v in items_ficha:
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_BOLD)
    ctx.set_font_size(8.3)
    ctx.set_source_rgb(*C_BRAND_BLUE)
    ctx.move_to(MARGIN_L + 20, fy)
    ctx.show_text(f"{k}:")
    
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_NORMAL)
    ctx.set_font_size(8.3)
    ctx.set_source_rgb(*C_TEXT_MAIN)
    ctx.move_to(MARGIN_L + 180, fy)
    ctx.show_text(v)
    fy += 21

# Tabla de Contenidos Rápida
cy = 516
draw_h2(ctx, "ÍNDICE GENERAL DE CONTENIDOS", cy)
indice = [
    ("Sección 1", "Estructura Física del Proyecto y Dónde está el Código (Carpetas Separadas)"),
    ("Sección 2", "Modelo de Seguridad Criptográfica, Bóveda PBKDF2 y Recuperación"),
    ("Sección 3", "Guía Completa de Módulos (Tarjetas, MSI, Apartados, Calendario y Día Dorado)"),
    ("Sección 4", "Aplicación Nativa de Escritorio para Linux Ubuntu (GTK3 / WebKit2)"),
    ("Sección 5", "Independencia Móvil para Android (PWA, Termux, APK) y Solución de Incidentes")
]
iy = cy + 20
for s, d in indice:
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_BOLD)
    ctx.set_font_size(8.3)
    ctx.set_source_rgb(*C_SURFACE_DARK)
    ctx.move_to(MARGIN_L + 10, iy)
    ctx.show_text(f"•  {s}:")
    
    ctx.select_font_face("DejaVu Sans", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_NORMAL)
    ctx.set_font_size(8.3)
    ctx.set_source_rgb(*C_TEXT_MAIN)
    ctx.move_to(MARGIN_L + 95, iy)
    ctx.show_text(d)
    iy += 17

draw_footer(ctx, 1)
surface.show_page()

# --------------------------------------------------------------
# PÁGINA 2: ESTRUCTURA DEL PROYECTO
# --------------------------------------------------------------
draw_header(ctx, "Estructura y Código", 2)
y = 58
y = draw_h1(ctx, "1. ¿En Dónde está el Código? — Arquitectura Modular", y)

y = draw_p(ctx, "CardMaster está estructurado bajo un modelo de desacoplamiento total en dos carpetas independientes. Cada plataforma posee su propio código fuente, dependencias, base de datos SQLite y scripts de arranque, garantizando que si se elimina cualquiera de las dos carpetas o se transfiere a otro equipo, funcione de manera autónoma sin errores.", y)

y = draw_callout(ctx, "Garantía de Independencia de Código", "Las carpetas 'desktop-ubuntu/' y 'mobile-android/' son dos mundos desconectados. No comparten una base de datos central ni enlaces cruzados frágiles. Cada proyecto vive, escala y se respalda con autonomía absoluta.", y, "success")

y = draw_h2(ctx, "Mapa del Directorio del Proyecto:", y)

headers_code = ["Carpeta / Archivo", "Tipo", "Responsabilidad en el Sistema"]
rows_code = [
    ["desktop-ubuntu/", "Proyecto", "Aplicación nativa 100% de escritorio para Linux Ubuntu."],
    ["desktop-ubuntu/desktop-app.py", "Python/GTK3", "Lanzador de ventana nativa de escritorio sin navegador."],
    ["desktop-ubuntu/iniciar-ubuntu.sh", "Bash Script", "Arranque en 1 paso del servicio y la ventana GTK3."],
    ["desktop-ubuntu/CardMaster.desktop", "Lanzador", "Acceso directo oficial para el Escritorio y menú GNOME."],
    ["desktop-ubuntu/tarjetas.db", "SQLite 3", "Base de datos física exclusiva de la versión de PC."],
    ["mobile-android/", "Proyecto", "Aplicación autónoma para dispositivos móviles Android."],
    ["mobile-android/iniciar-android.sh", "Bash Script", "Servidor móvil con detección de IP Wi-Fi y soporte PWA."],
    ["mobile-android/capacitor.config.json", "Config", "Configuración para empaquetado a paquete APK nativo."],
    ["mobile-android/tarjetas.db", "SQLite 3", "Base de datos física exclusiva del teléfono celular."],
    ["iniciar.sh / detener.sh", "Scripts Raíz", "Menú interactivo general para elegir qué versión arrancar."]
]
y = draw_table(ctx, headers_code, rows_code, [170, 90, 260], y)

y = draw_h2(ctx, "Capa Frontend y Componentes de Usuario (public/):", y)
y = draw_bullet(ctx, "index.html", "Maquetación semántica SPA con Sidebar fija en PC y menú cajón (☰) en móvil.", y)
y = draw_bullet(ctx, "styles.css", "Motor visual con modo Claro/Oscuro, selectores personalizados y grids dinámicos.", y)
y = draw_bullet(ctx, "app.js", "Controlador cliente con formateo de moneda, drag & drop de tarjetas y edición de gastos.", y)
y = draw_bullet(ctx, "manifest.json & SW", "Soporte PWA para instalación de pantalla completa sin depender de tiendas.", y)

draw_footer(ctx, 2)
surface.show_page()

# --------------------------------------------------------------
# PÁGINA 3: SEGURIDAD Y CRIPTOGRAFÍA
# --------------------------------------------------------------
draw_header(ctx, "Seguridad y Bóveda", 3)
y = 58
y = draw_h1(ctx, "2. Modelo de Seguridad y Privacidad Criptográfica", y)

y = draw_p(ctx, "El resguardo de la información financiera en CardMaster sigue estándares criptográficos de grado industrial para almacenamiento local, protegiendo los datos incluso si un tercero tiene acceso físico al disco duro.", y)

y = draw_h2(ctx, "Pilares de Seguridad Implementados:", y)
y = draw_bullet(ctx, "Derivación PBKDF2 (SHA-256)", "La clave maestra nunca se almacena en texto plano. Se procesa con la función de derivación basada en contraseñas ejecutando 100,000 iteraciones con la función resumen SHA-256.", y)
y = draw_bullet(ctx, "Salting de Alta Entropía", "Para cada archivo tarjetas.db se genera una sal aleatoria criptográfica única de 16 bytes (crypto.randomBytes(16)), anulando ataques con tablas precalculadas (Rainbow Tables).", y)
y = draw_bullet(ctx, "Revalidación en Acciones Críticas", "Operaciones como el reseteo mensual del corte, la eliminación de tarjetas o el cambio de clave exigen obligatoriamente reingresar la contraseña maestra activa.", y)
y = draw_bullet(ctx, "Aislamiento Físico y Exclusión", "El archivo tarjetas.db está formalmente excluido en .gitignore y .dockerignore para impedir fugas involuntarias de saldos o consumos hacia repositorios públicos.", y)

y = draw_callout(ctx, "Filosofía Local-First (Cero Nube)", "Ningún dato viaja jamás a servidores externos, analytics ni servicios de telemetría de terceros. La base de datos es un único archivo SQLite que vive físicamente en tu propio equipo.", y, "info")

y = draw_h2(ctx, "Procedimiento de Rescate: Olvido de Contraseña Maestra", y)
y = draw_p(ctx, "Dado que el algoritmo PBKDF2 es irreversible por diseño matemático, no es posible 'descifrar' la contraseña. No obstante, el sistema incluye una herramienta de rescate local en ambas carpetas:", y)

y = draw_bullet(ctx, "En Linux Desktop", "Ejecuta:  cd desktop-ubuntu && ./resetear-contrasena.sh", y)
y = draw_bullet(ctx, "En Android / Móvil", "Ejecuta:  cd mobile-android && ./resetear-contrasena.sh", y)
y = draw_bullet(ctx, "Efecto Técnico", "El script elimina únicamente la clave 'master_pwd_hash' de la tabla 'app_config'. Al abrir de nuevo la app, el sistema te solicitará crear una nueva clave sin alterar tarjetas, deudas ni compras.", y)

y = draw_callout(ctx, "Prevención ante Pérdida de Equipo", "Se recomienda realizar copias periódicas de tu archivo tarjetas.db hacia una memoria USB o almacenamiento cifrado personal utilizando el botón de descarga en la pestaña 'Ajustes'.", y, "warning")

draw_footer(ctx, 3)
surface.show_page()

# --------------------------------------------------------------
# PÁGINA 4: TARJETAS, MOVIMIENTOS Y MSI
# --------------------------------------------------------------
draw_header(ctx, "Tarjetas, Gastos y MSI", 4)
y = 58
y = draw_h1(ctx, "3. Funcionamiento: Tarjetas, Gastos y Amortización MSI", y)

y = draw_h2(ctx, "Módulo de Tarjetas de Crédito y Grid Inteligente:", y)
y = draw_p(ctx, "Es la pantalla principal del sistema financiero. Muestra cada tarjeta con su logotipo, colores y métricas de solvencia calculadas en tiempo real:", y)
y = draw_bullet(ctx, "Límite Total y Disponible Real", "El crédito real disponible deduce tanto los gastos corrientes del mes como el saldo total comprometido por compras a plazos.", y)
y = draw_bullet(ctx, "Bolsa de Apartados por Plástico", "Indica cuánto dinero ya tienes reunido en físico (Efectivo) o cuenta de débito para liquidar ese plástico bancario en particular.", y)
y = draw_bullet(ctx, "Distribución Dinámica", "El grid se adapta suavemente a cualquier resolución, evitando espacios vacíos desproporcionados sin encimar las tarjetas.", y)
y = draw_bullet(ctx, "Reordenamiento Drag & Drop", "Puedes arrastrar cualquier tarjeta con el mouse para posicionarla en el orden que prefieras; el orden se memoriza de forma permanente.", y)

y = draw_h2(ctx, "Módulo de Movimientos y Edición de Gastos:", y)
y = draw_bullet(ctx, "Botón de Edición (✏️)", "Permite editar montos, conceptos, fechas, plásticos o responsables de cualquier movimiento sin necesidad de borrarlo y crearlo de nuevo.", y)
y = draw_bullet(ctx, "Formato Automático de Moneda", "Todos los campos monetarios formatean en tiempo real con comas en miles y punto en decimales (ej. $15,000.00), evitando errores de captura.", y)
y = draw_bullet(ctx, "Asignación de Responsable", "Permite asociar cada compra a tu cuenta 'Personal' o a un tercero deudor para control de cobranza.", y)

y = draw_h2(ctx, "Mecánica Contable de Amortización a Meses Sin Intereses (MSI):", y)
y = draw_p(ctx, "Al registrar compras a 3, 6, 9, 12, 18 o 24 meses, el sistema aplica la lógica bancaria real:", y)
y = draw_bullet(ctx, "Impacto en Cupo de Crédito", "Deduce el 100% del valor del producto del crédito disponible (reflejando la retención bancaria).", y)
y = draw_bullet(ctx, "Cobro en Corte Mensual", "En el pago mensual actual, exige únicamente la cuota vigente (Monto Total / Plazo).", y)
y = draw_bullet(ctx, "Contador de Cuotas", "Monitorea el avance exacto (ej. Cuota 3 de 12) y libera cupo proporcionalmente con cada ciclo.", y)

draw_footer(ctx, 4)
surface.show_page()

# --------------------------------------------------------------
# PÁGINA 5: FONDOS APARTADOS Y CALENDARIO
# --------------------------------------------------------------
draw_header(ctx, "Apartados y Calendario", 5)
y = 58
y = draw_h1(ctx, "3. Funcionamiento: Fondos Apartados y Día Dorado", y)

y = draw_h2(ctx, "Módulo de Fondos Apartados (Dinero en Mano vs Débito):", y)
y = draw_p(ctx, "Este módulo resuelve el problema de gastarse el dinero antes de que llegue la fecha límite del banco. Cuando un deudor te entrega dinero o tú separas capital de tus ingresos, se registra un apartado:", y)
y = draw_bullet(ctx, "Doble Imputación Contable", "Disminuye el saldo pendiente del deudor y simultáneamente alimenta la bolsa 'Dinero listo para pagar' de la tarjeta de crédito destino.", y)
y = draw_bullet(ctx, "Clasificación Efectivo vs Débito", "Permite saber con exactitud si el dinero apartado se encuentra en billetes físicos guardados en sobre o en una cuenta de débito bancaria.", y)
y = draw_bullet(ctx, "Traspaso Rápido", "Botón para mover fondos entre Efectivo y Débito cuando depositas dinero en el cajero o retiras efectivo.", y)

y = draw_callout(ctx, "El Gran Consolidado Global", "La vista de apartados presenta una tarjeta superior dorada con el 'Dinero Total Reunido' sumando el capital de todas tus tarjetas, garantizando tranquilidad financiera.", y, "success")

y = draw_h2(ctx, "Calendario Financiero y el Algoritmo del 'Día Dorado':", y)
y = draw_p(ctx, "El calendario organiza los periodos bancarios con una codificación cromática intuitiva:", y)
y = draw_bullet(ctx, "Gris Pizarra", "Día de corte bancario (cierre de facturación, sin alarmas falsas).", y)
y = draw_bullet(ctx, "Verde Esmeralda", "Periodo de pago abierto (Corte + 1 al Corte + 15).", y)
y = draw_bullet(ctx, "Rojo Alerta", "Fecha límite de pago bancario.", y)

y = draw_callout(ctx, "El Algoritmo del 'Día Dorado' (Ventana Unificada)", "CardMaster calcula la intersección matemática de los periodos de pago de todas tus tarjetas activas. Identifica el intervalo de días del mes en el que TODAS las tarjetas ya cortaron y NINGUNA ha vencido. Este día se destaca con estrellas doradas en la interfaz para liquidar todos los plásticos en una sola sesión bancaria.", y, "warning")

draw_footer(ctx, 5)
surface.show_page()

# --------------------------------------------------------------
# PÁGINA 6: DEUDORES, PRESUPUESTOS Y CORTE
# --------------------------------------------------------------
draw_header(ctx, "Deudores y Presupuestos", 6)
y = 58
y = draw_h1(ctx, "3. Funcionamiento: Deudores, Presupuestos y Corte", y)

y = draw_h2(ctx, "Módulo de Deudores y Personas:", y)
y = draw_p(ctx, "Gestor de cobranza transparente para compras compartidas con familiares o amigos:", y)
y = draw_bullet(ctx, "Balance Consolidado", "Calcula la diferencia exacta entre lo que la persona consumió en tus tarjetas y lo que efectivamente ya te ha entregado en apartados.", y)
y = draw_bullet(ctx, "Botón '+ Recibir / Apartar'", "Permite asentar entregas de dinero en un solo clic, vinculando el abono al plástico bancario correspondiente.", y)

y = draw_h2(ctx, "Módulo de Presupuestos y Proyecciones Interanuales:", y)
y = draw_bullet(ctx, "Partidas Fijas y Variables", "Permite clasificar gastos por categorías (Hogar, Servicios, Suscripciones, Negocio).", y)
y = draw_bullet(ctx, "Tasa Global de Incremento (%)", "Porcentaje de inflación o crecimiento interanual guardado de forma persistente en SQLite. Simula automáticamente el presupuesto proyectado para el siguiente año.", y)

y = draw_h2(ctx, "Ciclo de Corte Bancario y Reseteo Contable:", y)
y = draw_p(ctx, "Al liquidar tus compromisos en el banco, el botón protegido 'Resetear Tarjetas' realiza la conciliación mensual limpia:", y)
y = draw_bullet(ctx, "Limpieza de Consumos Liquidados", "Borra compras ordinarias y apartados ya aplicados, restableciendo el disponible bancario.", y)
y = draw_bullet(ctx, "Promoción de Cuotas MSI", "Avanza los planes a meses a su siguiente mensualidad (paid_months + 1) sin borrarlos.", y)
y = draw_bullet(ctx, "Protección con Contraseña", "Exige la clave maestra de la bóveda para evitar borrados accidentales.", y)

y = draw_callout(ctx, "Scroll Suave y Diseño de Botones Unificado", "Todas las pantallas cuentan con desplazamiento suave 'overflow-y: auto' para evitar que la información se corte. Todos los botones y listas desplegables respetan la estética profesional y modo oscuro/claro.", y, "info")

draw_footer(ctx, 6)
surface.show_page()

# --------------------------------------------------------------
# PÁGINA 7: DESPLIEGUE MULTIPLATAFORMA
# --------------------------------------------------------------
draw_header(ctx, "Despliegue y Soporte", 7)
y = 58
y = draw_h1(ctx, "4. Despliegue Multiplataforma y Mantenimiento", y)

y = draw_h2(ctx, "🖥️ Modo Escritorio Nativo 100% en Linux Ubuntu (desktop-ubuntu/):", y)
y = draw_bullet(ctx, "Instalación en 1 Clic", "Ejecuta './instalar-en-ubuntu.sh' para registrar el icono en tu Escritorio y menú de aplicaciones.", y)
y = draw_bullet(ctx, "Experiencia Pura de Escritorio", "Ventana GTK3 + WebKitGTK 4.1 sin pestañas ni barras externas. Al cerrar con la 'X', apaga automáticamente el servidor en segundo plano sin dejar procesos huérfanos.", y)

y = draw_h2(ctx, "📱 Las 3 Modalidades para Android (mobile-android/):", y)
y = draw_bullet(ctx, "1. PWA Directa (Recomendada)", "Inicia './iniciar-android.sh', abre la dirección en el celular e instálala en la pantalla de inicio.", y)
y = draw_bullet(ctx, "2. Autónoma en Celular (Termux)", "Copia 'mobile-android/' a tu teléfono y corre 'npm start' en Termux para usar la base de datos sin PC.", y)
y = draw_bullet(ctx, "3. Compilar APK (Capacitor)", "Usa 'capacitor.config.json' y Android Studio para compilar un paquete .apk binario nativo.", y)

y = draw_h2(ctx, "Resolución de Incidentes Frecuentes:", y)

headers_err = ["Incidente / Mensaje", "Causa Probable", "Solución Técnica Inmediata"]
rows_err = [
    ["EADDRINUSE: puerto 3000", "Servidor previo en memoria.", "Ejecuta ./detener.sh o pkill -f 'node server.js'."],
    ["Permiso denegado al correr", "Script sin bit de ejecución.", "Ejecutar: chmod +x *.sh en la carpeta respectiva."],
    ["Olvido de contraseña maestra", "Hash PBKDF2 irreversible.", "Corre ./resetear-contrasena.sh para crear nueva clave."],
    ["Migración a otra computadora", "Traslado de información.", "Copia la carpeta completa o el archivo tarjetas.db."]
]
y = draw_table(ctx, headers_err, rows_err, [160, 150, 210], y)

y = draw_callout(ctx, "Soberanía y Portabilidad Garantizada", "CardMaster fue concebido con rigor de ingeniería para garantizar privacidad perpetua y funcionamiento autónomo en cualquier equipo.", y, "success")

draw_footer(ctx, 7)
surface.show_page()

# Finalizar PDF
surface.finish()
print(f"✅ Manual PDF generado exitosamente: {OUTPUT_PDF}")
