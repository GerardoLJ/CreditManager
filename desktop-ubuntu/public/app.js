
// ==============================================================
// FORMATEO DE MONEDA CON COMAS PARA MILES Y PUNTO PARA DECIMALES
// ==============================================================
function parseMoney(val) {
  if (typeof val === 'number') return isNaN(val) ? 0 : val;
  if (!val) return 0;
  const clean = val.toString().replace(/,/g, '').trim();
  const num = parseFloat(clean);
  return isNaN(num) ? 0 : num;
}

function formatMoneyString(val) {
  if (!val) return '';
  val = val.toString().replace(/[^0-9.]/g, '');
  const parts = val.split('.');
  if (parts.length > 2) {
    parts = [parts[0], parts.slice(1).join('')];
  }
  let intPart = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  if (parts.length > 1) {
    return intPart + '.' + parts[1].slice(0, 2);
  }
  return intPart;
}

function setMoneyInput(inputEl, val) {
  if (!inputEl) return;
  if (val === null || val === undefined || val === '') {
    inputEl.value = '';
    return;
  }
  const num = parseMoney(val);
  inputEl.value = num.toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function setupMoneyInput(input) {
  if (!input || input._moneyAttached) return;
  input._moneyAttached = true;
  
  input.addEventListener('input', () => {
    const start = input.selectionStart;
    const oldVal = input.value;
    const clean = oldVal.replace(/[^0-9.]/g, '');
    const formatted = formatMoneyString(clean);
    
    const cleanBefore = oldVal.slice(0, start).replace(/[^0-9.]/g, '');
    input.value = formatted;
    
    let newPos = formatted.length;
    let seenClean = 0;
    for (let i = 0; i < formatted.length; i++) {
      if (formatted[i] !== ',') {
        seenClean++;
      }
      if (seenClean === cleanBefore.length) {
        newPos = i + 1;
        break;
      }
    }
    input.setSelectionRange(newPos, newPos);
  });

  input.addEventListener('blur', () => {
    const num = parseMoney(input.value);
    if (!isNaN(num) && input.value.trim() !== '') {
      input.value = num.toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
  });
}

function initAllMoneyInputs() {
  document.querySelectorAll('.money-input').forEach(setupMoneyInput);
}

function openTransferModal(preselectedCardId = null) {
  document.getElementById('form-transfer-fund').reset();
  const cardSelect = document.getElementById('transfer-card');
  if (preselectedCardId) {
    cardSelect.value = preselectedCardId;
  }
  updateTransferDestLabel();
  document.getElementById('modal-transfer-fund').classList.remove('hidden');
}

function updateTransferDestLabel() {
  const fromVal = document.getElementById('transfer-from-type').value;
  document.getElementById('transfer-to-type').value = fromVal === 'Efectivo' ? '💳 Débito' : '💵 Efectivo';
}

async function toggleFundLocation(setId) {
  try {
    const res = await fetch(`/api/set-asides/${setId}/toggle-fund`, { method: 'PATCH' });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error);
    showToast(`Ubicación cambiada a ${data.newFundType}`, "success");
    refreshAllData();
  } catch (err) {
    showToast(err.message || "Error al cambiar ubicación", "error");
  }
}

async function handleTransferSubmit(e) {
  e.preventDefault();
  const payload = {
    card_id: document.getElementById('transfer-card').value,
    from_type: document.getElementById('transfer-from-type').value,
    amount: parseMoney(document.getElementById('transfer-amount').value),
    note: document.getElementById('transfer-note').value.trim()
  };

  try {
    const res = await fetch('/api/set-asides/transfer', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error);

    document.getElementById('modal-transfer-fund').classList.add('hidden');
    refreshAllData();
    showToast(`Traspaso de $${data.transferred} realizado (${data.from} ➔ ${data.to})`, "success");
  } catch (err) {
    showToast(err.message || "Error al realizar traspaso", "error");
  }
}


function toggleBudgetAccordion(budgetId) {
  const body = document.getElementById(`budget-body-${budgetId}`);
  const chevron = document.getElementById(`chevron-${budgetId}`);
  if (!body) return;
  const isCollapsed = body.classList.toggle('collapsed');
  if (chevron) {
    chevron.classList.toggle('collapsed', isCollapsed);
  }
}

function setupCollapsibles() {
  const toggleBtn = document.getElementById('toggle-setasides-table');
  if (toggleBtn) {
    toggleBtn.addEventListener('click', () => {
      const wrapper = document.getElementById('setasides-table-wrapper');
      const chevron = document.getElementById('setasides-chevron');
      if (!wrapper) return;
      const isCollapsed = wrapper.classList.toggle('collapsed');
      if (chevron) chevron.classList.toggle('collapsed', isCollapsed);
    });
  }
}

/**
 * CardMaster Client Engine
 * - Consume API REST Node.js local conectada directamente a tarjetas.db
 * - Drag & Drop estilizado para logos y sincronización
 * - Gestión completa de Apartados / Fondos de Pago
 */

let currentCalYear = new Date().getFullYear();
let currentCalMonth = new Date().getMonth();
let currentCardLogoBase64 = null;

// --- INICIALIZACIÓN ---
window.addEventListener('DOMContentLoaded', async () => {
  setupTheme();
  initAllMoneyInputs();
  setupEventListeners();
  setupDragAndDrop();
  checkAuthStatus();
});

function setupTheme() {
  const saved = localStorage.getItem('theme') || 'dark';
  document.documentElement.setAttribute('data-theme', saved);
  document.getElementById('theme-icon').textContent = saved === 'dark' ? '🌙' : '☀️';

  document.getElementById('theme-toggle-btn').addEventListener('click', () => {
    const cur = document.documentElement.getAttribute('data-theme');
    const next = cur === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    document.getElementById('theme-icon').textContent = next === 'dark' ? '🌙' : '☀️';
    localStorage.setItem('theme', next);
  });
}

// --- AUTENTICACIÓN ---
async function checkAuthStatus() {
  try {
    const res = await fetch('/api/auth/status');
    const data = await res.json();
    if (!data.isConfigured) {
      document.getElementById('auth-title').textContent = "Configurar Bóveda SQLite";
      document.getElementById('auth-subtitle').textContent = "Crea tu contraseña maestra para tarjetas.db";
      document.getElementById('setup-confirm-group').classList.remove('hidden');
      document.getElementById('btn-auth-submit').textContent = "Crear Bóveda";
    }
  } catch (err) {
    showToast("Error conectando con el servidor Node.js", "error");
  }
}

async function handleAuth() {
  const pwd = document.getElementById('master-pwd').value;
  const confirm = document.getElementById('master-pwd-confirm').value;
  const isSetup = !document.getElementById('setup-confirm-group').classList.contains('hidden');
  const errDiv = document.getElementById('auth-error');
  errDiv.classList.add('hidden');

  try {
    if (isSetup) {
      if (pwd !== confirm) {
        errDiv.textContent = "Las contraseñas no coinciden";
        errDiv.classList.remove('hidden');
        return;
      }
      const res = await fetch('/api/auth/setup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password: pwd })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);
    } else {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password: pwd })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);
    }

    document.getElementById('auth-screen').classList.add('hidden');
    document.getElementById('app-container').classList.remove('hidden');
    refreshAllData();
    showToast("Bóveda SQLite desbloqueada", "success");
  } catch (err) {
    errDiv.textContent = err.message || "Error al autenticar";
    errDiv.classList.remove('hidden');
  }
}

function lockApp() {
  document.getElementById('app-container').classList.add('hidden');
  document.getElementById('auth-screen').classList.remove('hidden');
  document.getElementById('master-pwd').value = '';
  showToast("Bóveda bloqueada");
}

// --- REFRESCAR TODO EL MODELO ---
async function refreshAllData() {
  await loadInflationConfig();
  loadDbInfo();
  await Promise.all([
    renderCards(),
    renderMovements(),
    renderSetAsides(),
    renderDebts(),
    renderCalendar(),
    renderBudgets(),
    populateSelects()
  ]);
}

