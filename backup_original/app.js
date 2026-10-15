/* Spendly frontend integration. Expected backend runs on the same origin (e.g. http://localhost:8080). */
const API_BASE = window.SPENDLY_API_BASE || '';
const DEMO_TRANSACTIONS = [
  { id: 'demo-1', date: '2026-10-02', description: 'Monthly salary', category: 'Income', amount: 40000, type: 'INCOME', isTransfer: false },
  { id: 'demo-2', date: '2026-10-03', description: 'Cafe coffee and lunch', category: 'Food & Dining', amount: 420, type: 'EXPENSE', isTransfer: false },
  { id: 'demo-3', date: '2026-10-04', description: 'City taxi', category: 'Transport', amount: 280, type: 'EXPENSE', isTransfer: false },
  { id: 'demo-4', date: '2026-10-05', description: 'Clothing store', category: 'Shopping', amount: 1850, type: 'EXPENSE', isTransfer: false },
  { id: 'demo-5', date: '2026-10-06', description: 'Transfer to own savings', category: 'Transfer', amount: 2000, type: 'TRANSFER', isTransfer: true }
];
const state = { transactions: [], budgets: [], dashboard: null, reports: null, backendOnline: false, demoMode: false, currentPage: 'dashboard' };
const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];

function money(value) {
  const number = Number(value || 0);
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(number);
}
function escapeHTML(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);
}
function dateLabel(value) {
  if (!value) return '—';
  const date = new Date(`${String(value).slice(0, 10)}T00:00:00`);
  return Number.isNaN(date.getTime()) ? escapeHTML(value) : date.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}
