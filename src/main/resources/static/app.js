/**
 * Spendly — Smart Money, Simply.
 * Master Client Application Script (Production Full-Stack Edition)
 */

(function () {
  'use strict';

  // --- API BASE URL CONFIGURATION ---
  const API_BASE = window.SPENDLY_API_BASE || (
    (typeof window !== 'undefined' && (window.location.protocol === 'file:' || (window.location.port && window.location.port !== '8080')))
      ? 'http://localhost:8080'
      : ''
  );

  // --- GLOBAL STATE ---
  const state = {
    currentUser: null,
    token: localStorage.getItem('spendly_token') || '',
    activeSection: 'dashboard',
    dashboardPeriod: 'month',
    transactions: [],
    budgets: [],
    splitBills: [],
    obligations: [],
    parentSummary: null,
    activityLogs: [],
    activityLogPage: 0,
    activityLogTotalPages: 1,
    alerts: [],
    categories: [],
    categorySourceModal: null,
    activeModal: null
  };

  // --- UTILITIES ---
  const $ = (selector, ctx = document) => ctx.querySelector(selector);
  const $$ = (selector, ctx = document) => [...ctx.querySelectorAll(selector)];

  function escapeHTML(str) {
    if (str === null || str === undefined) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function formatMoney(amount, currencySymbol = '₹') {
    const num = Number(amount || 0);
    return currencySymbol + num.toLocaleString('en-IN', {
      maximumFractionDigits: 2,
      minimumFractionDigits: 0
    });
  }

  function formatDate(dateStr) {
    if (!dateStr) return '—';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return escapeHTML(dateStr);
      return d.toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric'
      });
    } catch (e) {
      return escapeHTML(dateStr);
    }
  }

  function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '—';
    try {
      const d = new Date(dateTimeStr);
      if (isNaN(d.getTime())) return escapeHTML(dateTimeStr);
      return d.toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric'
      }) + ' ' + d.toLocaleTimeString('en-IN', {
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return escapeHTML(dateTimeStr);
    }
  }

  function getInitials(name) {
    if (!name) return 'SP';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  // --- TOAST NOTIFICATIONS ---
  function showToast(message, type = 'info', duration = 4000) {
    const container = $('#toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = 'ℹ';
    if (type === 'success') icon = '✓';
    else if (type === 'danger' || type === 'error') icon = '✕';
    else if (type === 'warning') icon = '⚠';

    toast.innerHTML = `
      <span class="toast-icon">${icon}</span>
      <div class="toast-body">${escapeHTML(message)}</div>
      <button class="toast-close" aria-label="Dismiss">&times;</button>
    `;

    toast.querySelector('.toast-close').addEventListener('click', () => {
      toast.remove();
    });

    container.appendChild(toast);

    setTimeout(() => {
      if (toast.parentNode) {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
      }
    }, duration);
  }

  // --- API REQUEST HELPER ---
  async function api(endpoint, options = {}) {
    const headers = new Headers(options.headers || {});
    
    if (state.token) {
      headers.set('Authorization', `Bearer ${state.token}`);
    }

    if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }

    headers.set('bypass-tunnel-reminder', 'true');

    const response = await fetch(`${API_BASE}${endpoint}`, {
      ...options,
      headers
    });

    if (response.status === 401) {
      // Unauthorized or session expired
      handleUnauthorized();
      throw new Error('Authentication required. Please sign in.');
    }

    const contentType = response.headers.get('content-type') || '';
    let body;
    if (contentType.includes('application/json')) {
      body = await response.json();
    } else {
      body = await response.text();
    }

    if (!response.ok) {
      let message = 'Request failed';
      if (typeof body === 'object' && body !== null) {
        message = body.message || body.error || JSON.stringify(body);
      } else if (typeof body === 'string' && body.trim().length > 0) {
        message = body;
      }
      const err = new Error(message);
      err.status = response.status;
      err.body = body;
      throw err;
    }

    return body;
  }

  // --- AUTHENTICATION MODULE ---
  function initAuth() {
    const loginForm = $('#loginForm');
    const registerForm = $('#registerForm');
    const showRegisterBtn = $('#showRegisterBtn');
    const showLoginBtn = $('#showLoginBtn');
    const fillParentDemo = $('#fillParentDemo');
    const fillChildDemo = $('#fillChildDemo');
    const toggleLoginPw = $('#toggleLoginPw');
    const toggleRegPw = $('#toggleRegPw');
    const forgotPwLink = $('#forgotPwLink');
    const logoutBtn = $('#logoutButton');

    // Toggle between login and register views
    showRegisterBtn?.addEventListener('click', (e) => {
      e.preventDefault();
      $('#loginView').style.display = 'none';
      $('#registerView').style.display = 'block';
      clearAuthAlerts();
    });

    showLoginBtn?.addEventListener('click', (e) => {
      e.preventDefault();
      $('#registerView').style.display = 'none';
      $('#loginView').style.display = 'block';
      clearAuthAlerts();
    });

    // Password visibility toggles
    toggleLoginPw?.addEventListener('click', () => {
      const input = $('#loginPassword');
      if (input.type === 'password') {
        input.type = 'text';
        toggleLoginPw.textContent = '🔒';
      } else {
        input.type = 'password';
        toggleLoginPw.textContent = '👁';
      }
    });

    toggleRegPw?.addEventListener('click', () => {
      const input = $('#regPassword');
      if (input.type === 'password') {
        input.type = 'text';
        toggleRegPw.textContent = '🔒';
      } else {
        input.type = 'password';
        toggleRegPw.textContent = '👁';
      }
    });

    // Instant Demo buttons
    fillParentDemo?.addEventListener('click', () => {
      $('#loginEmail').value = 'vrinda@spendly.io';
      $('#loginPassword').value = 'Spendly@2026';
      showToast('Filled parent demo account (Vrinda)', 'info', 2000);
    });

    fillChildDemo?.addEventListener('click', () => {
      $('#loginEmail').value = 'aarav@spendly.io';
      $('#loginPassword').value = 'Aarav@2026';
      showToast('Filled child demo account (Aarav)', 'info', 2000);
    });

    // Forgot password link
    forgotPwLink?.addEventListener('click', async (e) => {
      e.preventDefault();
      const email = $('#loginEmail').value.trim() || prompt('Enter your registered email:');
      if (!email) return;
      try {
        await api('/api/auth/forgot-password', {
          method: 'POST',
          body: JSON.stringify({ email })
        });
        showAuthAlert('loginAlert', 'Password reset instructions have been sent to your email.', 'success');
      } catch (err) {
        showAuthAlert('loginAlert', err.message, 'danger');
      }
    });

    // Login form submit
    loginForm?.addEventListener('submit', async (e) => {
      e.preventDefault();
      clearAuthAlerts();
      const email = $('#loginEmail').value.trim();
      const password = $('#loginPassword').value;
      const loginBtn = $('#loginBtn');

      loginBtn.disabled = true;
      loginBtn.textContent = 'Signing in…';

      try {
        const res = await api('/api/auth/login', {
          method: 'POST',
          body: JSON.stringify({ email, password })
        });

        if (res.token) {
          state.token = res.token;
          localStorage.setItem('spendly_token', res.token);
        }
        const userObj = res.user || {
          id: res.userId,
          email: res.email || email,
          fullName: res.fullName || email.split('@')[0],
          role: res.role || 'PARENT',
          currency: res.currency || '₹',
          monthlyBudget: res.monthlyBudget || 40000
        };
        state.currentUser = userObj;
        localStorage.setItem('spendly_user', JSON.stringify(userObj));

        completeLogin();
        showToast(`Welcome back, ${userObj.fullName || 'User'}!`, 'success');
      } catch (err) {
        showAuthAlert('loginAlert', err.message || 'Invalid credentials. Please try again.', 'danger');
      } finally {
        loginBtn.disabled = false;
        loginBtn.textContent = 'Sign in →';
      }
    });

    // Register form submit
    registerForm?.addEventListener('submit', async (e) => {
      e.preventDefault();
      clearAuthAlerts();
      const fullName = $('#regFullName').value.trim();
      const email = $('#regEmail').value.trim();
      const password = $('#regPassword').value;
      const confirmPassword = $('#regConfirmPassword').value;
      const registerBtn = $('#registerBtn');

      if (password !== confirmPassword) {
        showAuthAlert('registerAlert', 'Passwords do not match.', 'danger');
        return;
      }

      registerBtn.disabled = true;
      registerBtn.textContent = 'Creating account…';

      try {
        const res = await api('/api/auth/register', {
          method: 'POST',
          body: JSON.stringify({ fullName, email, password })
        });

        if (res.token) {
          state.token = res.token;
          localStorage.setItem('spendly_token', res.token);
        }
        state.currentUser = res.user;
        localStorage.setItem('spendly_user', JSON.stringify(res.user));

        completeLogin();
        showToast(`Account created successfully! Welcome, ${res.user.fullName}!`, 'success');
      } catch (err) {
        showAuthAlert('registerAlert', err.message || 'Registration failed.', 'danger');
      } finally {
        registerBtn.disabled = false;
        registerBtn.textContent = 'Create account →';
      }
    });

    // Logout button
    logoutBtn?.addEventListener('click', handleLogout);
  }

  function showAuthAlert(elementId, message, type = 'danger') {
    const el = $(`#${elementId}`);
    if (el) {
      el.className = `auth-alert show ${type}`;
      el.textContent = message;
    }
  }

  function clearAuthAlerts() {
    $$('.auth-alert').forEach(el => {
      el.className = 'auth-alert';
      el.textContent = '';
    });
  }

  function completeLogin() {
    const authWrapper = $('#authWrapper');
    const appShell = $('#appShell');

    if (authWrapper) authWrapper.style.display = 'none';
    if (appShell) appShell.style.display = 'flex';

    updateUserProfileUI();
    loadCategories();
    loadSectionData(state.activeSection);
    loadAlerts();
  }

  function handleUnauthorized() {
    state.currentUser = null;
    state.token = '';
    localStorage.removeItem('spendly_token');
    localStorage.removeItem('spendly_user');

    const authWrapper = $('#authWrapper');
    const appShell = $('#appShell');
    if (authWrapper) authWrapper.style.display = 'flex';
    if (appShell) appShell.style.display = 'none';
  }

  async function handleLogout() {
    try {
      await api('/api/auth/logout', { method: 'POST' });
    } catch (e) {
      // Ignore network errors on logout
    }
    handleUnauthorized();
    showToast('You have been signed out.', 'info');
  }

  async function checkExistingSession() {
    const savedToken = localStorage.getItem('spendly_token');
    if (!savedToken) {
      handleUnauthorized();
      return;
    }

    state.token = savedToken;
    try {
      const res = await api('/api/auth/me');
      const user = res.user || (res.id ? res : null);
      if (user) {
        state.currentUser = user;
        localStorage.setItem('spendly_user', JSON.stringify(user));
        completeLogin();
      } else {
        handleUnauthorized();
      }
    } catch (e) {
      // Fallback: if server error or offline, check if cached user exists
      const cached = localStorage.getItem('spendly_user');
      if (cached) {
        try {
          state.currentUser = JSON.parse(cached);
          completeLogin();
          return;
        } catch (err) {}
      }
      handleUnauthorized();
    }
  }

  function updateUserProfileUI() {
    const user = state.currentUser || {
      fullName: 'Vrinda Atluri',
      email: 'vrinda@spendly.io',
      role: 'PARENT'
    };

    const firstName = user.fullName ? user.fullName.split(' ')[0] : 'Vrinda';
    const initials = getInitials(user.fullName || 'Vrinda');

    // Topbar greeting
    const greetingEl = $('#pageTitle');
    if (greetingEl) {
      const hour = new Date().getHours();
      let timeGreeting = 'Good morning';
      if (hour >= 12 && hour < 17) timeGreeting = 'Good afternoon';
      else if (hour >= 17) timeGreeting = 'Good evening';
      greetingEl.innerHTML = `${timeGreeting}, ${escapeHTML(firstName)} <span>✦</span>`;
    }

    // Topbar & sidebar profile pills
    const topAvatar = $('#topAvatar');
    const topUserName = $('#topUserName');
    if (topAvatar) topAvatar.textContent = initials;
    if (topUserName) topUserName.textContent = firstName;

    const sidebarAvatar = $('#sidebarAvatar');
    const sidebarUserName = $('#sidebarUserName');
    const sidebarUserRole = $('#sidebarUserRole');
    if (sidebarAvatar) sidebarAvatar.textContent = initials;
    if (sidebarUserName) sidebarUserName.textContent = firstName;
    
    const isChild = user.role === 'CHILD';
    if (sidebarUserRole) {
      sidebarUserRole.textContent = isChild ? 'Linked minor account' : (user.role === 'PARENT' ? 'Parent account' : 'Personal account');
    }

    // Role-based rule switches accessibility
    const switches = [$('#ruleShopping'), $('#ruleEntertainment'), $('#ruleFood'), $('#ruleGaming')];
    switches.forEach(sw => {
      if (sw) {
        sw.disabled = isChild;
        if (isChild) {
          sw.title = 'Configured by parent. Children cannot alter permission settings.';
        } else {
          sw.removeAttribute('title');
        }
      }
    });

    // Topbar date
    const dateEl = $('#topbarDate');
    if (dateEl) {
      const now = new Date();
      dateEl.textContent = now.toLocaleDateString('en-US', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    }

    // Settings fields
    const sName = $('#settingsFullName');
    const sEmail = $('#settingsEmail');
    const sBudget = $('#settingsMonthlyBudget');
    if (sName) sName.value = user.fullName || '';
    if (sEmail) sEmail.value = user.email || '';
    if (sBudget && user.monthlyBudget) sBudget.value = user.monthlyBudget;
  }

  // --- NAVIGATION MODULE ---
  function initNavigation() {
    const navItems = $$('.sidebar .nav-item');
    const menuBtn = $('#menuBtn');
    const sidebar = $('#sidebar');

    navItems.forEach(item => {
      item.addEventListener('click', () => {
        const sectionId = item.dataset.section;
        if (!sectionId) return;

        navigateTo(sectionId);
        sidebar?.classList.remove('mobile-open');
      });
    });

    // Mobile menu toggle
    menuBtn?.addEventListener('click', () => {
      sidebar?.classList.toggle('mobile-open');
    });

    // Close mobile menu on click outside
    document.addEventListener('click', (e) => {
      if (sidebar?.classList.contains('mobile-open') &&
          !sidebar.contains(e.target) &&
          e.target !== menuBtn) {
        sidebar.classList.remove('mobile-open');
      }
    });

    // Quick jump buttons throughout UI (e.g. "View all →", "Review budget")
    document.addEventListener('click', (e) => {
      const jumpBtn = e.target.closest('[data-section-jump]');
      if (jumpBtn) {
        const targetSection = jumpBtn.dataset.sectionJump;
        if (targetSection) {
          navigateTo(targetSection);
        }
      }
    });

    // Topbar profile pill click opens Settings
    $('#topProfilePill')?.addEventListener('click', () => {
      navigateTo('settings');
    });

    $('#sidebarProfileCard')?.addEventListener('click', () => {
      navigateTo('settings');
    });

    // Notification dropdown toggle
    const bellBtn = $('#notifBellBtn');
    const notifDropdown = $('#notifDropdown');
    const markAllReadBtn = $('#markAllReadBtn');

    bellBtn?.addEventListener('click', (e) => {
      e.stopPropagation();
      if (notifDropdown) {
        const isHidden = notifDropdown.style.display === 'none' || notifDropdown.style.display === '';
        notifDropdown.style.display = isHidden ? 'block' : 'none';
      }
    });

    document.addEventListener('click', (e) => {
      if (notifDropdown && !notifDropdown.contains(e.target) && e.target !== bellBtn) {
        notifDropdown.style.display = 'none';
      }
    });

    markAllReadBtn?.addEventListener('click', async () => {
      try {
        await api('/api/parent-control/alerts/mark-read', { method: 'POST' });
        loadAlerts();
        showToast('All alerts marked as read', 'info', 2000);
      } catch (err) {
        showToast('Failed to clear alerts', 'danger');
      }
    });
  }

  function navigateTo(sectionId) {
    const targetSection = $(`#${sectionId}`);
    if (!targetSection) return;

    // Update nav links active class
    $$('.sidebar .nav-item').forEach(item => {
      item.classList.toggle('active', item.dataset.section === sectionId);
    });

    // Update section active class
    $$('.page-section').forEach(sec => {
      sec.classList.toggle('active-section', sec.id === sectionId);
    });

    state.activeSection = sectionId;
    window.scrollTo({ top: 0, behavior: 'smooth' });

    // Load data for this section
    loadSectionData(sectionId);
  }

  function loadSectionData(sectionId) {
    switch (sectionId) {
      case 'dashboard':
        loadCategories();
        loadDashboard();
        break;
      case 'expenses':
        loadCategories();
        loadExpenses();
        break;
      case 'budgets':
        loadCategories();
        loadBudgets();
        break;
      case 'splits':
        loadSplitBills();
        break;
      case 'lending':
        loadLending();
        break;
      case 'statements':
        loadStatementHistory();
        break;
      case 'parent':
        loadParentControl();
        break;
      case 'activity-log':
        loadActivityLog();
        break;
      case 'settings':
        loadSettings();
        break;
    }
  }

  // --- NOTIFICATION & ALERTS MODULE ---
  async function loadAlerts() {
    try {
      const alerts = await api('/api/parent-control/alerts');
      state.alerts = Array.isArray(alerts) ? alerts : [];

      const badge = $('#notifBadge');
      const list = $('#notifList');

      const unreadCount = state.alerts.filter(a => !a.isRead).length;
      if (badge) {
        if (unreadCount > 0) {
          badge.textContent = unreadCount > 9 ? '9+' : unreadCount;
          badge.style.display = 'inline-block';
        } else {
          badge.style.display = 'none';
        }
      }

      if (list) {
        if (state.alerts.length === 0) {
          list.innerHTML = `<p class="muted" style="font-size: 12px; text-align: center; padding: 12px 0;">No active alerts.</p>`;
        } else {
          list.innerHTML = state.alerts.slice(0, 8).map(alert => {
            const isDanger = alert.severity === 'DANGER' || alert.severity === 'HIGH';
            return `
              <div style="padding: 8px 10px; border-radius: 8px; background: ${isDanger ? '#fff5f5' : '#fcfcf9'}; border-left: 3px solid ${isDanger ? 'var(--danger)' : 'var(--gold)'}; font-size: 12px;">
                <div style="font-weight: 600; color: ${isDanger ? 'var(--danger)' : 'var(--dark)'}; display: flex; justify-content: space-between;">
                  <span>${escapeHTML(alert.title || 'Alert')}</span>
                  <small style="color: var(--muted); font-size: 10px;">${formatDateTime(alert.createdAt)}</small>
                </div>
                <p style="margin: 4px 0 0; color: var(--muted); font-size: 11px;">${escapeHTML(alert.message || '')}</p>
              </div>
            `;
          }).join('');
        }
      }
    } catch (e) {
      // Non-fatal
    }
  }

  // --- 1. DASHBOARD MODULE ---
  function initDashboard() {
    const periodSelect = $('#dashboardPeriodSelect');
    periodSelect?.addEventListener('change', () => {
      state.dashboardPeriod = periodSelect.value;
      loadDashboard();
    });

    const overviewToggle = $('#overviewViewToggle');
    overviewToggle?.addEventListener('change', () => {
      state.overviewMode = overviewToggle.value;
      updateSpendingOverviewChart(state.overviewMode);
    });
  }

  async function loadDashboard() {
    try {
      const data = await api(`/api/dashboard?period=${encodeURIComponent(state.dashboardPeriod)}`);
      state.dashboardData = data;

      // 1. Total spent card
      const totalSpent = Number(data.totalSpent ?? data.totalExpenses ?? 0);
      const budgetLimit = Number(data.budgetLimit || (state.currentUser && state.currentUser.monthlyBudget) || 40000);
      const spentPct = budgetLimit > 0 ? Math.min(100, (totalSpent / budgetLimit) * 100) : 0;

      const dashTotalSpent = $('#dashTotalSpent');
      const dashSpentSub = $('#dashSpentSub');
      const dashSpentProgress = $('#dashSpentProgress');
      if (dashTotalSpent) dashTotalSpent.textContent = formatMoney(totalSpent);
      if (dashSpentSub) dashSpentSub.textContent = `of ${formatMoney(budgetLimit)} monthly budget`;
      if (dashSpentProgress) dashSpentProgress.style.width = `${spentPct.toFixed(1)}%`;

      // 2. Remaining card
      const remaining = Number(data.remainingBudget ?? (budgetLimit - totalSpent));
      const remPct = budgetLimit > 0 ? Math.max(0, 100 - spentPct) : 0;
      const dashRemaining = $('#dashRemaining');
      const dashRemainingSub = $('#dashRemainingSub');
      const dashRemainingProgress = $('#dashRemainingProgress');
      if (dashRemaining) dashRemaining.textContent = formatMoney(remaining);
      if (dashRemainingSub) dashRemainingSub.textContent = `${remPct.toFixed(1)}% of your budget left`;
      if (dashRemainingProgress) dashRemainingProgress.style.width = `${remPct.toFixed(1)}%`;

      // 3. Owed to you card
      const netLent = Number(data.netLent || 0);
      const owedList = Array.isArray(data.owedToYouList) ? data.owedToYouList : [];
      const dashOwedToYou = $('#dashOwedToYou');
      const dashOwedCount = $('#dashOwedCount');
      const dashOwedMiniList = $('#dashOwedMiniList');
      if (dashOwedToYou) dashOwedToYou.textContent = formatMoney(netLent);
      if (dashOwedCount) dashOwedCount.textContent = `${owedList.length} ${owedList.length === 1 ? 'person owes' : 'people owe'} you`;
      if (dashOwedMiniList) {
        if (owedList.length === 0) {
          dashOwedMiniList.innerHTML = `<span class="muted" style="font-size: 11px;">No outstanding debts</span>`;
        } else {
          dashOwedMiniList.innerHTML = owedList.slice(0, 2).map(item => `
            <span>${escapeHTML(item.name || item.personName || 'Friend')} <b>${formatMoney(item.amount)}</b></span>
          `).join('');
        }
      }

      // 4. You owe card
      const netBorrowed = Number(data.netBorrowed || 0);
      const youOweList = Array.isArray(data.youOweList) ? data.youOweList : [];
      const dashYouOwe = $('#dashYouOwe');
      const dashOwingCount = $('#dashOwingCount');
      const dashOwingMiniList = $('#dashOwingMiniList');
      if (dashYouOwe) dashYouOwe.textContent = formatMoney(netBorrowed);
      if (dashOwingCount) dashOwingCount.textContent = `${youOweList.length} pending settlement${youOweList.length === 1 ? '' : 's'}`;
      if (dashOwingMiniList) {
        if (youOweList.length === 0) {
          dashOwingMiniList.innerHTML = `<span class="muted" style="font-size: 11px;">All settled up!</span>`;
        } else {
          dashOwingMiniList.innerHTML = youOweList.slice(0, 2).map(item => `
            <span>${escapeHTML(item.name || item.personName || 'Friend')} <b>${formatMoney(item.amount)}</b></span>
          `).join('');
        }
      }

      // 5. Spending overview bars (dynamic based on selected toggle)
      const activeOverviewMode = $('#overviewViewToggle')?.value || state.overviewMode || 'monthly';
      updateSpendingOverviewChart(activeOverviewMode);

      // 6. Category budget breakdown
      renderDashboardCategories(data.categoryBreakdowns || data.categoryBreakdown || []);

      // 7. Recent transactions
      renderDashboardRecentTransactions(data.recentTransactions || []);

      // 8. Budget alert card
      renderDashboardAlertCard(data);

    } catch (err) {
      console.error('Failed to load dashboard:', err);
      showToast('Could not refresh dashboard metrics', 'danger');
    }
  }

  function updateSpendingOverviewChart(mode) {
    if (!state.dashboardData) return;
    const modeVal = mode || $('#overviewViewToggle')?.value || 'monthly';
    const isWeekly = modeVal === 'weekly';

    // 1. Determine active dataset
    let dataset = isWeekly
      ? (state.dashboardData.spendingOverviewWeekly || [])
      : (state.dashboardData.spendingOverviewMonthly || state.dashboardData.spendingOverview || []);

    if (!Array.isArray(dataset) || dataset.length === 0) {
      if (isWeekly) {
        dataset = [
          { label: 'Mon', dayName: 'Monday', amount: 1200, height: 40, current: false },
          { label: 'Tue', dayName: 'Tuesday', amount: 2450, height: 65, current: false },
          { label: 'Wed', dayName: 'Wednesday', amount: 1800, height: 50, current: false },
          { label: 'Thu', dayName: 'Thursday', amount: 3200, height: 85, current: false },
          { label: 'Fri', dayName: 'Friday', amount: 3950, height: 95, current: true },
          { label: 'Sat', dayName: 'Saturday', amount: 2100, height: 60, current: false },
          { label: 'Sun', dayName: 'Sunday', amount: 950, height: 30, current: false }
        ];
      } else {
        dataset = [
          { label: '1', formattedDate: '1 Oct', amount: 1250, height: 45, current: false },
          { label: '5', formattedDate: '5 Oct', amount: 2850, height: 65, current: false },
          { label: '10', formattedDate: '10 Oct', amount: 1820, height: 40, current: false },
          { label: '15', formattedDate: '15 Oct', amount: 3450, height: 80, current: false },
          { label: '20', formattedDate: '20 Oct', amount: 4120, height: 90, current: false },
          { label: '24', formattedDate: '24 Oct', amount: 2150, height: 60, current: true },
          { label: '30', formattedDate: '30 Oct', amount: 980, height: 25, current: false }
        ];
      }
    }

    // 2. Update Header Subtitle, Badge, and Explanatory Texts
    const periodLabel = $('#chartPeriodLabel');
    const activeBadge = $('#chartActiveBadge');
    const descText = $('#chartDescriptionText');
    const hoverDetail = $('#chartHoverDetail');

    const now = new Date();
    const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
    const curMonthName = monthNames[now.getMonth()];
    const curYear = now.getFullYear();

    if (isWeekly) {
      if (periodLabel) periodLabel.textContent = `This Week (Mon – Sun) • Daily breakdown`;
      if (activeBadge) activeBadge.textContent = `Weekly View`;
      if (descText) descText.textContent = `Showing daily expenses for each day of the current week. Taller bars indicate higher spending days. The dark green bar highlights today.`;
    } else {
      if (periodLabel) periodLabel.textContent = `${curMonthName} ${curYear} • Milestone days breakdown`;
      if (activeBadge) activeBadge.textContent = `Monthly View`;
      if (descText) descText.textContent = `Showing daily expense totals across key milestone dates of ${curMonthName}. The dark green bar highlights peak/current spending.`;
    }

    if (hoverDetail) hoverDetail.textContent = 'Hover a bar for details';

    // 3. Update X-labels dynamically
    const xLabelsContainer = $('#overviewXLabels');
    if (xLabelsContainer) {
      xLabelsContainer.innerHTML = dataset.map(item => `<span>${escapeHTML(item.label || '')}</span>`).join('');
    }

    // 4. Calculate dynamic Y-labels based on max amount
    const maxAmount = Math.max(100, ...dataset.map(item => Number(item.amount || 0)));
    const yLabelsContainer = $('#overviewYLabels');
    if (yLabelsContainer) {
      const step = maxAmount / 4;
      const formatY = (val) => {
        if (val >= 1000) return `₹${(val / 1000).toFixed(val % 1000 === 0 ? 0 : 1)}k`;
        return `₹${Math.round(val)}`;
      };
      yLabelsContainer.innerHTML = `
        <span>${formatY(maxAmount)}</span>
        <span>${formatY(step * 3)}</span>
        <span>${formatY(step * 2)}</span>
        <span>${formatY(step * 1)}</span>
        <span>₹0</span>
      `;
    }

    // 5. Render Bars with interactive hover
    const barsContainer = $('#bars');
    if (!barsContainer) return;

    barsContainer.innerHTML = dataset.map((item, idx) => {
      const amt = Number(item.amount || 0);
      const pct = maxAmount > 0 ? Math.max(12, Math.min(100, Math.round((amt / maxAmount) * 100))) : 15;
      const isCurrent = Boolean(item.current) || (isWeekly ? (idx === 4) : (idx === dataset.length - 2));
      const fullLabel = item.dayName 
        ? `${item.dayName} (${item.label})` 
        : (item.formattedDate ? `Date: ${item.formattedDate}` : `Day ${item.label}`);
      const tooltipText = `${fullLabel}: ${formatMoney(amt)}`;

      return `
        <div 
          style="height: ${pct}%;" 
          class="bar ${isCurrent ? 'current' : ''}" 
          data-label="${escapeHTML(fullLabel)}" 
          data-amount="${formatMoney(amt)}"
          title="${escapeHTML(tooltipText)}"
        ></div>
      `;
    }).join('');

    // Attach interactive hover listeners to each bar
    barsContainer.querySelectorAll('.bar').forEach(barEl => {
      barEl.addEventListener('mouseenter', () => {
        const lbl = barEl.getAttribute('data-label');
        const amt = barEl.getAttribute('data-amount');
        if (hoverDetail && lbl && amt) {
          hoverDetail.innerHTML = `<b>${escapeHTML(lbl)}</b>: <span style="color:var(--green);font-weight:800;">${escapeHTML(amt)}</span>`;
          hoverDetail.style.background = '#eaf5ef';
        }
      });
      barEl.addEventListener('mouseleave', () => {
        if (hoverDetail) {
          hoverDetail.textContent = 'Hover a bar for details';
          hoverDetail.style.background = '#f4f7f5';
        }
      });
    });
  }

  function renderSpendingBars(overviewData) {
    const barsContainer = $('#bars');
    if (!barsContainer) return;

    if (!Array.isArray(overviewData) || overviewData.length === 0) {
      // Default 7 bars representing days/weeks
      overviewData = [
        { label: '1', height: 45, current: false },
        { label: '5', height: 60, current: false },
        { label: '10', height: 30, current: false },
        { label: '15', height: 80, current: false },
        { label: '20', height: 50, current: false },
        { label: '24', height: 95, current: true },
        { label: '30', height: 20, current: false }
      ];
    }

    barsContainer.innerHTML = overviewData.map((item, idx) => {
      const height = item.height || Math.min(100, Math.max(10, Math.round(Number(item.amount || 0) / 100)));
      const isCurrent = item.current || idx === overviewData.length - 2;
      return `<div style="height: ${height}%;" class="bar ${isCurrent ? 'current' : ''}" title="Day ${escapeHTML(item.label || '')}: ${item.amount ? formatMoney(item.amount) : ''}"></div>`;
    }).join('');
  }

  function renderDashboardCategories(breakdowns) {
    const list = $('#dashCategoryList');
    if (!list) return;

    if (!Array.isArray(breakdowns) || breakdowns.length === 0) {
      list.innerHTML = `<div style="padding: 20px; text-align: center; color: var(--muted); font-size: 13px;">No category spending recorded yet.</div>`;
      return;
    }

    const colorClasses = ['green', 'gold', 'blue', 'purple', 'teal'];

    list.innerHTML = breakdowns.slice(0, 5).map((cat, idx) => {
      const spent = Number(cat.spent || cat.amount || 0);
      const limit = Number(cat.limit || cat.budget || 5000);
      const pct = limit > 0 ? (spent / limit) * 100 : 0;
      const isDanger = pct >= 100;
      const colorClass = colorClasses[idx % colorClasses.length];

      let icon = '⌂';
      const nameLower = (cat.name || cat.category || '').toLowerCase();
      if (nameLower.includes('food') || nameLower.includes('dining')) icon = '🍴';
      else if (nameLower.includes('shop')) icon = '◇';
      else if (nameLower.includes('travel') || nameLower.includes('transport')) icon = '✈';
      else if (nameLower.includes('bill') || nameLower.includes('util')) icon = '▤';
      else if (nameLower.includes('game') || nameLower.includes('app')) icon = '🎮';
      else if (nameLower.includes('health')) icon = '✚';

      return `
        <div class="cat">
          <span class="cat-icon ${colorClass}">${icon}</span>
          <div class="cat-info">
            <b>${escapeHTML(cat.name || cat.category || 'Other')}</b>
            <small>${formatMoney(spent)} of ${formatMoney(limit)}</small>
            <div class="progress ${isDanger ? 'danger' : ''}">
              <i style="width: ${Math.min(100, pct).toFixed(1)}%;"></i>
            </div>
          </div>
          <strong class="${isDanger ? 'danger-text' : ''}">${pct.toFixed(0)}%</strong>
        </div>
      `;
    }).join('');
  }

  function renderDashboardRecentTransactions(transactions) {
    const container = $('#transactions');
    if (!container) return;

    if (!Array.isArray(transactions) || transactions.length === 0) {
      container.innerHTML = `<div style="padding: 24px; text-align: center; color: var(--muted); font-size: 13px;">No recent transactions found.</div>`;
      return;
    }

    container.innerHTML = transactions.slice(0, 5).map(tx => {
      const isCredit = (tx.direction || tx.type || '').toUpperCase() === 'CREDIT' || (tx.type || '').toUpperCase() === 'INCOME';
      const isTransfer = Boolean(tx.isTransfer || tx.transfer);
      const amount = Number(tx.amount || 0);

      let icon = '💳';
      const cat = (tx.category || '').toLowerCase();
      if (cat.includes('food')) icon = '🍔';
      else if (cat.includes('travel') || cat.includes('uber') || cat.includes('cab')) icon = '🚕';
      else if (cat.includes('shop')) icon = '🛍';
      else if (cat.includes('salary') || cat.includes('income')) icon = '💰';
      else if (isTransfer) icon = '⇄';

      return `
        <div class="tx-item">
          <div class="tx-left">
            <span class="tx-icon">${icon}</span>
            <div>
              <b>${escapeHTML(tx.merchant || tx.description || 'Transaction')}</b>
              <small>${escapeHTML(tx.category || 'General')} · ${formatDate(tx.date)}</small>
            </div>
          </div>
          <strong class="${isCredit ? 'credit' : ''}">
            ${isCredit ? '+' : '-'}${formatMoney(amount)}
          </strong>
        </div>
      `;
    }).join('');
  }

  function renderDashboardAlertCard(dashData) {
    const alertCard = $('#dashAlertCard');
    const alertTitle = $('#alertCardTitle');
    const alertSub = $('#alertCardSub');
    const alertNumber = $('#alertCardNumber');
    const alertDesc = $('#alertCardDesc');
    if (!alertCard) return;

    const breakdowns = dashData.categoryBreakdowns || [];
    const overBudgetCat = breakdowns.find(c => Number(c.spent || 0) > Number(c.limit || 0));

    if (overBudgetCat) {
      const overAmount = Number(overBudgetCat.spent || 0) - Number(overBudgetCat.limit || 0);
      alertTitle.textContent = 'Budget alert';
      alertSub.textContent = `${overBudgetCat.name || overBudgetCat.category} is over your limit`;
      alertNumber.innerHTML = `${formatMoney(overAmount)} <small>over budget</small>`;
      alertDesc.textContent = `Your ${overBudgetCat.name || overBudgetCat.category} budget is ${formatMoney(overBudgetCat.limit)}. You've spent ${formatMoney(overBudgetCat.spent)} this month.`;
      alertCard.style.display = 'block';
    } else if (dashData.activeAlerts && dashData.activeAlerts.length > 0) {
      const first = dashData.activeAlerts[0];
      alertTitle.textContent = first.title || 'Security notice';
      alertSub.textContent = first.message || 'Parent restriction active';
      alertNumber.innerHTML = `Active <small>alert</small>`;
      alertDesc.textContent = first.message || 'Review your parental controls and activity log.';
      alertCard.style.display = 'block';
    } else {
      // Default healthy status
      alertTitle.textContent = 'All budgets on track';
      alertSub.textContent = 'Spending within monthly plan';
      alertNumber.innerHTML = `✓ <small>Healthy</small>`;
      alertDesc.textContent = `You've used ${Number(dashData.spendingPercentage || 0).toFixed(0)}% of your total budget. Keep it up!`;
      alertCard.style.display = 'block';
    }
  }

  // --- 2. EXPENSES MODULE ---
  function initExpenses() {
    const searchInput = $('#expenseSearch');
    const categoryFilter = $('#expenseCategoryFilter');
    const clearBtn = $('#clearExpenseFiltersBtn');
    const exportBtn = $('#exportExpensesCsvBtn');

    let debounceTimer = null;
    searchInput?.addEventListener('input', () => {
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => loadExpenses(), 300);
    });

    categoryFilter?.addEventListener('change', () => loadExpenses());

    clearBtn?.addEventListener('click', () => {
      if (searchInput) searchInput.value = '';
      if (categoryFilter) categoryFilter.value = '';
      loadExpenses();
    });

    exportBtn?.addEventListener('click', () => {
      window.location.href = `${API_BASE}/api/transactions/export/csv`;
      showToast('Downloading expenses CSV…', 'info', 2000);
    });
  }

  async function loadExpenses() {
    const rowsContainer = $('#expenseRows');
    const countEl = $('#expenseTotalCount');
    if (!rowsContainer) return;

    const search = $('#expenseSearch')?.value.trim() || '';
    const category = $('#expenseCategoryFilter')?.value || '';

    try {
      let url = `/api/transactions?`;
      if (search) url += `search=${encodeURIComponent(search)}&`;
      if (category) url += `category=${encodeURIComponent(category)}&`;

      const data = await api(url);
      const transactions = Array.isArray(data) ? data : (data.content || []);
      state.transactions = transactions;

      if (countEl) {
        countEl.textContent = `Showing ${transactions.length} transaction${transactions.length === 1 ? '' : 's'}`;
      }

      if (transactions.length === 0) {
        rowsContainer.innerHTML = `
          <div style="padding: 40px; text-align: center; color: var(--muted);">
            <p style="font-size: 15px; margin-bottom: 6px;">No transactions match your criteria.</p>
            <small>Try clearing your search query or add a new expense above.</small>
          </div>
        `;
        return;
      }

      rowsContainer.innerHTML = transactions.map(tx => {
        const isCredit = (tx.direction || tx.type || '').toUpperCase() === 'CREDIT' || (tx.type || '').toUpperCase() === 'INCOME';
        const isTransfer = Boolean(tx.isTransfer || tx.transfer);
        const amount = Number(tx.amount || 0);

        return `
          <div class="table-row">
            <div style="display: flex; align-items: center; gap: 10px;">
              <span class="badge ${isTransfer ? 'secondary' : (isCredit ? 'success' : 'neutral')}">
                ${isTransfer ? 'Transfer' : (isCredit ? 'Credit' : 'Debit')}
              </span>
              <div>
                <strong>${escapeHTML(tx.merchant || tx.description || 'Transaction')}</strong>
                ${tx.isDemoData || (tx.description && tx.description.includes('[DEMO]')) ? `<span class="badge warning" style="font-size: 10px; margin-left: 6px; padding: 2px 6px;">DEMO DATA</span>` : ''}
                ${tx.notes ? `<small style="display: block; color: var(--muted); font-size: 11px;">${escapeHTML(tx.notes)}</small>` : ''}
              </div>
            </div>
            <div>
              <span class="cat-pill">${escapeHTML(tx.category || 'Other')}</span>
            </div>
            <div class="muted">${formatDate(tx.date)}</div>
            <div style="text-align: right; font-weight: 600; color: ${isCredit ? 'var(--green)' : 'var(--dark)'};">
              ${isCredit ? '+' : '-'}${formatMoney(amount)}
            </div>
            <div style="text-align: right; display: flex; justify-content: flex-end; gap: 6px;">
              <button class="icon-btn small danger-text" title="Delete transaction" onclick="window.spendlyDeleteTransaction(${tx.id})">🗑</button>
            </div>
          </div>
        `;
      }).join('');

    } catch (err) {
      console.error('Failed to load expenses:', err);
      rowsContainer.innerHTML = `<div style="padding: 24px; text-align: center; color: var(--danger);">Failed to load transactions: ${escapeHTML(err.message)}</div>`;
    }
  }

  window.spendlyDeleteTransaction = async function (id) {
    if (!confirm('Are you sure you want to delete this transaction?')) return;
    try {
      await api(`/api/transactions/${id}`, { method: 'DELETE' });
      showToast('Transaction deleted successfully', 'success');
      loadExpenses();
      loadDashboard();
    } catch (err) {
      showToast(err.message || 'Failed to delete transaction', 'danger');
    }
  };

  // --- 3. BUDGETS MODULE ---
  function initBudgets() {
    // Modal submission handled in modals section
  }

  async function loadBudgets() {
    const grid = $('#budgetsGrid');
    if (!grid) return;

    try {
      const data = await api('/api/budgets');
      const budgets = Array.isArray(data) ? data : (data.content || []);
      state.budgets = budgets;

      if (budgets.length === 0) {
        grid.innerHTML = `
          <div class="panel" style="grid-column: 1 / -1; padding: 40px; text-align: center; color: var(--muted);">
            <h3>No category budgets defined</h3>
            <p style="margin: 8px 0 16px;">Set monthly limits to get automated warnings and alerts before overspending.</p>
            <button class="primary" data-open-modal="budgetModal">+ Add category budget</button>
          </div>
        `;
        return;
      }

      grid.innerHTML = budgets.map(b => {
        const spent = Number(b.spentAmount || b.currentSpent || 0);
        const limit = Number(b.monthlyLimit || b.amount || 1);
        const pct = limit > 0 ? (spent / limit) * 100 : 0;
        const isOver = pct >= 100;
        const isWarning = pct >= 80 && !isOver;

        let badgeHtml = '';
        if (isOver) {
          badgeHtml = `<span class="badge danger">Over limit (${pct.toFixed(0)}%)</span>`;
        } else if (isWarning) {
          badgeHtml = `<span class="badge warning">Warning (${pct.toFixed(0)}%)</span>`;
        } else {
          badgeHtml = `<span class="badge success">On track</span>`;
        }

        return `
          <article class="panel budget-card">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
              <div>
                <h3 style="font-size: 16px; margin-bottom: 4px;">${escapeHTML(b.categoryName || b.category)}</h3>
                <span class="muted" style="font-size: 12px;">Monthly ceiling</span>
              </div>
              <div style="display: flex; align-items: center; gap: 8px;">
                ${badgeHtml}
                <button class="icon-btn small danger-text" title="Delete budget" onclick="window.spendlyDeleteBudget(${b.id})">🗑</button>
              </div>
            </div>

            <div style="margin: 14px 0 8px;">
              <strong style="font-size: 22px; font-family: 'Playfair Display', Georgia, serif; color: ${isOver ? 'var(--danger)' : 'var(--dark)'};">
                ${formatMoney(spent)}
              </strong>
              <small class="muted" style="font-size: 13px;"> / ${formatMoney(limit)}</small>
            </div>

            <div class="progress ${isOver ? 'danger' : ''}" style="height: 8px;">
              <i style="width: ${Math.min(100, pct).toFixed(1)}%;"></i>
            </div>

            <div style="display: flex; justify-content: space-between; margin-top: 10px; font-size: 11px; color: var(--muted);">
              <span>${pct.toFixed(0)}% used</span>
              <span>${formatMoney(Math.max(0, limit - spent))} remaining</span>
            </div>
          </article>
        `;
      }).join('');

    } catch (err) {
      console.error('Failed to load budgets:', err);
      grid.innerHTML = `<div class="panel" style="grid-column: 1 / -1; color: var(--danger); text-align: center; padding: 20px;">Failed to load budgets: ${escapeHTML(err.message)}</div>`;
    }
  }

  window.spendlyDeleteBudget = async function (id) {
    if (!confirm('Remove this category budget limit?')) return;
    try {
      await api(`/api/budgets/${id}`, { method: 'DELETE' });
      showToast('Budget limit removed', 'info');
      loadBudgets();
      loadDashboard();
    } catch (err) {
      showToast(err.message || 'Failed to remove budget', 'danger');
    }
  };

  // --- 4. SPLIT BILLS MODULE ---
  function initSplitBills() {
    $('#viewSettlementsBtn')?.addEventListener('click', () => {
      showToast('All participant balances are displayed on each bill card.', 'info', 3000);
    });
  }

  async function loadSplitBills() {
    const list = $('#splitGroupsList');
    const netBalanceEl = $('#splitNetBalance');
    const netSummaryEl = $('#splitNetSummary');
    if (!list) return;

    try {
      // 1. Net balance summary
      try {
        const netData = await api('/api/split-bills/net-balance');
        const net = Number(netData.netBalance || 0);
        if (netBalanceEl) {
          netBalanceEl.textContent = (net >= 0 ? '+' : '') + formatMoney(net);
          netBalanceEl.style.color = net >= 0 ? 'var(--green)' : 'var(--danger)';
        }
        if (netSummaryEl) {
          netSummaryEl.textContent = netData.summary || (net >= 0 ? 'You are owed more than you owe.' : 'You owe more than you are owed.');
        }
      } catch (e) {
        // Fallback default
      }

      // 2. Groups list
      const data = await api('/api/split-bills');
      const bills = Array.isArray(data) ? data : (data.content || []);
      state.splitBills = bills;

      if (bills.length === 0) {
        list.innerHTML = `
          <div style="padding: 30px; text-align: center; color: var(--muted);">
            <p>No active split bills or shared expenses.</p>
            <button class="primary" data-open-modal="splitModal" style="margin-top: 12px;">+ Create first split bill</button>
          </div>
        `;
        return;
      }

      list.innerHTML = bills.map(bill => {
        const participants = Array.isArray(bill.participants) ? bill.participants : [];
        const total = Number(bill.totalAmount || 0);

        return `
          <div class="panel split-group-card" style="margin-bottom: 16px; border: 1px solid var(--line); border-radius: 12px; padding: 16px;">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
              <div>
                <strong style="font-size: 16px; color: var(--green);">${escapeHTML(bill.title)}</strong>
                <small class="muted" style="display: block; font-size: 11px;">Created ${formatDate(bill.createdAt || bill.date)}</small>
              </div>
              <div style="display: flex; align-items: center; gap: 10px;">
                <strong style="font-size: 17px; font-family: 'Playfair Display', Georgia, serif;">${formatMoney(total)}</strong>
                <button class="icon-btn small danger-text" title="Delete split bill group" onclick="window.spendlyDeleteSplitBill(${bill.id})">🗑</button>
              </div>
            </div>

            <div style="display: grid; gap: 8px;">
              ${participants.map(p => {
                const isSettled = Boolean(p.isSettled || p.settled);
                const share = Number(p.shareAmount || p.amount || 0);
                return `
                  <div style="display: flex; justify-content: space-between; align-items: center; padding: 6px 10px; background: #fafaf7; border-radius: 8px; font-size: 12px;">
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span class="avatar small">${getInitials(p.name)}</span>
                      <span>${escapeHTML(p.name)}</span>
                    </div>
                    <div style="display: flex; align-items: center; gap: 10px;">
                      <b style="color: ${isSettled ? 'var(--muted)' : 'var(--dark)'};">${formatMoney(share)}</b>
                      ${isSettled 
                        ? `<span class="badge success">Settled ✓</span>` 
                        : `<button class="secondary small" onclick="window.spendlySettleParticipant(${bill.id}, ${p.id})">Settle</button>`
                      }
                    </div>
                  </div>
                `;
              }).join('')}
            </div>
          </div>
        `;
      }).join('');

    } catch (err) {
      console.error('Failed to load split bills:', err);
      list.innerHTML = `<div style="padding: 20px; color: var(--danger); text-align: center;">Failed to load split bills: ${escapeHTML(err.message)}</div>`;
    }
  }

  window.spendlySettleParticipant = async function (billId, participantId) {
    try {
      await api(`/api/split-bills/${billId}/settle/${participantId}`, { method: 'POST' });
      showToast('Participant share settled successfully', 'success');
      loadSplitBills();
      loadDashboard();
    } catch (err) {
      showToast(err.message || 'Failed to settle participant share', 'danger');
    }
  };

  window.spendlyDeleteSplitBill = async function (billId) {
    if (!confirm('Are you sure you want to delete this split bill group?')) return;
    try {
      await api(`/api/split-bills/${billId}`, { method: 'DELETE' });
      showToast('Split bill group deleted successfully', 'success');
      loadSplitBills();
      loadDashboard();
    } catch (err) {
      showToast(err.message || 'Failed to delete split bill group', 'danger');
    }
  };

  // --- 5. LENDING & BORROWING MODULE ---
  function initLending() {
    // Modal submissions handled in modals section
  }

  async function loadLending() {
    const rows = $('#lendingRows');
    const lentTotal = $('#lentTotalAmount');
    const lentOut = $('#lentOutstandingAmount');
    const borrowedTotal = $('#borrowedTotalAmount');
    const borrowedOut = $('#borrowedOutstandingAmount');
    if (!rows) return;

    try {
      // 1. Summary totals
      try {
        const summary = await api('/api/obligations/summary');
        if (lentTotal) lentTotal.textContent = formatMoney(summary.totalLent || 0);
        if (lentOut) lentOut.textContent = `${formatMoney(summary.lentOutstanding || 0)} still outstanding`;
        if (borrowedTotal) borrowedTotal.textContent = formatMoney(summary.totalBorrowed || 0);
        if (borrowedOut) borrowedOut.textContent = `${formatMoney(summary.borrowedOutstanding || 0)} still to repay`;
      } catch (e) {}

      // 2. Obligations list
      const data = await api('/api/obligations');
      const obligations = Array.isArray(data) ? data : (data.content || []);
      state.obligations = obligations;

      if (obligations.length === 0) {
        rows.innerHTML = `
          <div style="padding: 30px; text-align: center; color: var(--muted);">
            <p>No active loans or borrowings on record.</p>
            <button class="primary" data-open-modal="lendingModal" style="margin-top: 10px;">+ Record money</button>
          </div>
        `;
        return;
      }

      rows.innerHTML = obligations.map(ob => {
        const isLent = (ob.type || '').toUpperCase() === 'LENT';
        const isSettled = (ob.status || '').toUpperCase() === 'SETTLED' || Number(ob.remainingBalance || 0) <= 0;
        const remaining = Number(ob.remainingBalance ?? ob.amount);

        return `
          <div class="table-row">
            <div>
              <strong>${escapeHTML(ob.personName)}</strong>
              ${ob.notes ? `<small class="muted" style="display: block; font-size: 11px;">${escapeHTML(ob.notes)}</small>` : ''}
            </div>
            <div>
              <span class="badge ${isLent ? 'success' : 'warning'}">
                ${isLent ? '↗ Lent (Owed to you)' : '↙ Borrowed (You owe)'}
              </span>
            </div>
            <div>
              <strong>${formatMoney(remaining)}</strong>
              <small class="muted" style="display: block; font-size: 11px;">Due: ${ob.dueDate ? formatDate(ob.dueDate) : 'No due date'}</small>
            </div>
            <div>
              <span class="badge ${isSettled ? 'success' : 'neutral'}">
                ${isSettled ? 'Settled ✓' : 'Active'}
              </span>
            </div>
            <div style="text-align: right; display: flex; justify-content: flex-end; align-items: center; gap: 6px;">
              ${!isSettled ? `
                <button class="secondary small" onclick="window.spendlyOpenRepaymentModal(${ob.id}, ${remaining})">Repay</button>
                <button class="secondary small" onclick="window.spendlySettleObligation(${ob.id})">Settle</button>
              ` : `
                <span class="muted" style="font-size: 11px; margin-right: 4px;">Complete</span>
              `}
              <button class="icon-btn small danger-text" title="Delete record" onclick="window.spendlyDeleteObligation(${ob.id})">🗑</button>
            </div>
          </div>
        `;
      }).join('');

    } catch (err) {
      console.error('Failed to load lending records:', err);
      rows.innerHTML = `<div style="padding: 20px; color: var(--danger); text-align: center;">Failed to load lending records: ${escapeHTML(err.message)}</div>`;
    }
  }

  window.spendlyOpenRepaymentModal = function (obligationId, currentBalance) {
    const modal = $('#repaymentModal');
    const idInput = $('#repayObligationId');
    const amountInput = $('#repayAmount');

    if (idInput) idInput.value = obligationId;
    if (amountInput) {
      amountInput.value = currentBalance;
      amountInput.max = currentBalance;
    }

    openModal('repaymentModal');
  };

  window.spendlySettleObligation = async function (id) {
    if (!confirm('Mark this entire balance as fully settled?')) return;
    try {
      await api(`/api/obligations/${id}/settle`, { method: 'POST' });
      showToast('Obligation settled successfully', 'success');
      loadLending();
      loadDashboard();
    } catch (err) {
      showToast(err.message || 'Failed to settle obligation', 'danger');
    }
  };

  window.spendlyDeleteObligation = async function (id) {
    if (!confirm('Are you sure you want to delete this lending/borrowing record?')) return;
    try {
      await api(`/api/obligations/${id}`, { method: 'DELETE' });
      showToast('Record deleted successfully', 'success');
      loadLending();
      loadDashboard();
    } catch (err) {
      showToast(err.message || 'Failed to delete record', 'danger');
    }
  };

  // --- 6. BANK STATEMENTS MODULE ---
  function initBankStatements() {
    const dropZone = $('#statementDropZone');
    const fileInput = $('#realStatementFileInput');
    const chooseBtn = $('#chooseFileBtn');
    const refreshHistoryBtn = $('#refreshStatementHistoryBtn');

    chooseBtn?.addEventListener('click', () => fileInput?.click());
    dropZone?.addEventListener('click', (e) => {
      if (e.target !== chooseBtn) fileInput?.click();
    });

    // Drag and drop events
    ['dragenter', 'dragover'].forEach(eventName => {
      dropZone?.addEventListener(eventName, (e) => {
        e.preventDefault();
        dropZone.style.borderColor = 'var(--gold)';
        dropZone.style.background = '#f2efe6';
      });
    });

    ['dragleave', 'drop'].forEach(eventName => {
      dropZone?.addEventListener(eventName, (e) => {
        e.preventDefault();
        dropZone.style.borderColor = 'var(--line)';
        dropZone.style.background = '#fff';
      });
    });

    dropZone?.addEventListener('drop', (e) => {
      const files = e.dataTransfer.files;
      if (files && files.length > 0) {
        handleStatementUpload(files[0]);
      }
    });

    fileInput?.addEventListener('change', () => {
      if (fileInput.files && fileInput.files.length > 0) {
        handleStatementUpload(fileInput.files[0]);
      }
    });

    refreshHistoryBtn?.addEventListener('click', () => {
      loadStatementHistory();
      showToast('Statement history refreshed', 'info', 1500);
    });
  }

  async function handleStatementUpload(file) {
    const statusEl = $('#uploadFileStatus');
    const summaryCard = $('#importSummaryCard');

    if (!file) return;

    if (statusEl) {
      statusEl.textContent = `Uploading and parsing ${file.name}…`;
      statusEl.style.color = 'var(--green)';
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
      const summary = await api('/api/statements/import', {
        method: 'POST',
        body: formData
      });

      if (statusEl) {
        statusEl.textContent = 'Statement imported successfully.';
        statusEl.style.color = 'var(--green)';
      }

      // Display summary card
      if (summaryCard) {
        summaryCard.classList.remove('hidden');
        $('#summaryFilename').textContent = `Uploaded: ${file.name}`;
        $('#summaryDateRange').textContent = summary.dateRange || 'Statement Transactions Processed';
        $('#badgeAccepted').textContent = `✓ ${summary.acceptedCount || 0} Accepted`;
        $('#badgeDuplicate').textContent = `⧉ ${summary.duplicateCount || 0} Duplicates`;
        $('#badgeRejected').textContent = `✕ ${summary.rejectedCount || 0} Rejected`;
        $('#summaryNotes').textContent = summary.notes || `SHA-256 fingerprint deduplication verified. Internal transfers are isolated automatically.`;
      }

      showToast(`Statement processed: ${summary.acceptedCount || 0} added, ${summary.duplicateCount || 0} duplicates skipped.`, 'success', 5000);

      loadStatementHistory();
      loadDashboard();
      loadExpenses();
    } catch (err) {
      console.error('Statement upload error:', err);
      if (statusEl) {
        statusEl.textContent = `Upload failed: ${err.message}`;
        statusEl.style.color = 'var(--danger)';
      }
      showToast(err.message || 'Failed to parse statement', 'danger', 6000);
    }
  }

  async function loadStatementHistory() {
    const rows = $('#statementHistoryRows');
    if (!rows) return;

    try {
      const data = await api('/api/statements/history');
      const history = Array.isArray(data) ? data : (data.content || []);

      if (history.length === 0) {
        rows.innerHTML = `<div style="padding: 24px; text-align: center; color: var(--muted); font-size: 13px;">No statement imports on record yet.</div>`;
        return;
      }

      rows.innerHTML = history.map(item => `
        <div class="table-row">
          <div>
            <strong>${escapeHTML(item.fileName || item.filename)}</strong>
            <small class="muted" style="display: block; font-size: 10px;">${escapeHTML(item.fileHash ? item.fileHash.substring(0, 16) + '…' : '')}</small>
          </div>
          <div>
            <span class="cat-pill">${escapeHTML(item.fileType || 'CSV')}</span>
          </div>
          <div class="muted">${formatDateTime(item.importedAt || item.createdAt)}</div>
          <div>
            <span class="badge ${item.status === 'SUCCESS' ? 'success' : 'warning'}">
              ${escapeHTML(item.status || 'PROCESSED')}
            </span>
          </div>
          <div style="text-align: right; font-weight: 500;">
            <span style="color: var(--green);">${item.acceptedCount || 0} accepted</span> / 
            <span style="color: var(--gold);">${item.duplicateCount || 0} dup</span>
          </div>
        </div>
      `).join('');

    } catch (err) {
      console.error('Failed to load statement history:', err);
      rows.innerHTML = `<div style="padding: 20px; color: var(--danger); text-align: center;">Failed to load import history: ${escapeHTML(err.message)}</div>`;
    }
  }

  // --- 7. PARENT CONTROL MODULE ---
  function initParentControl() {
    // Category toggle switches
    const switches = [
      { id: 'ruleShopping', category: 'Shopping' },
      { id: 'ruleEntertainment', category: 'Entertainment' },
      { id: 'ruleFood', category: 'Food & Dining' },
      { id: 'ruleGaming', category: 'Gaming / Apps' }
    ];

    switches.forEach(sw => {
      const checkbox = $(`#${sw.id}`);
      if (!checkbox) return;

      checkbox.addEventListener('change', async () => {
        const isAllowed = checkbox.checked;
        const category = sw.category;

        try {
          await api(`/api/parent-control/rules/${encodeURIComponent(category)}`, {
            method: 'PUT',
            body: JSON.stringify({ isAllowed })
          });

          const msg = isAllowed
            ? `Permission granted: ${category} spending is now allowed.`
            : `Permission restricted: ${category} spending is now BLOCKED.`;
          showToast(msg, isAllowed ? 'success' : 'warning', 3500);

          // Refresh logs & alerts
          loadParentControl();
          loadAlerts();
        } catch (err) {
          // Revert checkbox on error
          checkbox.checked = !isAllowed;
          showToast(err.message || 'Failed to update category permission', 'danger');
        }
      });
    });
  }

  async function loadParentControl() {
    try {
      const summary = await api('/api/parent-control/summary');
      state.parentSummary = summary;

      const childName = $('#parentChildName');
      const monthlyLimit = $('#parentMonthlyLimit');
      const spentThisMonth = $('#parentSpentThisMonth');
      const remainingLimit = $('#parentRemainingLimit');
      const progress = $('#parentProgress');

      if (childName && summary.childName) {
        childName.textContent = `${summary.childName}'s account`;
      }
      if (monthlyLimit) monthlyLimit.textContent = formatMoney(summary.monthlyLimit || 12000);
      if (spentThisMonth) spentThisMonth.textContent = formatMoney(summary.spentThisMonth || 8240);
      if (remainingLimit) {
        const remaining = Number(summary.remainingLimit ?? (Number(summary.monthlyLimit || 12000) - Number(summary.spentThisMonth || 0)));
        remainingLimit.textContent = formatMoney(remaining);
      }
      if (progress) {
        const pct = summary.usagePercentage ?? (
          summary.monthlyLimit > 0 ? (summary.spentThisMonth / summary.monthlyLimit) * 100 : 0
        );
        progress.style.width = `${Math.min(100, pct).toFixed(1)}%`;
      }

      // Update rule switches
      const allRules = Array.isArray(summary.categoryRules) ? summary.categoryRules : (Array.isArray(summary.rules) ? summary.rules : []);
      const standardCats = ['Shopping', 'Entertainment', 'Food & Dining', 'Gaming / Apps'];

      allRules.forEach(rule => {
        const cat = rule.category || rule.categoryName;
        const isAllowed = rule.enabled !== undefined ? Boolean(rule.enabled) : Boolean(rule.isAllowed);
        let el = null;
        if (cat === 'Shopping') el = $('#ruleShopping');
        else if (cat === 'Entertainment') el = $('#ruleEntertainment');
        else if (cat === 'Food & Dining') el = $('#ruleFood');
        else if (cat === 'Gaming / Apps') el = $('#ruleGaming');

        if (el) {
          el.checked = isAllowed;
        }
      });

      // Render custom category rules
      const customContainer = $('#parentCustomRulesContainer');
      if (customContainer) {
        const customRules = allRules.filter(r => {
          const cName = r.category || r.categoryName || '';
          return !standardCats.some(s => s.toLowerCase() === cName.toLowerCase());
        });

        if (customRules.length === 0) {
          customContainer.innerHTML = '';
        } else {
          customContainer.innerHTML = customRules.map(rule => {
            const cat = rule.category || rule.categoryName;
            const isAllowed = rule.enabled !== undefined ? Boolean(rule.enabled) : Boolean(rule.isAllowed);
            const limit = rule.categoryLimit ? ` (Limit: ${formatMoney(rule.categoryLimit)})` : '';
            return `
              <div class="control-row" style="margin-top: 10px;">
                <div>
                  <span>${escapeHTML(cat)}</span>
                  <small style="display: block; font-size: 11px; color: ${isAllowed ? 'var(--green)' : 'var(--danger)'}; font-weight: 500;">
                    ${isAllowed ? 'Custom category: Allowed' + limit : 'Restricted: in-app purchases blocked'}
                  </small>
                </div>
                <label class="switch">
                  <input type="checkbox" ${isAllowed ? 'checked' : ''} data-custom-parent-rule="${escapeHTML(cat)}" />
                  <i></i>
                </label>
              </div>
            `;
          }).join('');

          customContainer.querySelectorAll('input[data-custom-parent-rule]').forEach(cb => {
            cb.addEventListener('change', async () => {
              const catName = cb.dataset.customParentRule;
              const newAllowed = cb.checked;
              try {
                await api(`/api/parent-control/rules/${encodeURIComponent(catName)}`, {
                  method: 'PUT',
                  body: JSON.stringify({ isAllowed: newAllowed, enabled: newAllowed })
                });
                showToast(`Permission for ${catName} switched to ${newAllowed ? 'Allowed' : 'Blocked'}`, newAllowed ? 'success' : 'warning');
                loadParentControl();
                loadAlerts();
              } catch (err) {
                cb.checked = !newAllowed;
                showToast(err.message || 'Failed to update rule', 'danger');
              }
            });
          });
        }
      }
    } catch (err) {
      console.error('Failed to load parent control summary:', err);
    }
  }

  // --- 8. PARENT CONTROL ACTIVITY LOG MODULE (Req 13.E) ---
  function initActivityLog() {
    const childSelect = $('#logChildSelect');
    const typeSelect = $('#logTypeSelect');
    const statusSelect = $('#logStatusSelect');
    const searchInput = $('#logSearchInput');
    const resetBtn = $('#resetLogFiltersBtn');
    const exportBtn = $('#exportAuditCsvBtn');
    const prevBtn = $('#prevLogPageBtn');
    const nextBtn = $('#nextLogPageBtn');
    const pageSizeSelect = $('#logPageSizeSelect');

    pageSizeSelect?.addEventListener('change', () => {
      state.activityLogPage = 0;
      loadActivityLog();
    });

    [childSelect, typeSelect, statusSelect].forEach(el => {
      el?.addEventListener('change', () => {
        state.activityLogPage = 0;
        loadActivityLog();
      });
    });

    let debounce = null;
    searchInput?.addEventListener('input', () => {
      clearTimeout(debounce);
      debounce = setTimeout(() => {
        state.activityLogPage = 0;
        loadActivityLog();
      }, 300);
    });

    resetBtn?.addEventListener('click', () => {
      if (childSelect) childSelect.value = '';
      if (typeSelect) typeSelect.value = '';
      if (statusSelect) statusSelect.value = '';
      if (searchInput) searchInput.value = '';
      if (pageSizeSelect) pageSizeSelect.value = '10';
      state.activityLogPage = 0;
      loadActivityLog();
    });

    exportBtn?.addEventListener('click', () => {
      window.location.href = `${API_BASE}/api/parent-control/activity-log/export`;
      showToast('Downloading Activity Log audit CSV…', 'info', 2000);
    });

    prevBtn?.addEventListener('click', () => {
      const totalPages = Math.max(1, state.activityLogTotalPages || 1);
      if (totalPages <= 1) {
        state.activityLogPage = 0;
        loadActivityLog();
        showToast('Refreshing Activity Log (Page 1 of 1)…', 'info', 1500);
        return;
      }
      if (state.activityLogPage > 0) {
        state.activityLogPage--;
      } else {
        state.activityLogPage = totalPages - 1;
        showToast(`Jumped to Page ${totalPages} of ${totalPages}`, 'info', 1500);
      }
      loadActivityLog();
    });

    nextBtn?.addEventListener('click', () => {
      const totalPages = Math.max(1, state.activityLogTotalPages || 1);
      if (totalPages <= 1) {
        state.activityLogPage = 0;
        loadActivityLog();
        showToast('Refreshing Activity Log (Page 1 of 1)…', 'info', 1500);
        return;
      }
      if (state.activityLogPage < totalPages - 1) {
        state.activityLogPage++;
      } else {
        state.activityLogPage = 0;
        showToast(`Wrapped around to Page 1 of ${totalPages}`, 'info', 1500);
      }
      loadActivityLog();
    });
  }

  async function loadActivityLog() {
    const rowsContainer = $('#activityLogRows');
    const summaryEl = $('#logPageSummary');
    const prevBtn = $('#prevLogPageBtn');
    const nextBtn = $('#nextLogPageBtn');
    const numbersContainer = $('#logPageNumbers');
    if (!rowsContainer) return;

    const child = $('#logChildSelect')?.value || '';
    const type = $('#logTypeSelect')?.value || '';
    const status = $('#logStatusSelect')?.value || '';
    const search = $('#logSearchInput')?.value.trim() || '';
    const pageSize = Number($('#logPageSizeSelect')?.value || 5);

    try {
      let url = `/api/parent-control/activity-log?page=${state.activityLogPage}&size=${pageSize}&`;
      if (child) url += `childAccount=${encodeURIComponent(child)}&`;
      if (type) url += `activityType=${encodeURIComponent(type)}&`;
      if (status) url += `status=${encodeURIComponent(status)}&`;
      if (search) url += `search=${encodeURIComponent(search)}&`;

      const data = await api(url);
      const logs = Array.isArray(data) ? data : (data.content || []);
      state.activityLogs = logs;
      state.activityLogTotalPages = Math.max(1, data.totalPages || 1);

      const currentPage = state.activityLogPage;
      const totalPages = state.activityLogTotalPages;
      const totalEvents = data.totalElements ?? logs.length;

      // Keep buttons permanently enabled and interactive
      if (prevBtn) {
        prevBtn.disabled = false;
        prevBtn.removeAttribute('disabled');
        prevBtn.title = 'Previous page';
      }
      if (nextBtn) {
        nextBtn.disabled = false;
        nextBtn.removeAttribute('disabled');
        nextBtn.title = 'Next page';
      }

      if (summaryEl) {
        summaryEl.textContent = `Page ${currentPage + 1} of ${totalPages} (${totalEvents} event${totalEvents === 1 ? '' : 's'})`;
      }

      if (numbersContainer) {
        numbersContainer.innerHTML = '';
        for (let p = 0; p < totalPages; p++) {
          const isCur = p === currentPage;
          const pageBtn = document.createElement('button');
          pageBtn.type = 'button';
          pageBtn.className = `page-num-btn ${isCur ? 'active' : ''}`;
          pageBtn.textContent = p + 1;
          pageBtn.title = `Go to Page ${p + 1}`;
          pageBtn.addEventListener('click', () => {
            state.activityLogPage = p;
            loadActivityLog();
          });
          numbersContainer.appendChild(pageBtn);
        }
      }

      if (logs.length === 0) {
        rowsContainer.innerHTML = `
          <div style="padding: 30px; text-align: center; color: var(--muted); font-size: 13px;">
            No activity log events match your filters.
          </div>
        `;
        return;
      }

      rowsContainer.innerHTML = logs.map(log => {
        const isBlocked = log.status === 'BLOCKED';
        const isFlagged = log.status === 'FLAGGED' || log.status === 'WARNING';
        const statusBadgeClass = isBlocked ? 'danger' : (isFlagged ? 'warning' : 'success');

        return `
          <div class="table-row" style="grid-template-columns: 80px 1.6fr 1.6fr 1fr 1fr 1fr;">
            <div style="font-family: monospace; font-size: 11px; color: var(--muted);">#${log.id}</div>
            <div style="font-size: 12px; color: var(--dark);">${formatDateTime(log.createdAt || log.timestamp)}</div>
            <div>
              <strong style="display: block; font-size: 12px; color: ${isBlocked ? 'var(--danger)' : 'var(--dark)'};">
                ${escapeHTML(log.description || log.activityType)}
              </strong>
              ${log.categoryName ? `<span class="cat-pill" style="margin-top: 4px; display: inline-block;">${escapeHTML(log.categoryName)}</span>` : ''}
            </div>
            <div style="font-weight: 600;">
              ${log.amount ? formatMoney(log.amount) : '—'}
            </div>
            <div>
              <span class="badge ${statusBadgeClass}">
                ${escapeHTML(log.status || 'COMPLETED')}
              </span>
            </div>
            <div style="text-align: right; font-size: 11px;">
              <strong>${escapeHTML(log.actor || 'System')}</strong>
              <small class="muted" style="display: block;">${escapeHTML(log.source || 'Web')}</small>
            </div>
          </div>
        `;
      }).join('');

    } catch (err) {
      console.error('Failed to load activity log:', err);
      rowsContainer.innerHTML = `<div style="padding: 20px; color: var(--danger); text-align: center;">Failed to load activity logs: ${escapeHTML(err.message)}</div>`;
    }
  }

  // --- 9. SETTINGS MODULE ---
  function initSettings() {
    const form = $('#settingsForm');
    form?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const fullName = $('#settingsFullName').value.trim();
      const currency = $('#settingsCurrency').value;
      const monthlyBudget = Number($('#settingsMonthlyBudget').value || 40000);
      const newPassword = $('#settingsNewPassword').value;

      try {
        const updated = await api('/api/settings', {
          method: 'PUT',
          body: JSON.stringify({ fullName, currency, monthlyBudget, newPassword })
        });

        if (state.currentUser) {
          state.currentUser.fullName = fullName;
          state.currentUser.currency = currency;
          state.currentUser.monthlyBudget = monthlyBudget;
          localStorage.setItem('spendly_user', JSON.stringify(state.currentUser));
        }

        updateUserProfileUI();
        $('#settingsNewPassword').value = '';
        showToast('Settings saved successfully!', 'success');
      } catch (err) {
        showToast(err.message || 'Failed to update settings', 'danger');
      }
    });
  }

  async function loadSettings() {
    try {
      const data = await api('/api/settings');
      if (data) {
        const sName = $('#settingsFullName');
        const sEmail = $('#settingsEmail');
        const sBudget = $('#settingsMonthlyBudget');
        const sCurrency = $('#settingsCurrency');

        if (sName && data.fullName) sName.value = data.fullName;
        if (sEmail && data.email) sEmail.value = data.email;
        if (sBudget && data.monthlyBudget) sBudget.value = data.monthlyBudget;
        if (sCurrency && data.currency) sCurrency.value = data.currency;
      }
    } catch (e) {
      // Non-fatal
    }
  }

  // --- MODALS SYSTEM ---
  function initModals() {
    // Open modal buttons (e.g. data-open-modal="expenseModal")
    document.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-open-modal]');
      if (btn) {
        const modalId = btn.dataset.openModal;
        openModal(modalId);
      }
    });

    // Close modal buttons (e.g. data-close-modal="expenseModal")
    document.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-close-modal]');
      if (btn) {
        const modalId = btn.dataset.closeModal;
        closeModal(modalId);
      }
    });

    // Close on background click
    $$('.modal').forEach(modal => {
      modal.addEventListener('click', (e) => {
        if (e.target === modal) {
          closeModal(modal.id);
        }
      });
    });

    // Close on ESC key
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && state.activeModal) {
        closeModal(state.activeModal);
      }
    });

    // Set default date for expense form to today
    const expDate = $('#expenseDate');
    if (expDate) {
      expDate.value = new Date().toISOString().split('T')[0];
    }

    // Modal Form 1: Add Expense
    const expenseForm = $('#modalExpenseForm');
    expenseForm?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const alertEl = $('#expenseModalAlert');
      if (alertEl) { alertEl.className = 'auth-alert'; alertEl.textContent = ''; }

      const merchant = $('#merchant').value.trim();
      const amount = Number($('#amount').value || 0);
      const category = $('#category').value;
      const direction = $('#expenseDirection').value;
      const date = $('#expenseDate').value;
      const isTransfer = $('#expenseIsTransfer').checked;

      const saveBtn = $('#saveExpenseBtn');
      saveBtn.disabled = true;
      saveBtn.textContent = 'Saving…';

      try {
        await api('/api/transactions', {
          method: 'POST',
          body: JSON.stringify({
            merchant,
            description: merchant,
            amount,
            category,
            direction,
            date,
            transactionDate: date,
            isTransfer
          })
        });

        closeModal('expenseModal');
        expenseForm.reset();
        if (expDate) expDate.value = new Date().toISOString().split('T')[0];

        showToast('Expense recorded successfully!', 'success');
        loadExpenses();
        loadDashboard();
      } catch (err) {
        console.error('Save expense error:', err);
        // Specifically format blocked parent control alert if applicable
        if (alertEl) {
          alertEl.className = 'auth-alert show danger';
          alertEl.textContent = err.message || 'Transaction could not be saved.';
        }
        showToast(err.message || 'Transaction blocked or failed', 'danger', 5000);
      } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = 'Save transaction';
      }
    });

    // Modal Form 2: Add Budget
    const budgetForm = $('#modalBudgetForm');
    budgetForm?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const categoryName = $('#modalBudgetCategory').value;
      const monthlyLimit = Number($('#modalBudgetLimit').value || 0);

      try {
        await api('/api/budgets', {
          method: 'POST',
          body: JSON.stringify({ categoryName, monthlyLimit })
        });

        closeModal('budgetModal');
        budgetForm.reset();
        showToast(`Budget ceiling of ${formatMoney(monthlyLimit)} set for ${categoryName}`, 'success');
        loadBudgets();
        loadDashboard();
      } catch (err) {
        showToast(err.message || 'Failed to save category budget', 'danger');
      }
    });

    // Modal Form 3: New Split Bill
    const splitForm = $('#modalSplitForm');
    splitForm?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const title = $('#splitTitle').value.trim();
      const totalAmount = Number($('#splitTotalAmount').value || 0);
      const rawParticipants = $('#splitParticipantsInput').value.trim();
      const notes = $('#splitNotes').value.trim();

      const participantNames = rawParticipants.split(',').map(s => s.trim()).filter(Boolean);
      if (participantNames.length === 0) {
        showToast('Please specify at least one participant', 'danger');
        return;
      }

      const count = participantNames.length;
      const share = count > 0 ? Number((totalAmount / count).toFixed(2)) : 0;
      const participants = participantNames.map(name => ({
        name: name,
        shareAmount: share,
        isSettled: false
      }));

      try {
        await api('/api/split-bills', {
          method: 'POST',
          body: JSON.stringify({
            title,
            totalAmount,
            participants,
            notes
          })
        });

        closeModal('splitModal');
        splitForm.reset();
        showToast('Split bill created and shared!', 'success');
        loadSplitBills();
        loadDashboard();
      } catch (err) {
        showToast(err.message || 'Failed to create split bill', 'danger');
      }
    });

    // Modal Form 4: Record Lending / Borrowing
    const lendingForm = $('#modalLendingForm');
    lendingForm?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const personName = $('#lendingPersonName').value.trim();
      const type = $('#lendingType').value;
      const amount = Number($('#lendingAmount').value || 0);
      const dueDate = $('#lendingDueDate').value || null;
      const notes = $('#lendingNotes').value.trim();

      try {
        await api('/api/obligations', {
          method: 'POST',
          body: JSON.stringify({
            personName,
            type,
            amount,
            dueDate,
            notes
          })
        });

        closeModal('lendingModal');
        lendingForm.reset();
        showToast(`Recorded ${type === 'LENT' ? 'loan to' : 'borrowing from'} ${personName}`, 'success');
        loadLending();
        loadDashboard();
      } catch (err) {
        showToast(err.message || 'Failed to record loan', 'danger');
      }
    });

    // Modal Form 5: Record Repayment
    const repaymentForm = $('#modalRepaymentForm');
    repaymentForm?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const id = $('#repayObligationId').value;
      const amount = Number($('#repayAmount').value || 0);
      const notes = $('#repayNotes').value.trim();

      try {
        await api(`/api/obligations/${id}/repay`, {
          method: 'POST',
          body: JSON.stringify({ amount, notes })
        });

        closeModal('repaymentModal');
        repaymentForm.reset();
        showToast(`Repayment of ${formatMoney(amount)} recorded!`, 'success');
        loadLending();
        loadDashboard();
      } catch (err) {
        showToast(err.message || 'Failed to record repayment', 'danger');
      }
    });
  }

  function openModal(modalId) {
    const modal = $(`#${modalId}`);
    if (modal) {
      modal.classList.add('show');
      modal.classList.add('open');
      modal.style.display = 'grid';
      state.activeModal = modalId;
    }
  }

  function closeModal(modalId) {
    const modal = $(`#${modalId}`);
    if (modal) {
      modal.classList.remove('show');
      modal.classList.remove('open');
      modal.style.display = 'none';
      if (state.activeModal === modalId) {
        state.activeModal = null;
      }
    }
  }

  // --- CUSTOM CATEGORIES MODULE ---
  const CURATED_ICONS = ['🏷️', '☕', '🛒', '🛍', '🚗', '🎬', '💊', '📚', '⚡', '📈', '🏠', '✂', '🐾', '✈️', '🎮', '💻', '🎁', '🏋️', '🍽️', '🎨', '🛠️', '👶', '🍕', '🚴', '🩺', '🎟️', '💈', '💼', '📦', '🛡️', '🍿', '🎧'];
  const CURATED_COLORS = ['#16865c', '#1abc9c', '#27ae60', '#2980b9', '#3498db', '#9b59b6', '#e67e22', '#e74c3c', '#f39c12', '#c0392b', '#8e44ad', '#d35400', '#7f8c8d', '#2c3e50', '#145a43', '#c8a24a'];

  async function loadCategories() {
    try {
      const data = await api('/api/categories');
      state.categories = Array.isArray(data) ? data : [];

      // 1. Populate Expense modal select (#category)
      const expenseCatSelect = $('#category');
      if (expenseCatSelect) {
        const currentVal = expenseCatSelect.value;
        expenseCatSelect.innerHTML = state.categories
          .filter(c => (c.type || '').toUpperCase() !== 'TRANSFER')
          .map(c => `<option value="${escapeHTML(c.name)}">${c.icon ? c.icon + ' ' : ''}${escapeHTML(c.name)}</option>`)
          .join('');
        if (currentVal && state.categories.some(c => c.name.toLowerCase() === currentVal.toLowerCase())) {
          expenseCatSelect.value = currentVal;
        }
      }

      // 2. Populate Budget modal select (#modalBudgetCategory)
      const budgetCatSelect = $('#modalBudgetCategory');
      if (budgetCatSelect) {
        const currentVal = budgetCatSelect.value;
        budgetCatSelect.innerHTML = state.categories
          .filter(c => (c.type || '').toUpperCase() === 'EXPENSE')
          .map(c => `<option value="${escapeHTML(c.name)}">${c.icon ? c.icon + ' ' : ''}${escapeHTML(c.name)}</option>`)
          .join('');
        if (currentVal && state.categories.some(c => c.name.toLowerCase() === currentVal.toLowerCase())) {
          budgetCatSelect.value = currentVal;
        }
      }

      // 3. Populate Expenses filter select (#expenseCategoryFilter)
      const filterCatSelect = $('#expenseCategoryFilter');
      if (filterCatSelect) {
        const currentVal = filterCatSelect.value;
        filterCatSelect.innerHTML = `<option value="">All categories</option>` +
          state.categories.map(c => `<option value="${escapeHTML(c.name)}">${c.icon ? c.icon + ' ' : ''}${escapeHTML(c.name)}</option>`).join('');
        if (currentVal) {
          filterCatSelect.value = currentVal;
        }
      }

      // 4. Populate Custom Categories management list inside categoryModal (#customCategoriesList)
      renderCustomCategoriesList();
    } catch (err) {
      console.warn('Could not load categories:', err);
    }
  }

  function initCustomCategories() {
    // 1. Render icon grid
    const iconGrid = $('#categoryIconGrid');
    const iconInput = $('#categoryIconInput');
    if (iconGrid && iconInput) {
      iconGrid.innerHTML = CURATED_ICONS.map((icon, idx) => `
        <span class="icon-choice ${idx === 0 ? 'active' : ''}" data-icon="${icon}">${icon}</span>
      `).join('');

      iconGrid.addEventListener('click', (e) => {
        const choice = e.target.closest('.icon-choice');
        if (!choice) return;
        iconGrid.querySelectorAll('.icon-choice').forEach(el => el.classList.remove('active'));
        choice.classList.add('active');
        iconInput.value = choice.dataset.icon;
      });
    }

    // 2. Render color grid
    const colorGrid = $('#categoryColorGrid');
    const colorInput = $('#categoryColorInput');
    if (colorGrid && colorInput) {
      colorGrid.innerHTML = CURATED_COLORS.map((color, idx) => `
        <span class="color-choice ${idx === 0 ? 'active' : ''}" data-color="${color}" style="background-color: ${color};"></span>
      `).join('');

      colorGrid.addEventListener('click', (e) => {
        const choice = e.target.closest('.color-choice');
        if (!choice) return;
        colorGrid.querySelectorAll('.color-choice').forEach(el => el.classList.remove('active'));
        choice.classList.add('active');
        colorInput.value = choice.dataset.color;
      });
    }

    // 3. Track modal origin buttons
    $('#openCategoryModalFromExpensesBtn')?.addEventListener('click', () => {
      state.categorySourceModal = 'expensesPage';
      clearCategoryModalAlert();
      openModal('categoryModal');
    });

    $('#openCategoryModalFromBudgetsBtn')?.addEventListener('click', () => {
      state.categorySourceModal = 'budgetsPage';
      clearCategoryModalAlert();
      openModal('categoryModal');
    });

    $('#openCategoryFromExpenseModalBtn')?.addEventListener('click', () => {
      state.categorySourceModal = 'expenseModal';
      clearCategoryModalAlert();
      openModal('categoryModal');
    });

    $('#openCategoryFromBudgetModalBtn')?.addEventListener('click', () => {
      state.categorySourceModal = 'budgetModal';
      clearCategoryModalAlert();
      openModal('categoryModal');
    });

    function clearCategoryModalAlert() {
      const alertEl = $('#categoryModalAlert');
      if (alertEl) {
        alertEl.className = 'auth-alert';
        alertEl.textContent = '';
      }
    }

    // 4. Form submission
    const form = $('#modalCategoryForm');
    form?.addEventListener('submit', async (e) => {
      e.preventDefault();
      clearCategoryModalAlert();
      const alertEl = $('#categoryModalAlert');

      // Parent Control check: Children cannot create categories
      if (state.currentUser && String(state.currentUser.role).toUpperCase() === 'CHILD') {
        if (alertEl) {
          alertEl.className = 'auth-alert show danger';
          alertEl.textContent = 'Child accounts are not permitted to create custom categories. Please request your parent to add new spending categories.';
        }
        showToast('Child accounts cannot create categories', 'danger');
        return;
      }

      const nameInput = $('#categoryNameInput');
      const name = nameInput ? nameInput.value.trim() : '';

      if (!name) {
        if (alertEl) {
          alertEl.className = 'auth-alert show danger';
          alertEl.textContent = 'Please enter a valid category name.';
        }
        nameInput?.focus();
        return;
      }

      // Check duplicates locally first
      if (state.categories.some(c => (c.name || '').trim().toLowerCase() === name.toLowerCase())) {
        if (alertEl) {
          alertEl.className = 'auth-alert show danger';
          alertEl.textContent = `A category named "${escapeHTML(name)}" already exists.`;
        }
        nameInput?.focus();
        return;
      }

      const type = $('#categoryTypeSelect')?.value || 'EXPENSE';
      const icon = $('#categoryIconInput')?.value || '🏷️';
      const color = $('#categoryColorInput')?.value || '#16865c';

      const saveBtn = $('#saveCategoryBtn');
      if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.textContent = 'Saving…';
      }

      try {
        const created = await api('/api/categories', {
          method: 'POST',
          body: JSON.stringify({ name, type, icon, color })
        });

        closeModal('categoryModal');
        form.reset();

        // Reload categories across app
        await loadCategories();

        showToast(`Category "${created.name || name}" created successfully!`, 'success');

        // Context-aware navigation & auto-selection
        const source = state.categorySourceModal;
        if (source === 'expenseModal') {
          openModal('expenseModal');
          const expSelect = $('#category');
          if (expSelect) expSelect.value = created.name || name;
        } else if (source === 'budgetModal') {
          openModal('budgetModal');
          const budSelect = $('#modalBudgetCategory');
          if (budSelect) budSelect.value = created.name || name;
          $('#modalBudgetLimit')?.focus();
        } else if (source === 'budgetsPage') {
          openModal('budgetModal');
          const budSelect = $('#modalBudgetCategory');
          if (budSelect) budSelect.value = created.name || name;
          $('#modalBudgetLimit')?.focus();
        } else if (source === 'expensesPage') {
          openModal('expenseModal');
          const expSelect = $('#category');
          if (expSelect) expSelect.value = created.name || name;
        }

        // Refresh parent control summary so new rule is visible
        if (state.activeSection === 'parent') {
          loadParentControl();
        }
      } catch (err) {
        console.error('Create category error:', err);
        if (alertEl) {
          alertEl.className = 'auth-alert show danger';
          alertEl.textContent = err.message || 'Failed to create category.';
        }
        showToast(err.message || 'Category creation failed', 'danger');
      } finally {
        if (saveBtn) {
          saveBtn.disabled = false;
          saveBtn.textContent = 'Save category';
        }
      }
    });
  }

  function renderCustomCategoriesList() {
    const listEl = $('#customCategoriesList');
    if (!listEl) return;

    const customCats = state.categories.filter(c => c.isCustom || c.custom);
    if (customCats.length === 0) {
      listEl.innerHTML = `<p class="muted" style="font-size: 11px; margin: 4px 0; text-align: center; padding: 10px; background: #fafaf7; border-radius: 8px;">No custom categories added yet.</p>`;
      return;
    }

    listEl.innerHTML = customCats.map(c => `
      <div style="display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: #fafaf7; border-radius: 8px; font-size: 12px; border: 1px solid var(--line);">
        <div style="display: flex; align-items: center; gap: 8px;">
          <span style="font-size: 16px;">${c.icon || '🏷️'}</span>
          <strong style="color: var(--ink);">${escapeHTML(c.name)}</strong>
          <span class="badge ${c.type === 'INCOME' ? 'success' : 'neutral'}" style="font-size: 9px; padding: 2px 6px;">${c.type || 'EXPENSE'}</span>
        </div>
        <button class="icon-btn small danger-text" title="Delete category" onclick="window.spendlyDeleteCategory(${c.id}, '${escapeHTML(c.name).replace(/'/g, "\\'")}')">🗑</button>
      </div>
    `).join('');
  }

  window.spendlyDeleteCategory = async function (id, name) {
    if (state.currentUser && String(state.currentUser.role).toUpperCase() === 'CHILD') {
      showToast('Child accounts are not permitted to delete categories', 'danger');
      return;
    }

    if (!confirm(`Are you sure you want to delete custom category "${name}"?`)) return;

    try {
      await api(`/api/categories/${id}`, { method: 'DELETE' });
      showToast(`Category "${name}" deleted successfully`, 'success');
      await loadCategories();
      if (state.activeSection === 'expenses') loadExpenses();
      if (state.activeSection === 'budgets') loadBudgets();
      if (state.activeSection === 'dashboard') loadDashboard();
      if (state.activeSection === 'parent') loadParentControl();
    } catch (err) {
      showToast(err.message || 'Cannot delete category (it may be used by transactions or budgets)', 'danger');
    }
  };

  // --- DEMO DATA CENTRE MODULE (Phase 5) ---
  function initDemoDataCentre() {
    const form = $('#demoDataForm');
    const startInput = $('#demoStartDate');
    const endInput = $('#demoEndDate');
    const loadSampleBtn = $('#demoLoadSampleBtn');
    const resetBtn = $('#demoResetBtn');

    // Default dates: last 30 days
    if (startInput && endInput) {
      const now = new Date();
      endInput.value = now.toISOString().split('T')[0];
      const monthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
      startInput.value = monthAgo.toISOString().split('T')[0];
    }

    form?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const alertEl = $('#demoModalAlert');
      if (alertEl) { alertEl.className = 'auth-alert'; alertEl.textContent = ''; }

      const account = $('#demoAccountSelect').value;
      const count = parseInt($('#demoCountSelect').value, 10);
      const startDate = $('#demoStartDate').value || null;
      const endDate = $('#demoEndDate').value || null;
      const transactionType = $('#demoTypeSelect').value;

      const checkedCategories = $$('input[name="demoCat"]:checked').map(cb => cb.value);
      const includeTransfers = $('#demoIncludeTransfers').checked;
      const includeDuplicates = $('#demoIncludeDuplicates').checked;
      const includeAttemptedRestricted = $('#demoIncludeRestricted').checked;
      const includeBudgetOverruns = $('#demoIncludeOverruns').checked;
      const includeSplitBillsAndLoans = $('#demoIncludeSplitLoans').checked;

      const generateBtn = $('#demoGenerateBtn');
      generateBtn.disabled = true;
      generateBtn.textContent = 'Generating…';

      try {
        const res = await api('/api/demo/generate', {
          method: 'POST',
          body: JSON.stringify({
            account,
            count,
            startDate,
            endDate,
            categories: checkedCategories,
            transactionType,
            includeTransfers,
            includeDuplicates,
            includeAttemptedRestricted,
            includeBudgetOverruns,
            includeSplitBillsAndLoans
          })
        });

        closeModal('demoModal');
        showToast(res.message || 'Demo data generated successfully!', 'success', 4000);

        // Refresh currently active section & alerts
        loadSectionData(state.activeSection);
        loadAlerts();
      } catch (err) {
        if (alertEl) {
          alertEl.className = 'auth-alert show danger';
          alertEl.textContent = err.message || 'Failed to generate demo data.';
        }
        showToast(err.message || 'Failed to generate demo data', 'danger');
      } finally {
        generateBtn.disabled = false;
        generateBtn.textContent = 'Generate Demo Data →';
      }
    });

    loadSampleBtn?.addEventListener('click', async () => {
      try {
        const res = await api('/api/demo/load-sample', { method: 'POST' });
        closeModal('demoModal');
        showToast(res.message || 'Standard sample reference dataset loaded!', 'success', 4000);
        loadSectionData(state.activeSection);
        loadAlerts();
      } catch (err) {
        showToast(err.message || 'Failed to load sample dataset', 'danger');
      }
    });

    resetBtn?.addEventListener('click', async () => {
      if (!confirm('Are you sure you want to reset demo data? All records tagged as [DEMO DATA] will be safely deleted. Your real user records and imported bank statements will NOT be affected.')) {
        return;
      }

      try {
        const res = await api('/api/demo/reset', { method: 'POST' });
        closeModal('demoModal');
        showToast(res.message || 'Demo data has been reset.', 'info', 4000);
        loadSectionData(state.activeSection);
        loadAlerts();
      } catch (err) {
        showToast(err.message || 'Failed to reset demo data', 'danger');
      }
    });
  }

  // --- INITIALIZATION ---
  document.addEventListener('DOMContentLoaded', () => {
    initAuth();
    initNavigation();
    initDashboard();
    initExpenses();
    initBudgets();
    initSplitBills();
    initLending();
    initBankStatements();
    initParentControl();
    initActivityLog();
    initDemoDataCentre();
    initSettings();
    initModals();
    initCustomCategories();

    // Verify authentication and kick off app
    checkExistingSession();
  });

})();