// --- TARJETAS ---
async function renderCards() {
  const container = document.getElementById('cards-grid');
  container.innerHTML = '';

  const [cardsRes, movsRes, setAsideRes] = await Promise.all([
    fetch('/api/cards').then(r => r.json()),
    fetch('/api/movements').then(r => r.json()),
    fetch('/api/set-asides').then(r => r.json())
  ]);

  if (!cardsRes.length) {
    container.innerHTML = `<div class="card-box" style="grid-column: 1/-1; text-align: center; color: var(--text-muted);">
      No hay tarjetas en tarjetas.db. Presiona <strong>+ Nueva Tarjeta</strong> para registrar una.
    </div>`;
    return;
  }

  // Orden personalizado persistente con Drag & Drop
  try {
    const savedOrder = JSON.parse(localStorage.getItem('cardmaster_cards_order') || '[]');
    if (Array.isArray(savedOrder) && savedOrder.length) {
      cardsRes.sort((a, b) => {
        const idxA = savedOrder.indexOf(a.id);
        const idxB = savedOrder.indexOf(b.id);
        if (idxA === -1 && idxB === -1) return 0;
        if (idxA === -1) return 1;
        if (idxB === -1) return -1;
        return idxA - idxB;
      });
    }
  } catch (err) {
    console.error("Error leyendo orden de tarjetas:", err);
  }

  for (const card of cardsRes) {
    // Calcular gastos regulares
    const cardMovs = movsRes.filter(m => m.card_id === card.id);
    const regularSpent = cardMovs.reduce((sum, m) => sum + m.amount, 0);

    // Calcular MSI
    const msiRes = await fetch(`/api/msi/${card.id}`).then(r => r.json());
    const msiPending = msiRes.reduce((sum, p) => sum + (p.total_amount - (p.total_amount / p.months * p.paid_months)), 0);

    // Calcular Apartados asociados a esta tarjeta
    const cardSetAsides = setAsideRes.filter(s => s.card_id === card.id);
    const totalSetAside = cardSetAsides.reduce((sum, s) => sum + s.amount, 0);

    const totalDebt = regularSpent + msiPending;
    const available = Math.max(0, card.credit_limit - totalDebt);
    const percent = Math.min(100, (totalDebt / card.credit_limit) * 100);

    let progressClass = '';
    if (percent > 70) progressClass = 'warning';
    if (percent > 90) progressClass = 'danger';

    const cardEl = document.createElement('div');
    cardEl.className = 'credit-card-ui';
    cardEl.dataset.cardId = card.id;
    cardEl.setAttribute('draggable', 'true');
    cardEl.style.backgroundColor = card.color || '#1e293b';

    cardEl.innerHTML = `
      <div class="card-top">
        <div style="display:flex; align-items:center; gap:8px;">
          <div class="card-bank-name">${escapeHtml(card.name)}</div>
          <span class="card-drag-indicator" title="Arrastra con el mouse para reordenar esta tarjeta">⠿ Mover</span>
        </div>
        ${card.logo_base64 ? `<img src="${card.logo_base64}" class="card-logo-img" alt="logo">` : ''}
      </div>
      <div class="card-metrics">
        <div class="metric-row">
          <span>Límite Total:</span>
          <span class="metric-val">$${card.credit_limit.toLocaleString('es-MX', {minimumFractionDigits: 2})}</span>
        </div>
        <div class="metric-row">
          <span>Disponible:</span>
          <span class="metric-val" style="color: ${available > 0 ? '#10b981' : '#ef4444'}">
            $${available.toLocaleString('es-MX', {minimumFractionDigits: 2})}
          </span>
        </div>
        <div class="metric-row">
          <span>Deuda al Banco:</span>
          <span class="metric-val">$${totalDebt.toLocaleString('es-MX', {minimumFractionDigits: 2})}</span>
        </div>
        <div class="metric-row" style="margin-top: 6px; padding-top: 4px; border-top: 1px dashed rgba(255,255,255,0.2);">
          <span style="color: #6ee7b7;">💰 Apartado / En Mano:</span>
          <span class="metric-val" style="color: #6ee7b7;">$${totalSetAside.toLocaleString('es-MX', {minimumFractionDigits: 2})}</span>
        </div>
        <div class="card-progress-bar">
          <div class="card-progress-fill ${progressClass}" style="width: ${percent}%;"></div>
        </div>
      </div>
      <div class="card-bottom">
        <span>Corte: Día ${card.cutoff_day}</span>
        <span>Ventana Pago: Días ${card.cutoff_day + 1} al ${card.cutoff_day + 15}</span>
      </div>
      <div class="card-ui-actions">
        <button class="btn btn-sm btn-secondary" onclick="openSetAsideModal(null, '${card.id}', null, '')">💰 Apartar</button>
        <button class="btn btn-sm btn-secondary" onclick="viewCardDetails('${card.id}')">📋 Detalle / MSI</button>
        <button class="btn btn-sm btn-secondary" onclick="editCard('${card.id}')">✏️</button>
        <button class="btn btn-sm btn-outline-danger" onclick="deleteCard('${card.id}')">🗑️</button>
      </div>
    `;
    container.appendChild(cardEl);
  }

  setupCardDragAndDrop();
}

function setupCardDragAndDrop() {
  const container = document.getElementById('cards-grid');
  const cards = container.querySelectorAll('.credit-card-ui');

  cards.forEach(card => {
    card.addEventListener('dragstart', (e) => {
      if (e.target.closest('button') || e.target.closest('input')) {
        e.preventDefault();
        return;
      }
      card.classList.add('dragging');
      e.dataTransfer.effectAllowed = 'move';
      e.dataTransfer.setData('text/plain', card.dataset.cardId);
    });

    card.addEventListener('dragend', () => {
      card.classList.remove('dragging');
      cards.forEach(c => c.classList.remove('drag-over'));
      saveCardsOrder();
    });

    card.addEventListener('dragover', (e) => {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move';
      const dragging = container.querySelector('.credit-card-ui.dragging');
      if (dragging && dragging !== card) {
        const rect = card.getBoundingClientRect();
        const midX = rect.left + rect.width / 2;
        if (e.clientX < midX) {
          container.insertBefore(dragging, card);
        } else {
          container.insertBefore(dragging, card.nextSibling);
        }
      }
    });

    card.addEventListener('dragenter', (e) => {
      e.preventDefault();
      if (!card.classList.contains('dragging')) {
        card.classList.add('drag-over');
      }
    });

    card.addEventListener('dragleave', () => {
      card.classList.remove('drag-over');
    });

    card.addEventListener('drop', (e) => {
      e.preventDefault();
      card.classList.remove('drag-over');
      saveCardsOrder();
    });
  });
}

function saveCardsOrder() {
  const container = document.getElementById('cards-grid');
  const cardEls = container.querySelectorAll('.credit-card-ui');
  const ids = Array.from(cardEls).map(el => el.dataset.cardId).filter(Boolean);
  localStorage.setItem('cardmaster_cards_order', JSON.stringify(ids));
}

async function handleSaveCard(e) {
  e.preventDefault();
  const id = document.getElementById('card-id').value;
  const name = document.getElementById('card-name').value.trim();
  const credit_limit = parseMoney(document.getElementById('card-limit').value);
  const cutoff_day = parseInt(document.getElementById('card-cutoff-day').value);
  const color = document.getElementById('card-color').value;

  await fetch('/api/cards', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id, name, credit_limit, cutoff_day, color, logo_base64: currentCardLogoBase64 })
  });

  document.getElementById('modal-card').classList.add('hidden');
  refreshAllData();
  showToast("Tarjeta guardada en tarjetas.db", "success");
}