function normaliseTransaction(raw, index = 0) {
  const amount = Number(raw.amount ?? raw.transactionAmount ?? raw.value ?? 0);
  const typeRaw = String(raw.type ?? raw.transactionType ?? raw.direction ?? '').toUpperCase();
  const category = raw.category?.name ?? raw.categoryName ?? raw.category ?? 'Other';
  const transfer = Boolean(raw.isTransfer ?? raw.ownAccountTransfer ?? typeRaw.includes('TRANSFER') ?? false) || String(category).toLowerCase() === 'transfer';
  let type = typeRaw;
  if (!type || !['INCOME', 'EXPENSE', 'TRANSFER'].includes(type)) {
    if (transfer) type = 'TRANSFER';
    else if (String(raw.direction ?? '').toLowerCase() === 'credit' || amount < 0) type = 'INCOME';
    else type = 'EXPENSE';
  }
  return {
    ...raw,
    id: raw.id ?? raw.transactionId ?? `row-${index}`,
    date: raw.date ?? raw.transactionDate ?? raw.postedDate ?? '',
    description: raw.description ?? raw.narration ?? raw.merchant ?? raw.details ?? 'Transaction',
    category: String(category),
    amount: Math.abs(amount),
    type,
    isTransfer: transfer || type === 'TRANSFER'
  };
}
function normaliseList(payload, keys = ['content', 'transactions', 'items', 'data', 'results']) {
  if (Array.isArray(payload)) return payload;
  for (const key of keys) if (Array.isArray(payload?.[key])) return payload[key];
  return [];
}
async function apiRequest(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const contentType = response.headers.get('content-type') || '';
  const body = contentType.includes('application/json') ? await response.json() : await response.text();
  if (!response.ok) {
    const message = typeof body === 'string' ? body : body.message || body.error || body.detail || `Request failed (${response.status})`;
    throw new Error(message);
  }
  return body;
}
function setConnection(online, message) {
  state.backendOnline = online;
  $('#connectionDot').classList.toggle('online', online);
  $('#connectionText').textContent = message || (online ? 'Backend connected' : 'Backend unavailable');
}
function showNotice(message, kind = 'info') {
  const notice = $('#globalNotice');
  notice.textContent = message;
  notice.className = `notice ${kind}`;
  notice.classList.remove('hidden');
}
function clearNotice() { $('#globalNotice').classList.add('hidden'); }
function setPage(page) {
  if (!$(`#page-${page}`)) page = 'dashboard';
  state.currentPage = page;
  $$('.page').forEach((el) => el.classList.toggle('active', el.id === `page-${page}`));
  $$('.nav-link').forEach((el) => el.classList.toggle('active', el.dataset.page === page));
  $('#pageCrumb').textContent = ({ dashboard: 'Dashboard', transactions: 'Transactions', import: 'Import statement', reports: 'Reports', budgets: 'Budgets' })[page];
  $('#sidebar').classList.remove('mobile-open');
  if (page === 'reports') loadReports();
  if (page === 'budgets') loadBudgets();
  if (page === 'dashboard') loadDashboard();
}
function renderStat(id, value) { $(id).textContent = value; }
function calculateTotals(transactions) {
  return transactions.reduce((totals, tx) => {
    if (tx.isTransfer || tx.type === 'TRANSFER') return totals;
    if (tx.type === 'INCOME') totals.income += tx.amount;
    else totals.expenses += tx.amount;
    return totals;
  }, { income: 0, expenses: 0 });
}
function renderDashboard(payload = {}) {
  const transactions = state.transactions;
  const computed = calculateTotals(transactions);
  const income = Number(payload.totalIncome ?? payload.income ?? payload.incomeTotal ?? computed.income);
  const expenses = Number(payload.totalExpenses ?? payload.expenses ?? payload.expenseTotal ?? computed.expenses);
  const net = Number(payload.netCashFlow ?? payload.netCashflow ?? payload.net ?? income - expenses);
  const savings = income > 0 ? (net / income) * 100 : 0;
  renderStat('#totalIncome', money(income));
  renderStat('#totalExpenses', money(expenses));
  renderStat('#netCashFlow', money(net));
  renderStat('#savingsRate', `${Math.round(savings)}%`);
  const rawCategories = payload.categoryBreakdown ?? payload.spendingByCategory ?? payload.categories ?? payload.expensesByCategory;
  let categories = [];
  if (Array.isArray(rawCategories)) categories = rawCategories.map((item) => ({ name: item.category ?? item.name ?? item.label ?? 'Other', value: Number(item.amount ?? item.total ?? item.value ?? 0) }));
  else if (rawCategories && typeof rawCategories === 'object') categories = Object.entries(rawCategories).map(([name, value]) => ({ name, value: Number(value?.amount ?? value?.total ?? value) }));
  if (!categories.length) {
    const map = {};
    transactions.filter((tx) => tx.type === 'EXPENSE' && !tx.isTransfer).forEach((tx) => { map[tx.category] = (map[tx.category] || 0) + tx.amount; });
    categories = Object.entries(map).map(([name, value]) => ({ name, value }));
  }
  renderCategoryBars('#categoryChart', categories);
  renderRecentTransactions();
  renderBudgetPreview();
}
function renderCategoryBars(selector, categories) {
  const container = $(selector);
  const sorted = categories.filter((item) => item.value > 0).sort((a, b) => b.value - a.value).slice(0, 8);
  if (!sorted.length) { container.innerHTML = '<div class="empty-state">No spending data yet. Import a statement to see your categories.</div>'; return; }
  const max = Math.max(...sorted.map((item) => item.value), 1);
  container.innerHTML = sorted.map((item) => `<div class="category-row"><div class="category-row-top"><span>${escapeHTML(item.name)}</span><strong>${money(item.value)}</strong></div><div class="bar-track"><div class="bar-fill" style="width:${Math.max(3, item.value / max * 100)}%"></div></div></div>`).join('');
}
function transactionRow(tx, editable = false) {
  const isIncome = tx.type === 'INCOME';
  const amountClass = tx.isTransfer ? 'amount-neutral' : isIncome ? 'amount-positive' : 'amount-negative';
  const amountPrefix = tx.isTransfer ? '' : isIncome ? '+' : '−';
  const categoryOptions = ['Income', 'Food & Dining', 'Transport', 'Shopping', 'Bills & Utilities', 'Health', 'Transfer', 'Other'];
  const categoryCell = editable
    ? `<select class="inline-category" data-transaction-id="${escapeHTML(tx.id)}" aria-label="Change category for ${escapeHTML(tx.description)}">${[...new Set([...categoryOptions, tx.category])].map((category) => `<option value="${escapeHTML(category)}" ${category === tx.category ? 'selected' : ''}>${escapeHTML(category)}</option>`).join('')}</select>`
    : `<span class="category-chip">${escapeHTML(tx.category)}</span>`;
  return `<tr><td><div class="transaction-title">${escapeHTML(tx.description)}</div><div class="transaction-subtitle">${tx.isTransfer ? 'Own-account transfer' : escapeHTML(tx.type.toLowerCase())}</div></td><td>${dateLabel(tx.date)}</td><td>${categoryCell}</td>${editable ? `<td><span class="type-label ${tx.type.toLowerCase()}">${escapeHTML(tx.type)}</span></td>` : ''}<td class="align-right ${amountClass}">${amountPrefix}${money(tx.amount)}</td></tr>`;
}
function renderRecentTransactions() {
  const recent = [...state.transactions].sort((a, b) => String(b.date).localeCompare(String(a.date))).slice(0, 5);
  $('#recentTransactions').innerHTML = recent.length ? recent.map((tx) => transactionRow(tx)).join('') : '<tr><td colspan="4" class="table-empty">No transactions yet. Import a statement to get started.</td></tr>';
}
function renderTransactionsTable() {
  const query = $('#transactionSearch').value.trim().toLowerCase();
  const category = $('#categoryFilter').value;
  const filtered = [...state.transactions].filter((tx) => (!category || tx.category === category) && (!query || `${tx.description} ${tx.category} ${tx.date}`.toLowerCase().includes(query))).sort((a, b) => String(b.date).localeCompare(String(a.date)));
  $('#transactionsTable').innerHTML = filtered.length ? filtered.map((tx) => transactionRow(tx, true)).join('') : '<tr><td colspan="5" class="table-empty">No matching transactions.</td></tr>';
  $('#transactionCount').textContent = `${filtered.length} transaction${filtered.length === 1 ? '' : 's'}`;
}
function renderBudgetPreview() {
  const container = $('#budgetPreview');
  const budgets = state.budgets.slice(0, 3);
  if (!budgets.length) { container.innerHTML = '<div class="empty-state">No budgets yet. Create a category budget to track spending.</div>'; return; }
  container.innerHTML = budgets.map((budget) => budgetItemHTML(budget, true)).join('');
}
function budgetItemHTML(budget, compact = false) {
  const category = budget.category?.name ?? budget.categoryName ?? budget.category ?? 'Other';
  const limit = Number(budget.limitAmount ?? budget.monthlyLimit ?? budget.limit ?? budget.amount ?? 0);
  const spent = Number(budget.spentAmount ?? budget.spent ?? budget.actualAmount ?? 0);
  const pct = limit > 0 ? Math.min(100, spent / limit * 100) : 0;
  const warning = limit > 0 && spent >= limit;
  return `<article class="${compact ? 'budget-preview-item' : 'panel budget-card'}"><div class="budget-item-head"><div><strong>${escapeHTML(category)}</strong><span>${money(spent)} of ${money(limit)}</span></div><span class="budget-percent ${warning ? 'over-budget' : ''}">${Math.round(pct)}%</span></div><div class="bar-track"><div class="bar-fill ${warning ? 'bar-warning' : ''}" style="width:${pct}%"></div></div>${warning ? '<p class="budget-warning">Budget limit reached or exceeded.</p>' : ''}</article>`;
}
function renderBudgets() {
  $('#budgetsList').innerHTML = state.budgets.length ? state.budgets.map((budget) => budgetItemHTML(budget)).join('') : '<article class="panel empty-state">No budgets yet. Create your first category budget above.</article>';
  renderBudgetPreview();
}
function renderReports(payload = {}) {
  const computed = calculateTotals(state.transactions);
  const income = Number(payload.totalIncome ?? payload.income ?? computed.income);
  const expenses = Number(payload.totalExpenses ?? payload.expenses ?? computed.expenses);
  const net = Number(payload.netCashFlow ?? payload.net ?? income - expenses);
  renderStat('#reportIncome', money(income)); renderStat('#reportExpenses', money(expenses)); renderStat('#reportNet', money(net));
  const raw = payload.categoryBreakdown ?? payload.spendingByCategory ?? payload.categories ?? [];
  let categories = Array.isArray(raw) ? raw.map((item) => ({ name: item.category ?? item.name ?? 'Other', value: Number(item.amount ?? item.total ?? item.value ?? 0) })) : Object.entries(raw).map(([name, value]) => ({ name, value: Number(value) }));
  if (!categories.length) { const map = {}; state.transactions.filter((tx) => tx.type === 'EXPENSE' && !tx.isTransfer).forEach((tx) => map[tx.category] = (map[tx.category] || 0) + tx.amount); categories = Object.entries(map).map(([name, value]) => ({ name, value })); }
  renderCategoryBars('#reportCategories', categories);
}
async function loadTransactions() {
  const payload = await apiRequest('/api/transactions');
  state.transactions = normaliseList(payload).map(normaliseTransaction);
  renderTransactionsTable(); renderRecentTransactions();
}
async function loadBudgets() {
  try {
    const payload = await apiRequest('/api/budgets');
    state.budgets = normaliseList(payload, ['budgets', 'content', 'items', 'data']).map((item) => ({ ...item }));
    renderBudgets();
  } catch (error) {
    if (!state.backendOnline) return;
    $('#budgetsList').innerHTML = `<article class="panel error-state">Could not load budgets: ${escapeHTML(error.message)}</article>`;
  }
}
async function loadDashboard() {
  const period = $('#dashboardPeriod').value;
  try {
    const payload = await apiRequest(`/api/dashboard?period=${encodeURIComponent(period)}`);
    state.dashboard = payload; renderDashboard(payload);
  } catch (error) {
    renderDashboard({});
    if (state.backendOnline) showNotice(`Dashboard summary could not be loaded: ${error.message}`, 'error');
  }
}
async function loadReports() {
  const period = $('#reportPeriod').value;
  try {
    const payload = await apiRequest(`/api/reports?period=${encodeURIComponent(period)}`);
    state.reports = payload; renderReports(payload);
  } catch (error) {
    renderReports({});
    if (state.backendOnline) showNotice(`Report could not be loaded: ${error.message}`, 'error');
  }
}
async function refreshAll() {
  clearNotice();
  try {
    await apiRequest('/api/transactions');
    setConnection(true, 'Backend connected');
    const transactionPayload = await apiRequest('/api/transactions');
    state.transactions = normaliseList(transactionPayload).map(normaliseTransaction);
    state.demoMode = false;
    await Promise.allSettled([loadBudgets(), loadDashboard(), loadReports()]);
    renderTransactionsTable(); renderRecentTransactions();
  } catch (error) {
    setConnection(false, 'Backend unavailable');
    state.demoMode = true;
    state.transactions = DEMO_TRANSACTIONS.map(normaliseTransaction);
    state.budgets = [];
    renderTransactionsTable(); renderDashboard({}); renderReports({}); renderBudgets();
    showNotice('The backend is not responding. Showing sample data for frontend preview only. Start Spring Boot and check the API routes before testing real imports.', 'warning');
  }
}
async function uploadStatement(event) {
  event.preventDefault();
  const input = $('#statementFile'); const file = input.files[0];
  if (!file) { $('#uploadStatus').textContent = 'Choose a statement file first.'; return; }
  const extension = file.name.split('.').pop().toLowerCase();
  if (!['csv', 'xls', 'xlsx', 'pdf'].includes(extension)) { $('#uploadStatus').textContent = 'Please choose a CSV, XLS, XLSX or PDF file.'; return; }
  if (!state.backendOnline) { $('#uploadStatus').textContent = 'Backend is not connected. Start the Spring Boot server before uploading.'; return; }
  const formData = new FormData(); formData.append('file', file);
  const button = $('#uploadButton'); button.disabled = true; button.textContent = 'Uploading…'; $('#uploadStatus').textContent = 'Uploading and analysing your statement…';
  try {
    const result = await apiRequest('/api/statements/import', { method: 'POST', body: formData });
    const imported = result.importedCount ?? result.transactionCount ?? result.count;
    $('#uploadStatus').textContent = `Import completed${imported !== undefined ? `: ${imported} transaction(s) processed` : ''}. Refreshing your data…`;
    showNotice('Statement import completed. Your transaction and dashboard data are refreshing.', 'success');
    await refreshAll();
    setPage('transactions');
    $('#uploadStatus').textContent = 'Import completed. Check the transaction list for the imported rows.';
  } catch (error) {
    $('#uploadStatus').textContent = `Upload failed: ${error.message}. Confirm the backend endpoint and multipart field name with Person 1.`;
  } finally { button.disabled = false; button.textContent = 'Upload and analyse'; }
}
async function updateCategory(event) {
  const select = event.target.closest('.inline-category');
  if (!select) return;
  const id = select.dataset.transactionId; const category = select.value;
  if (String(id).startsWith('demo-') || state.demoMode) { showNotice('This is sample data. Connect the backend to save category changes.', 'warning'); return; }
  const previous = state.transactions.find((tx) => String(tx.id) === String(id))?.category;
  select.disabled = true;
  try {
    await apiRequest(`/api/transactions/${encodeURIComponent(id)}`, { method: 'PATCH', body: JSON.stringify({ category }) });
    const transaction = state.transactions.find((tx) => String(tx.id) === String(id));
    if (transaction) transaction.category = category;
    renderTransactionsTable(); renderDashboard(state.dashboard || {}); showNotice('Category updated successfully.', 'success');
  } catch (error) {
    select.value = previous || category;
    showNotice(`Could not save category: ${error.message}`, 'error');
  } finally { select.disabled = false; }
}
async function saveBudget(event) {
  event.preventDefault();
  const category = $('#budgetCategory').value; const limitAmount = Number($('#budgetLimit').value);
  if (!(limitAmount > 0)) { $('#budgetFormStatus').textContent = 'Enter a budget greater than zero.'; return; }
  if (!state.backendOnline || state.demoMode) { $('#budgetFormStatus').textContent = 'Connect the backend before saving a budget.'; return; }
  const button = $('#saveBudgetButton'); button.disabled = true; $('#budgetFormStatus').textContent = 'Saving budget…';
  try {
    await apiRequest('/api/budgets', { method: 'POST', body: JSON.stringify({ category, limitAmount, monthlyLimit: limitAmount }) });
    $('#budgetFormStatus').textContent = 'Budget saved.'; $('#budgetLimit').value = '';
    await loadBudgets(); showNotice('Budget saved successfully.', 'success');
  } catch (error) { $('#budgetFormStatus').textContent = `Could not save budget: ${error.message}`; }
  finally { button.disabled = false; }
}
function bindEvents() {
  $$('.nav-link').forEach((button) => button.addEventListener('click', () => setPage(button.dataset.page)));
  $$('[data-go]').forEach((button) => button.addEventListener('click', () => setPage(button.dataset.go)));
  $('#mobileMenu').addEventListener('click', () => $('#sidebar').classList.toggle('mobile-open'));
  $('#refreshButton').addEventListener('click', refreshAll);
  $('#dashboardPeriod').addEventListener('change', loadDashboard);
  $('#reportPeriod').addEventListener('change', loadReports);
  $('#transactionSearch').addEventListener('input', renderTransactionsTable);
  $('#categoryFilter').addEventListener('change', renderTransactionsTable);
  $('#clearFilters').addEventListener('click', () => { $('#transactionSearch').value = ''; $('#categoryFilter').value = ''; renderTransactionsTable(); });
  $('#transactionsTable').addEventListener('change', updateCategory);
  $('#uploadForm').addEventListener('submit', uploadStatement);
  $('#statementFile').addEventListener('change', () => {
    const file = $('#statementFile').files[0];
    $('#selectedFile').classList.toggle('hidden', !file);
    $('#selectedFile').textContent = file ? `Selected: ${file.name} (${(file.size / 1024).toFixed(1)} KB)` : '';
    if (file) $('#filePrompt').textContent = file.name;
  });
  const dropZone = $('#dropZone');
  ['dragenter', 'dragover'].forEach((name) => dropZone.addEventListener(name, (event) => { event.preventDefault(); dropZone.classList.add('drag-over'); }));
  ['dragleave', 'drop'].forEach((name) => dropZone.addEventListener(name, (event) => { event.preventDefault(); dropZone.classList.remove('drag-over'); }));
  dropZone.addEventListener('drop', (event) => { const file = event.dataTransfer.files[0]; if (file) { const transfer = new DataTransfer(); transfer.items.add(file); $('#statementFile').files = transfer.files; $('#statementFile').dispatchEvent(new Event('change')); } });
  $('#budgetForm').addEventListener('submit', saveBudget);
}
document.addEventListener('DOMContentLoaded', () => { bindEvents(); refreshAll(); });