async function editCard(cardId) {
  const cards = await fetch('/api/cards').then(r => r.json());
  const c = cards.find(x => x.id === cardId);
  if (!c) return;

  document.getElementById('modal-card-title').textContent = "Editar Tarjeta";
  document.getElementById('card-id').value = c.id;
  document.getElementById('card-name').value = c.name;
  setMoneyInput(document.getElementById('card-limit'), c.credit_limit);
  document.getElementById('card-cutoff-day').value = c.cutoff_day;
  document.getElementById('card-color').value = c.color || '#1e293b';
  currentCardLogoBase64 = c.logo_base64;
  updateLogoPreview(c.logo_base64);

  document.getElementById('modal-card').classList.remove('hidden');
}

function deleteCard(cardId) {
  document.getElementById('delete-card-id').value = cardId;
  document.getElementById('delete-card-pwd').value = '';
  document.getElementById('delete-card-error').classList.add('hidden');
  document.getElementById('modal-delete-card').classList.remove('hidden');
}

async function handleConfirmDeleteCard(e) {
  e.preventDefault();
  const cardId = document.getElementById('delete-card-id').value;
  const password = document.getElementById('delete-card-pwd').value;
  const errDiv = document.getElementById('delete-card-error');
  errDiv.classList.add('hidden');

  try {
    const res = await fetch(`/api/cards/${cardId}/delete`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ password })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error);

    document.getElementById('modal-delete-card').classList.add('hidden');
    refreshAllData();
    showToast("Tarjeta eliminada con éxito", "success");
  } catch (err) {
    errDiv.textContent = err.message || "Error al eliminar";
    errDiv.classList.remove('hidden');
  }
}

// --- APARTADOS (LÓGICA SOLICITADA) ---
async function renderSetAsides() {
  const [cards, setAsides] = await Promise.all([
    fetch('/api/cards').then(r => r.json()),
    fetch('/api/set-asides').then(r => r.json())
  ]);

  const summaryGrid = document.getElementById('setaside-summary-cards');
  summaryGrid.innerHTML = '';

  // Calcular consolidado global
  let globalEfectivo = 0;
  let globalDebito = 0;

  cards.forEach(card => {
    const cardSetAsides = setAsides.filter(s => s.card_id === card.id);
    const totalCash = cardSetAsides.filter(s => s.fund_type !== 'Débito' && s.fund_type !== 'Debito').reduce((sum, s) => sum + s.amount, 0);
    const totalDebit = cardSetAsides.filter(s => s.fund_type === 'Débito' || s.fund_type === 'Debito').reduce((sum, s) => sum + s.amount, 0);
    const cardTotal = totalCash + totalDebit;

    globalEfectivo += totalCash;
    globalDebito += totalDebit;

    const box = document.createElement('div');
    box.className = 'setaside-card';
    box.innerHTML = `
      <div class="setaside-header">
        <strong>${escapeHtml(card.name)}</strong>
        <span class="countdown-badge success">Total: $${cardTotal.toLocaleString('es-MX', {minimumFractionDigits: 2})}</span>
      </div>
      <div style="margin: 12px 0;">
        <div class="setaside-breakdown-row">
          <span>💵 Apartado en Efectivo:</span>
          <strong style="color: #34d399;">$${totalCash.toLocaleString('es-MX', {minimumFractionDigits: 2})}</strong>
        </div>
        <div class="setaside-breakdown-row">
          <span>💳 Apartado en Débito:</span>
          <strong style="color: #60a5fa;">$${totalDebit.toLocaleString('es-MX', {minimumFractionDigits: 2})}</strong>
        </div>
      </div>
      <div style="margin-top: 10px; padding-top: 8px; border-top: 1px solid var(--border-color); display:flex; justify-content:space-between; align-items:center;">
        <span class="small text-muted">Consolidado Tarjeta:</span>
        <span class="setaside-amount-highlight" style="font-size:1.25rem;">$${cardTotal.toLocaleString('es-MX', {minimumFractionDigits: 2})}</span>
      </div>
      <div style="display:flex; justify-content:flex-end; margin-top:8px;">
        <button class="btn btn-sm btn-secondary" onclick="openTransferModal('${card.id}')">🔄 Mover Efectivo ⇄ Débito</button>
      </div>
    `;
    summaryGrid.appendChild(box);
  });

  // Agregar tarjeta con el Gran Consolidado Global si hay tarjetas
  if (cards.length > 0) {
    const globalTotal = globalEfectivo + globalDebito;
    const globalBox = document.createElement('div');
    globalBox.className = 'setaside-card';
    globalBox.style.borderColor = 'var(--primary-color)';
    globalBox.innerHTML = `
      <div class="setaside-header">
        <strong style="color: var(--primary-color);">🌟 CONSOLIDADO GLOBAL</strong>
        <span class="countdown-badge normal">Todas las cuentas</span>
      </div>
      <div style="margin: 12px 0;">
        <div class="setaside-breakdown-row">
          <span>💵 Total en Efectivo:</span>
          <strong style="color: #34d399; font-size:1rem;">$${globalEfectivo.toLocaleString('es-MX', {minimumFractionDigits: 2})}</strong>
        </div>
        <div class="setaside-breakdown-row">
          <span>💳 Total en Débito:</span>
          <strong style="color: #60a5fa; font-size:1rem;">$${globalDebito.toLocaleString('es-MX', {minimumFractionDigits: 2})}</strong>
        </div>
      </div>
      <div style="margin-top: 10px; padding-top: 8px; border-top: 1px solid var(--border-color); display:flex; justify-content:space-between; align-items:center;">
        <span class="small text-muted">Dinero Total Reunido:</span>
        <span class="setaside-amount-highlight">$${globalTotal.toLocaleString('es-MX', {minimumFractionDigits: 2})}</span>
      </div>
    `;
    summaryGrid.prepend(globalBox);
  }

  const tbody = document.getElementById('setaside-tbody');
  tbody.innerHTML = '';

  if (!setAsides.length) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--text-muted);">Aún no tienes apartados registrados</td></tr>`;
    return;
  }

  setAsides.forEach(s => {
    const isDebit = s.fund_type === 'Débito' || s.fund_type === 'Debito';
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${s.date}</td>
      <td><strong>${escapeHtml(s.card_name)}</strong></td>
      <td><span class="countdown-badge normal">${escapeHtml(s.person_name)}</span></td>
      <td>
        <button class="badge-fund ${isDebit ? 'debito' : 'efectivo'}" style="cursor:pointer; border:none;" onclick="toggleFundLocation('${s.id}')" title="Clic para cambiar entre Efectivo y Débito">
          ${isDebit ? '💳 Débito ⇄' : '💵 Efectivo ⇄'}
        </button>
      </td>
      <td style="font-weight:700; color:${s.amount < 0 ? 'var(--danger-color)' : 'var(--success-color)'};">
        $${s.amount.toLocaleString('es-MX', {minimumFractionDigits: 2})}
      </td>
      <td>${escapeHtml(s.note || 'Apartado ordinario')}</td>
    `;
    tbody.appendChild(tr);
  });
}

function openSetAsideModal(movId = null, cardId = null, personId = null, defaultAmount = '') {
  document.getElementById('form-setaside').reset();
  document.getElementById('setaside-date').value = new Date().toISOString().split('T')[0];
  document.getElementById('setaside-movement-id').value = movId || '';

  if (cardId) document.getElementById('setaside-card').value = cardId;
  if (personId) document.getElementById('setaside-person').value = personId;
  if (defaultAmount) setMoneyInput(document.getElementById('setaside-amount'), defaultAmount);

  document.getElementById('modal-setaside').classList.remove('hidden');
}

async function handleSaveSetAside(e) {
  e.preventDefault();
  const payload = {
    card_id: document.getElementById('setaside-card').value,
    person_id: document.getElementById('setaside-person').value,
    movement_id: document.getElementById('setaside-movement-id').value || null,
    amount: parseMoney(document.getElementById('setaside-amount').value),
    fund_type: document.getElementById('setaside-fund-type').value,
    note: document.getElementById('setaside-note').value.trim(),
    date: document.getElementById('setaside-date').value
  };

  await fetch('/api/set-asides', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });

  document.getElementById('modal-setaside').classList.add('hidden');
  refreshAllData();
  showToast("Dinero apartado registrado con éxito", "success");
}

// --- MOVIMIENTOS ---
async function renderMovements() {
  const movs = await fetch('/api/movements').then(r => r.json());
  const cardFilter = document.getElementById('filter-card').value;
  const personFilter = document.getElementById('filter-person').value;
  const tbody = document.getElementById('movements-tbody');
  tbody.innerHTML = '';

  const filtered = movs.filter(m => {
    if (cardFilter !== 'all' && m.card_id !== cardFilter) return false;
    if (personFilter !== 'all' && m.person_id !== personFilter) return false;
    return true;
  });

  if (!filtered.length) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">Sin gastos registrados</td></tr>`;
    return;
  }

  filtered.forEach(m => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${m.date}</td>
      <td><strong>${escapeHtml(m.concept)}</strong></td>
      <td>${escapeHtml(m.card_name)}</td>
      <td><span class="countdown-badge normal">${escapeHtml(m.person_name)}</span></td>
      <td style="font-weight: 700;">$${m.amount.toLocaleString('es-MX', {minimumFractionDigits: 2})}</td>
      <td>
        ${m.is_set_aside 
          ? `<span class="countdown-badge success">✓ Dinero en Mano</span>` 
          : `<button class="btn btn-sm btn-secondary" onclick="openSetAsideModal('${m.id}', '${m.card_id}', '${m.person_id}', '${m.amount}')">💰 Apartar</button>`
        }
      </td>
      <td>
        <div style="display:flex; gap:6px;">
          <button class="btn btn-sm btn-secondary" onclick="openEditMovement('${m.id}')" title="Editar Gasto">✏️</button>
          <button class="btn btn-sm btn-outline-danger" onclick="deleteMovement('${m.id}')" title="Eliminar Gasto">🗑️</button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

async function openEditMovement(id) {
  const movs = await fetch('/api/movements').then(r => r.json());
  const m = movs.find(x => x.id === id);
  if (!m) return;

  document.getElementById('modal-movement-title').textContent = "Editar Gasto";
  const submitBtn = document.getElementById('btn-save-movement-submit');
  if (submitBtn) submitBtn.textContent = "Guardar Cambios";

  document.getElementById('mov-id').value = m.id;
  document.getElementById('mov-concept').value = m.concept;
  setMoneyInput(document.getElementById('mov-amount'), m.amount);
  document.getElementById('mov-date').value = m.date;
  document.getElementById('mov-card').value = m.card_id;
  document.getElementById('mov-person').value = m.person_id;

  const msiContainer = document.getElementById('mov-msi-container');
  if (msiContainer) msiContainer.classList.add('hidden');
  document.getElementById('msi-extra-fields').classList.add('hidden');
  document.getElementById('mov-is-msi').checked = false;

  document.getElementById('modal-movement').classList.remove('hidden');
}

async function handleSaveMovement(e) {
  e.preventDefault();
  const movId = document.getElementById('mov-id').value;
  const payload = {
    concept: document.getElementById('mov-concept').value.trim(),
    amount: parseMoney(document.getElementById('mov-amount').value),
    date: document.getElementById('mov-date').value,
    card_id: document.getElementById('mov-card').value,
    person_id: document.getElementById('mov-person').value,
    is_msi: document.getElementById('mov-is-msi').checked,
    msi_months: parseInt(document.getElementById('mov-msi-months').value)
  };

  if (movId) {
    await fetch(`/api/movements/${movId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    showToast("Gasto actualizado con éxito", "success");
  } else {
    await fetch('/api/movements', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    showToast("Gasto guardado en base de datos", "success");
  }

  document.getElementById('modal-movement').classList.add('hidden');
  refreshAllData();
}

async function deleteMovement(id) {
  await fetch(`/api/movements/${id}`, { method: 'DELETE' });
  refreshAllData();
  showToast("Movimiento eliminado");
}

// --- DEUDORES (RECONCILIACIÓN DE APARTADOS) ---
async function renderDebts() {
  const [people, cards, movs, setAsides] = await Promise.all([
    fetch('/api/people').then(r => r.json()),
    fetch('/api/cards').then(r => r.json()),
    fetch('/api/movements').then(r => r.json()),
    fetch('/api/set-asides').then(r => r.json())
  ]);

  const container = document.getElementById('debts-summary-container');
  container.innerHTML = '';

  people.forEach(p => {
    const personMovs = movs.filter(m => m.person_id === p.id);
    const personSetAsides = setAsides.filter(s => s.person_id === p.id);

    const totalSpent = personMovs.reduce((sum, m) => sum + m.amount, 0);
    const totalApartado = personSetAsides.reduce((sum, s) => sum + s.amount, 0);
    const pendingToCollect = Math.max(0, totalSpent - totalApartado);

    const cardBreakdown = [];
    cards.forEach(c => {
      const cMovs = personMovs.filter(m => m.card_id === c.id).reduce((sum, m) => sum + m.amount, 0);
      const cSetAside = personSetAsides.filter(s => s.card_id === c.id).reduce((sum, s) => sum + s.amount, 0);
      const cPending = Math.max(0, cMovs - cSetAside);
      if (cMovs > 0) {
        cardBreakdown.push({ name: c.name, spent: cMovs, apartado: cSetAside, pending: cPending });
      }
    });

    const box = document.createElement('div');
    box.className = 'debt-card';
    box.innerHTML = `
      <div class="debt-header">
        <div>
          <h3>${escapeHtml(p.name)}</h3>
          <span class="small text-muted">${p.name === 'Personal' ? 'Tus gastos propios' : 'A cobrar'}</span>
        </div>
        <div style="text-align: right;">
          <div class="debt-total-amount">$${pendingToCollect.toLocaleString('es-MX', {minimumFractionDigits: 2})}</div>
          <span class="small text-muted">Falta cobrar</span>
        </div>
      </div>
      <div class="small" style="margin-bottom: 8px;">
        <span>Total consumido: $${totalSpent.toLocaleString('es-MX')}</span> | 
        <span style="color:var(--success-color);">Ya entregado: $${totalApartado.toLocaleString('es-MX')}</span>
      </div>
      <strong>Desglose por Tarjetas:</strong>
      <ul class="debt-breakdown-list">
        ${cardBreakdown.length > 0 ? cardBreakdown.map(b => `
          <li>
            <span>${escapeHtml(b.name)}</span>
            <span>$${b.spent.toLocaleString()} (Apartado: $${b.apartado.toLocaleString()}) &rarr; <strong>$${b.pending.toLocaleString()}</strong></span>
          </li>
        `).join('') : '<li class="text-muted">Sin adeudos pendientes</li>'}
      </ul>
      <div style="display:flex; justify-content: flex-end; gap:8px; margin-top: 12px;">
        <button class="btn btn-sm btn-secondary" onclick="openSetAsideModal(null, null, '${p.id}', '${pendingToCollect}')">💰 + Recibir / Apartar</button>
        ${p.name !== 'Personal' ? `<button class="btn btn-sm btn-outline-danger" onclick="deletePerson('${p.id}')">Eliminar</button>` : ''}
      </div>
    `;
    container.appendChild(box);
  });
}

async function deletePerson(pId) {
  if (!confirm("¿Eliminar persona?")) return;
  const res = await fetch(`/api/people/${pId}`, { method: 'DELETE' });
  const data = await res.json();
  if (!res.ok) alert(data.error);
  refreshAllData();
}

// --- CALENDARIO ---
async function renderCalendar() {
  const cards = await fetch('/api/cards').then(r => r.json());
  const summaryBox = document.getElementById('calendar-cards-summary');
  summaryBox.innerHTML = '';

  // Calcular Ventana Óptima de Pago Unificado (Convergencia)
  let convergenceStart = null;
  let convergenceEnd = null;
  let hasValidConvergence = false;

  if (cards.length > 0) {
    // Para cada tarjeta: ventana [cutoff + 1, cutoff + 15]
    const startDays = cards.map(c => c.cutoff_day + 1);
    const endDays = cards.map(c => c.cutoff_day + 15);

    convergenceStart = Math.max(...startDays);
    convergenceEnd = Math.min(...endDays);
    hasValidConvergence = convergenceStart <= convergenceEnd;
  }

  // Renderizar Banner de Pago Unificado
  const existingConvergenceBox = document.getElementById('convergence-box');
  if (existingConvergenceBox) existingConvergenceBox.remove();

  if (cards.length >= 2) {
    const convBox = document.createElement('div');
    convBox.id = 'convergence-box';
    if (hasValidConvergence) {
      convBox.className = 'convergence-card';
      convBox.innerHTML = `
        <div>
          <h3 style="display:flex; align-items:center; gap:8px;">🎯 Ventana Óptima de Pago Unificado</h3>
          <p class="small text-muted" style="margin-top:4px;">
            En este rango de días todas tus tarjetas ya pasaron corte y siguen en periodo vigente de pago.
          </p>
          <div class="convergence-badge">
            Días ${convergenceStart} al ${convergenceEnd} de cada mes
          </div>
        </div>
        <div style="text-align: right;">
          <span class="small text-muted">Día ideal para liquidar todas juntas:</span>
          <div style="font-size: 1.3rem; font-weight:800; color:#3b82f6; margin-top:4px;">
            Día ${convergenceStart}
          </div>
        </div>
      `;
    } else {
      convBox.className = 'convergence-card no-overlap';
      convBox.innerHTML = `
        <div>
          <h3 style="color:#f59e0b;">⚠️ Sin convergencia unificada directa para todas las tarjetas</h3>
          <p class="small text-muted" style="margin-top:4px;">
            Las fechas de corte de tus tarjetas están muy dispersas entre sí. La última tarjeta en cortar inicia pago el día <strong>${convergenceStart}</strong>, mientras que la primera vence el día <strong>${convergenceEnd}</strong>.
          </p>
        </div>
      `;
    }
    summaryBox.parentNode.insertBefore(convBox, summaryBox);
  }

  const today = new Date();
  const currentDay = today.getDate();

  cards.forEach(card => {
    let daysToCutoff = card.cutoff_day - currentDay;
    if (daysToCutoff < 0) {
      const lastDayThisMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0).getDate();
      daysToCutoff = (lastDayThisMonth - currentDay) + card.cutoff_day;
    }

    const cardEl = document.createElement('div');
    cardEl.className = 'cal-summary-card';
    cardEl.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <strong>${escapeHtml(card.name)}</strong>
        <span class="countdown-badge ${daysToCutoff <= 3 ? 'urgent' : 'normal'}">
          ${daysToCutoff === 0 ? '¡Corte HOY!' : `Corte en ${daysToCutoff} días`}
        </span>
      </div>
      <div class="small text-muted">Día de corte: <strong>${card.cutoff_day}</strong></div>
      <div class="small">Ventana de pago: <strong>Día ${card.cutoff_day + 1} al ${card.cutoff_day + 15}</strong></div>
    `;
    summaryBox.appendChild(cardEl);
  });

  const monthNames = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"];
  document.getElementById('cal-month-title').textContent = `${monthNames[currentCalMonth]} ${currentCalYear}`;

  const daysGrid = document.getElementById('calendar-days-grid');
  daysGrid.innerHTML = '';

  const firstDay = new Date(currentCalYear, currentCalMonth, 1).getDay();
  const totalDays = new Date(currentCalYear, currentCalMonth + 1, 0).getDate();

  for (let i = 0; i < firstDay; i++) {
    const el = document.createElement('div');
    el.className = 'cal-day-cell';
    el.style.background = 'transparent';
    daysGrid.appendChild(el);
  }

  for (let day = 1; day <= totalDays; day++) {
    const cell = document.createElement('div');
    cell.className = 'cal-day-cell';
    if (day === today.getDate() && currentCalMonth === today.getMonth() && currentCalYear === today.getFullYear()) {
      cell.classList.add('today');
    }

    if (hasValidConvergence && day >= convergenceStart && day <= convergenceEnd) {
      cell.classList.add('golden-day');
      cell.title = "Día dentro de la ventana de pago unificado (todas las tarjetas vigentes)";
    }

    cell.innerHTML = `<strong>${day}</strong>`;

    cards.forEach(c => {
      if (day === c.cutoff_day) {
        cell.innerHTML += `<div class="cal-event-pill corte" title="Día de corte">✂️ ${escapeHtml(c.name)}</div>`;
      }
      if (day === (c.cutoff_day + 1)) {
        cell.innerHTML += `<div class="cal-event-pill pago-inicio" title="Inicio de ventana de pago">💰 Pago ${escapeHtml(c.name)}</div>`;
      }
      if (day === (c.cutoff_day + 15)) {
        cell.innerHTML += `<div class="cal-event-pill pago-limite" title="Último día de pago (Día 15)">⚠️ Límite ${escapeHtml(c.name)}</div>`;
      }
    });

    daysGrid.appendChild(cell);
  }
}

// --- PRESUPUESTOS ---

async function loadInflationConfig() {
  let rate = 6.0;
  try {
    const res = await fetch('/api/config/inflation');
    if (res.ok) {
      const data = await res.json();
      if (data && data.inflationRate !== undefined) {
        rate = data.inflationRate;
      }
    } else {
      const cached = localStorage.getItem('cardmaster_inflation_rate');
      if (cached) rate = parseFloat(cached);
    }
  } catch (err) {
    const cached = localStorage.getItem('cardmaster_inflation_rate');
    if (cached) rate = parseFloat(cached);
  }
  document.getElementById('global-inflation-input').value = rate;
  return rate;
}

async function handleSaveInflation() {
  const inputEl = document.getElementById('global-inflation-input');
  const inputVal = parseFloat(inputEl.value);
  const rate = isNaN(inputVal) ? 6.0 : inputVal;

  // Guardado preventivo inmediato en almacenamiento local para no perder el dato
  localStorage.setItem('cardmaster_inflation_rate', String(rate));

  try {
    const res = await fetch('/api/config/inflation', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rate })
    });
    
    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      throw new Error(errData.error || `Error del servidor (${res.status})`);
    }

    const data = await res.json();
    showToast(`Tasa interanual fijada con éxito: ${data.inflationRate}%`, "success");
  } catch (err) {
    // Si no se ha reiniciado el server.js aún con node server.js, le avisamos de forma clara y amigable
    showToast(`Guardado localmente (${rate}%). Reinicia 'node server.js' para sincronizar con tarjetas.db`, "warning");
  }

  await renderBudgets();
}

async function renderBudgets() {
  const { budgets, items } = await fetch('/api/budgets').then(r => r.json());
  const container = document.getElementById('budgets-accordion');
  container.innerHTML = '';

  const rate = (parseFloat(document.getElementById('global-inflation-input').value) || 6.0) / 100;
  const currentYear = new Date().getFullYear();
  const nextYear = currentYear + 1;

  if (!budgets.length) {
    container.innerHTML = `<div class="card-box" style="text-align: center; color: var(--text-muted);">Sin presupuestos registrados</div>`;
    return;
  }

  budgets.forEach(b => {
    const bItems = items.filter(i => i.budget_id === b.id);
    const totalCurrentMonthly = bItems.reduce((sum, i) => sum + i.amount, 0);
    const totalNextMonthly = totalCurrentMonthly * (1 + rate);

    const box = document.createElement('div');
    box.className = 'budget-accordion-item';
    box.innerHTML = `
      <div class="budget-acc-header" onclick="toggleBudgetAccordion('${b.id}')">
        <div style="display:flex; align-items:center; gap:10px;">
          <span id="chevron-${b.id}" class="accordion-chevron">▼</span>
          <div>
            <h3>${escapeHtml(b.name)}</h3>
            <p class="small text-muted">${escapeHtml(b.description || 'Clic en la fila para colapsar / mostrar tabla')}</p>
          </div>
        </div>
        <div style="text-align: right;">
          <div><strong>Mensual (${currentYear}):</strong> $${totalCurrentMonthly.toLocaleString('es-MX', {minimumFractionDigits:2})}</div>
          <div style="color: var(--primary-color);"><strong>Proy. (${nextYear} +${(rate*100).toFixed(1)}%):</strong> $${totalNextMonthly.toLocaleString('es-MX', {minimumFractionDigits:2})}</div>
        </div>
      </div>
      <div class="budget-acc-body collapsible-content" id="budget-body-${b.id}">
        <div style="display:flex; justify-content:space-between; margin-bottom: 12px;">
          <span><strong>Total Anual:</strong> $${(totalCurrentMonthly*12).toLocaleString()} &rarr; <strong>Proy. Anual:</strong> $${(totalNextMonthly*12).toLocaleString()}</span>
          <div>
            <button class="btn btn-sm btn-primary" onclick="openBudgetItemModal('${b.id}')">+ Concepto</button>
            <button class="btn btn-sm btn-outline-danger" onclick="deleteBudget('${b.id}')">🗑️ Borrar</button>
          </div>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Concepto</th>
                <th>Subetiqueta</th>
                <th>Tipo</th>
                <th>${currentYear}</th>
                <th>Proy. ${nextYear}</th>
                <th>Acción</th>
              </tr>
            </thead>
            <tbody>
              ${bItems.length > 0 ? bItems.map(i => `
                <tr>
                  <td><strong>${escapeHtml(i.concept)}</strong></td>
                  <td><span class="countdown-badge normal">${escapeHtml(i.tag)}</span></td>
                  <td>${i.type}</td>
                  <td>$${i.amount.toLocaleString()}</td>
                  <td style="color:var(--primary-color); font-weight:700;">$${(i.amount*(1+rate)).toLocaleString()}</td>
                  <td><button class="btn btn-sm btn-outline-danger" onclick="deleteBudgetItem('${i.id}')">🗑️</button></td>
                </tr>
              `).join('') : `<tr><td colspan="6" style="text-align:center; color:var(--text-muted);">Sin conceptos</td></tr>`}
            </tbody>
          </table>
        </div>
      </div>
    `;
    container.appendChild(box);
  });
}

// --- POPULATE SELECTS ---
async function populateSelects() {
  const [cards, people] = await Promise.all([
    fetch('/api/cards').then(r => r.json()),
    fetch('/api/people').then(r => r.json())
  ]);

  const populate = (id, items) => {
    const el = document.getElementById(id);
    if (!el) return;
    el.innerHTML = '';
    items.forEach(i => el.innerHTML += `<option value="${i.id}">${escapeHtml(i.name)}</option>`);
  };

  populate('mov-card', cards);
  populate('setaside-card', cards);
  populate('transfer-card', cards);
  populate('mov-person', people);
  populate('setaside-person', people);

  const filterCard = document.getElementById('filter-card');
  const filterPerson = document.getElementById('filter-person');
  filterCard.innerHTML = `<option value="all">Todas las tarjetas</option>`;
  filterPerson.innerHTML = `<option value="all">Todas las personas</option>`;
  cards.forEach(c => filterCard.innerHTML += `<option value="${c.id}">${escapeHtml(c.name)}</option>`);
  people.forEach(p => filterPerson.innerHTML += `<option value="${p.id}">${escapeHtml(p.name)}</option>`);
}

// --- RESETEO DE TARJETAS ---
async function handleReset(e) {
  e.preventDefault();
  const pwd = document.getElementById('reset-pwd').value;
  const errDiv = document.getElementById('reset-error');
  errDiv.classList.add('hidden');

  const res = await fetch('/api/cards/reset', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password: pwd })
  });
  const data = await res.json();
  if (!res.ok) {
    errDiv.textContent = data.error;
    errDiv.classList.remove('hidden');
    return;
  }

  document.getElementById('modal-reset').classList.add('hidden');
  refreshAllData();
  showToast("Tarjetas reseteadas a ceros. MSI preservados", "success");
}

// --- SINCRONIZACIÓN FUSIÓN INCREMENTAL CON CELULAR ---
async function handleExportSync() {
  window.location.href = '/api/sync/export';
}

async function handleImportSync(file) {
  const reader = new FileReader();
  reader.onload = async () => {
    try {
      const json = JSON.parse(reader.result);
      const res = await fetch('/api/sync/import', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(json)
      });
      const data = await res.json();
      showToast(`Fusión completada: ${data.processed} registros actualizados en SQLite`, "success");
      refreshAllData();
    } catch (err) {
      alert("Error procesando archivo de sincronización JSON");
    }
  };
  reader.readAsText(file);
}

// --- GESTIÓN FÍSICA DE BASE DE DATOS LOCAL (tarjetas.db) ---
async function loadDbInfo() {
  try {
    const res = await fetch('/api/database/info');
    if (!res.ok) return;
    const info = await res.json();
    const pathEl = document.getElementById('db-active-path');
    const sizeEl = document.getElementById('db-active-size');
    if (pathEl) {
      pathEl.textContent = info.dbPath;
      pathEl.title = `Ruta física: ${info.dbPath}`;
    }
    if (sizeEl) {
      sizeEl.textContent = `${info.sizeFormatted} (${info.exists ? 'Conectado' : 'No encontrado'})`;
    }
  } catch (err) {
    console.warn("No se pudo obtener información de la base de datos:", err);
  }
}

async function saveDatabaseToLocalDevice() {
  try {
    showToast("Preparando copia de tarjetas.db...", "info");
    const res = await fetch('/api/database/download');
    if (!res.ok) throw new Error("No se pudo descargar la base de datos desde el servidor");
    const blob = await res.blob();

    // Intentar abrir el selector nativo del sistema operativo ("Guardar como") si está soportado
    if (window.showSaveFilePicker) {
      try {
        const handle = await window.showSaveFilePicker({
          suggestedName: 'tarjetas.db',
          types: [{
            description: 'Base de Datos SQLite (*.db)',
            accept: { 'application/x-sqlite3': ['.db', '.sqlite'] }
          }]
        });
        const writable = await handle.createWritable();
        await writable.write(blob);
        await writable.close();
        showToast("✅ Base de datos guardada en la carpeta seleccionada", "success");
        return;
      } catch (pickerErr) {
        if (pickerErr.name === 'AbortError') {
          return; // El usuario canceló
        }
        console.warn("showSaveFilePicker cancelado o no disponible:", pickerErr);
      }
    }

    // Descarga directa tradicional para cualquier navegador / dispositivo
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'tarjetas.db';
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    a.remove();
    showToast("💾 Descargando tarjetas.db a tu dispositivo...", "success");
  } catch (err) {
    alert("Error al guardar base de datos: " + err.message);
  }
}

async function handleRestoreDb(file) {
  if (!file) return;
  const isDb = file.name.endsWith('.db') || file.name.endsWith('.sqlite') || file.name.endsWith('.sqlite3') || file.name.endsWith('.bd');
  if (!isDb) {
    alert("Por favor selecciona un archivo de base de datos válido (.db o .sqlite)");
    return;
  }

  showToast(`Cargando y sobreescribiendo con "${file.name}"...`, "info");
  const reader = new FileReader();
  reader.onload = async () => {
    try {
      const base64 = reader.result;
      const res = await fetch('/api/database/restore', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ dbBase64: base64 })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Error al restaurar");
      showToast("✅ Base de datos sobreescrita con éxito", "success");
      await checkAuthStatus();
      const errDiv = document.getElementById('auth-error');
      if (errDiv) errDiv.classList.add('hidden');
      await loadDbInfo();
      refreshAllData();
      const fileInput = document.getElementById('db-file-input');
      if (fileInput) fileInput.value = '';
      const authFileInput = document.getElementById('auth-db-file-input');
      if (authFileInput) authFileInput.value = '';
    } catch (err) {
      alert("Error restaurando base de datos: " + err.message);
    }
  };
  reader.readAsDataURL(file);
}

// --- DETALLES DE TARJETA & MSI ---
async function viewCardDetails(cardId) {
  const [cards, msiPlans] = await Promise.all([
    fetch('/api/cards').then(r => r.json()),
    fetch(`/api/msi/${cardId}`).then(r => r.json())
  ]);
  const card = cards.find(c => c.id === cardId);
  if (!card) return;

  document.getElementById('card-detail-title').textContent = `Detalle: ${card.name}`;
  const tbody = document.getElementById('card-msi-tbody');
  tbody.innerHTML = '';

  if (!msiPlans.length) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:var(--text-muted);">Sin planes a Meses Sin Intereses</td></tr>`;
  } else {
    msiPlans.forEach(p => {
      const monthly = p.total_amount / p.months;
      const remaining = Math.max(0, p.total_amount - (monthly * p.paid_months));
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><strong>${escapeHtml(p.concept)}</strong></td>
        <td>${p.start_date}</td>
        <td>Fin +${p.months}m</td>
        <td>$${p.total_amount.toLocaleString()}</td>
        <td>$${monthly.toFixed(2)}</td>
        <td>${p.paid_months} de ${p.months} meses</td>
        <td style="color:${remaining>0?'var(--warning-color)':'var(--success-color)'}; font-weight:700;">$${remaining.toLocaleString()}</td>
        <td>
          <button class="btn btn-sm btn-secondary" onclick="advanceMsi('${p.id}', '${cardId}')">+1 Mes</button>
          <button class="btn btn-sm btn-outline-danger" onclick="deleteMsi('${p.id}', '${cardId}')">🗑️</button>
        </td>
      `;
      tbody.appendChild(tr);
    });
  }

  document.getElementById('modal-card-details').classList.remove('hidden');
}

async function advanceMsi(id, cardId) {
  await fetch(`/api/msi/${id}/advance`, { method: 'POST' });
  viewCardDetails(cardId);
  renderCards();
}

async function deleteMsi(id, cardId) {
  if (!confirm("¿Eliminar plan MSI?")) return;
  await fetch(`/api/msi/${id}`, { method: 'DELETE' });
  viewCardDetails(cardId);
  renderCards();
}

async function handleChangePassword() {
  const oldPassword = document.getElementById('old-pwd').value;
  const newPassword = document.getElementById('new-pwd').value;
  const newPasswordConfirm = document.getElementById('new-pwd-confirm').value;

  if (!oldPassword || !newPassword) {
    showToast("Por favor completa los campos de contraseña", "error");
    return;
  }

  if (newPassword !== newPasswordConfirm) {
    showToast("La nueva contraseña y su confirmación no coinciden", "error");
    return;
  }

  if (newPassword.length < 4) {
    showToast("La nueva contraseña debe tener al menos 4 caracteres", "error");
    return;
  }

  try {
    const res = await fetch('/api/auth/change-pwd', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ oldPassword, newPassword })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error);

    document.getElementById('old-pwd').value = '';
    document.getElementById('new-pwd').value = '';
    document.getElementById('new-pwd-confirm').value = '';
    showToast("Contraseña maestra actualizada exitosamente", "success");
  } catch (err) {
    showToast(err.message || "Error al cambiar contraseña", "error");
  }
}

// --- SETUP LISTENERS Y DRAG & DROP ---
function setupEventListeners() {
  setupCollapsibles();
  document.getElementById('auth-form').addEventListener('submit', handleAuth);
  document.getElementById('lock-btn').addEventListener('click', lockApp);

  // Control de Sidebar / Drawer
  const menuToggleBtn = document.getElementById('menu-toggle-btn');
  const drawerOverlay = document.getElementById('drawer-overlay');
  const appSidebar = document.getElementById('app-sidebar');
  const sidebarCloseBtn = document.getElementById('sidebar-close-btn');
  const topbarViewTitle = document.getElementById('topbar-view-title');

  function openSidebar() {
    if (drawerOverlay && appSidebar) {
      drawerOverlay.classList.add('active');
      appSidebar.classList.add('active');
      document.body.style.overflow = 'hidden';
    }
  }

  function closeSidebar() {
    if (drawerOverlay && appSidebar) {
      drawerOverlay.classList.remove('active');
      appSidebar.classList.remove('active');
      document.body.style.overflow = '';
    }
  }

  if (menuToggleBtn) menuToggleBtn.addEventListener('click', openSidebar);
  if (sidebarCloseBtn) sidebarCloseBtn.addEventListener('click', closeSidebar);
  if (drawerOverlay) drawerOverlay.addEventListener('click', closeSidebar);

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && appSidebar && appSidebar.classList.contains('active')) {
      closeSidebar();
    }
  });

  // Navegación Sidebar (Pestañas Desktop y Móvil/Tablet)
  function switchTab(tabId) {
    document.querySelectorAll('.nav-tab').forEach(t => {
      const isActive = (t.dataset.tab === tabId);
      t.classList.toggle('active', isActive);
      if (isActive && topbarViewTitle) {
        topbarViewTitle.textContent = t.textContent.trim();
      }
    });
    
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
    
    const targetPane = document.getElementById(tabId);
    if (targetPane) targetPane.classList.add('active');
    
    if (tabId === 'settings-view') {
      loadDbInfo();
    }
    closeSidebar();
  }

  document.querySelectorAll('.nav-tab').forEach(tab => {
    tab.addEventListener('click', () => switchTab(tab.dataset.tab));
  });

  const btnSaveDb = document.getElementById('btn-save-db-local');
  if (btnSaveDb) {
    btnSaveDb.addEventListener('click', saveDatabaseToLocalDevice);
  }

  document.querySelectorAll('[data-close]').forEach(b => {
    b.addEventListener('click', () => document.getElementById(b.dataset.close).classList.add('hidden'));
  });

  document.getElementById('btn-new-card').addEventListener('click', () => {
    document.getElementById('form-card').reset();
    document.getElementById('card-id').value = '';
    currentCardLogoBase64 = null;
    updateLogoPreview(null);
    document.getElementById('modal-card').classList.remove('hidden');
  });
  document.getElementById('form-card').addEventListener('submit', handleSaveCard);
  document.getElementById('form-delete-card').addEventListener('submit', handleConfirmDeleteCard);

  document.getElementById('btn-new-movement').addEventListener('click', () => {
    document.getElementById('form-movement').reset();
    document.getElementById('mov-id').value = '';
    document.getElementById('modal-movement-title').textContent = "Registrar Gasto";
    const submitBtn = document.getElementById('btn-save-movement-submit');
    if (submitBtn) submitBtn.textContent = "Guardar";
    const msiContainer = document.getElementById('mov-msi-container');
    if (msiContainer) msiContainer.classList.remove('hidden');
    document.getElementById('msi-extra-fields').classList.add('hidden');
    document.getElementById('mov-is-msi').checked = false;
    document.getElementById('mov-date').value = new Date().toISOString().split('T')[0];
    document.getElementById('modal-movement').classList.remove('hidden');
  });
  document.getElementById('form-movement').addEventListener('submit', handleSaveMovement);
  document.getElementById('mov-is-msi').addEventListener('change', (e) => {
    document.getElementById('msi-extra-fields').classList.toggle('hidden', !e.target.checked);
  });

  document.getElementById('btn-new-setaside').addEventListener('click', () => openSetAsideModal());
  document.getElementById('btn-transfer-funds').addEventListener('click', () => openTransferModal());
  document.getElementById('transfer-from-type').addEventListener('change', updateTransferDestLabel);
  document.getElementById('form-transfer-fund').addEventListener('submit', handleTransferSubmit);
  document.getElementById('form-setaside').addEventListener('submit', handleSaveSetAside);

  document.getElementById('btn-new-person').addEventListener('click', () => {
    document.getElementById('form-person').reset();
    document.getElementById('modal-person').classList.remove('hidden');
  });
  document.getElementById('form-person').addEventListener('submit', async (e) => {
    e.preventDefault();
    await fetch('/api/people', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: document.getElementById('person-name').value })
    });
    document.getElementById('modal-person').classList.add('hidden');
    refreshAllData();
  });

  document.getElementById('btn-new-budget').addEventListener('click', () => {
    document.getElementById('form-budget').reset();
    document.getElementById('modal-budget').classList.remove('hidden');
  });
  document.getElementById('form-budget').addEventListener('submit', async (e) => {
    e.preventDefault();
    await fetch('/api/budgets', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: document.getElementById('budget-name').value, description: document.getElementById('budget-desc').value })
    });
    document.getElementById('modal-budget').classList.add('hidden');
    refreshAllData();
  });

  document.getElementById('form-budget-item').addEventListener('submit', async (e) => {
    e.preventDefault();
    await fetch('/api/budget-items', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        budget_id: document.getElementById('item-budget-id').value,
        concept: document.getElementById('item-concept').value,
        amount: parseMoney(document.getElementById('item-amount').value),
        type: document.getElementById('item-type').value,
        tag: document.getElementById('item-tag').value
      })
    });
    document.getElementById('modal-budget-item').classList.add('hidden');
    refreshAllData();
  });

  document.getElementById('btn-open-reset-modal').addEventListener('click', () => {
    document.getElementById('reset-pwd').value = '';
    document.getElementById('reset-error').classList.add('hidden');
    document.getElementById('modal-reset').classList.remove('hidden');
  });
  document.getElementById('form-reset').addEventListener('submit', handleReset);

  document.getElementById('btn-save-inflation').addEventListener('click', handleSaveInflation);
  document.getElementById('btn-export-sync').addEventListener('click', handleExportSync);
  document.getElementById('btn-change-pwd').addEventListener('click', handleChangePassword);
  document.getElementById('btn-print-movements').addEventListener('click', () => window.print());

  document.getElementById('filter-card').addEventListener('change', renderMovements);
  document.getElementById('filter-person').addEventListener('change', renderMovements);
}

function openBudgetItemModal(bId) {
  document.getElementById('form-budget-item').reset();
  document.getElementById('item-budget-id').value = bId;
  document.getElementById('modal-budget-item').classList.remove('hidden');
}

async function deleteBudget(bId) {
  if (!confirm("¿Eliminar presupuesto?")) return;
  await fetch(`/api/budgets/${bId}`, { method: 'DELETE' });
  refreshAllData();
}

async function deleteBudgetItem(id) {
  await fetch(`/api/budget-items/${id}`, { method: 'DELETE' });
  refreshAllData();
}

function setupDragAndDrop() {
  // Logo
  const dropLogo = document.getElementById('dropzone-logo');
  const inputLogo = document.getElementById('card-logo');
  dropLogo.addEventListener('click', (e) => { if (e.target.id !== 'btn-remove-logo') inputLogo.click(); });
  ['dragenter', 'dragover'].forEach(ev => dropLogo.addEventListener(ev, (e) => { e.preventDefault(); dropLogo.classList.add('dragover'); }));
  ['dragleave', 'drop'].forEach(ev => dropLogo.addEventListener(ev, (e) => { e.preventDefault(); dropLogo.classList.remove('dragover'); }));
  dropLogo.addEventListener('drop', (e) => {
    if (e.dataTransfer.files[0]) processLogo(e.dataTransfer.files[0]);
  });
  inputLogo.addEventListener('change', (e) => {
    if (e.target.files[0]) processLogo(e.target.files[0]);
  });
  document.getElementById('btn-remove-logo').addEventListener('click', (e) => {
    e.stopPropagation();
    currentCardLogoBase64 = null;
    inputLogo.value = '';
    updateLogoPreview(null);
  });

  // Sync Dropzone
  const dropSync = document.getElementById('dropzone-sync');
  const inputSync = document.getElementById('sync-file-input');
  if (dropSync && inputSync) {
    dropSync.addEventListener('click', () => inputSync.click());
    ['dragenter', 'dragover'].forEach(ev => dropSync.addEventListener(ev, (e) => { e.preventDefault(); dropSync.classList.add('dragover'); }));
    ['dragleave', 'drop'].forEach(ev => dropSync.addEventListener(ev, (e) => { e.preventDefault(); dropSync.classList.remove('dragover'); }));
    dropSync.addEventListener('drop', (e) => {
      e.preventDefault();
      dropSync.classList.remove('dragover');
      if (e.dataTransfer.files[0]) handleImportSync(e.dataTransfer.files[0]);
    });
    inputSync.addEventListener('change', (e) => {
      if (e.target.files[0]) handleImportSync(e.target.files[0]);
    });
  }

  // Base de Datos SQLite Dropzone
  const dropDb = document.getElementById('dropzone-db-restore');
  const inputDb = document.getElementById('db-file-input');
  if (dropDb && inputDb) {
    dropDb.addEventListener('click', () => inputDb.click());
    ['dragenter', 'dragover'].forEach(ev => dropDb.addEventListener(ev, (e) => { e.preventDefault(); dropDb.classList.add('dragover'); }));
    ['dragleave', 'drop'].forEach(ev => dropDb.addEventListener(ev, (e) => { e.preventDefault(); dropDb.classList.remove('dragover'); }));
    dropDb.addEventListener('drop', (e) => {
      e.preventDefault();
      dropDb.classList.remove('dragover');
      if (e.dataTransfer.files[0]) handleRestoreDb(e.dataTransfer.files[0]);
    });
    inputDb.addEventListener('change', (e) => {
      if (e.target.files[0]) handleRestoreDb(e.target.files[0]);
    });
  }

  // Restaurar archivo físico tarjetas.db desde pantalla de bloqueo
  const btnAuthRestore = document.getElementById('btn-auth-restore-db');
  const inputAuthDb = document.getElementById('auth-db-file-input');
  if (btnAuthRestore && inputAuthDb) {
    btnAuthRestore.addEventListener('click', () => inputAuthDb.click());
    inputAuthDb.addEventListener('change', (e) => {
      if (e.target.files[0]) handleRestoreDb(e.target.files[0]);
    });
  }
}

function processLogo(file) {
  const reader = new FileReader();
  reader.onload = () => {
    currentCardLogoBase64 = reader.result;
    updateLogoPreview(reader.result);
  };
  reader.readAsDataURL(file);
}

function updateLogoPreview(base64) {
  const content = document.getElementById('dropzone-logo-content');
  const preview = document.getElementById('card-logo-preview');
  const img = document.getElementById('logo-preview-img');
  if (base64) {
    img.src = base64;
    content.classList.add('hidden');
    preview.classList.remove('hidden');
  } else {
    img.src = '';
    content.classList.remove('hidden');
    preview.classList.add('hidden');
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function showToast(msg, type = "normal") {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.textContent = msg;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3500);
}

// --- SOPORTE PWA / INSTALACIÓN NATIVA EN ANDROID ---
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/service-worker.js').catch(err => {
      console.warn("ServiceWorker:", err);
    });
  });
}

let deferredInstallPrompt = null;
window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredInstallPrompt = e;
  const btnInstall = document.getElementById('btn-install-app');
  if (btnInstall) {
    btnInstall.classList.remove('hidden');
    btnInstall.onclick = async () => {
      if (deferredInstallPrompt) {
        deferredInstallPrompt.prompt();
        const { outcome } = await deferredInstallPrompt.userChoice;
        if (outcome === 'accepted') {
          showToast("¡CardMaster instalada con éxito en tu celular!", "success");
        }
        deferredInstallPrompt = null;
        btnInstall.classList.add('hidden');
      }
    };
  }
});
