/**
 * Money Manager - Showcase Website Scripts
 * Interactive Phone Mockup, Expense Simulator, Currency Switcher, and FAQ
 */

document.addEventListener('DOMContentLoaded', () => {
  // State
  let currentCurrency = 'INR';
  let isPrivacyMasked = false;
  
  const currencyRates = {
    INR: { symbol: '₹', rate: 1, locale: 'en-IN' },
    USD: { symbol: '$', rate: 0.012, locale: 'en-US' },
    EUR: { symbol: '€', rate: 0.011, locale: 'de-DE' },
    GBP: { symbol: '£', rate: 0.0094, locale: 'en-GB' },
    JPY: { symbol: '¥', rate: 1.78, locale: 'ja-JP' }
  };

  let simState = {
    netWorthInr: 248500,
    monthlyExpenseInr: 28450,
    monthlyIncomeInr: 85000,
    logs: [
      { id: 1, title: 'Blue Tokai Coffee', amount: 280, type: 'EXPENSE', category: 'Food & Dining', account: 'HDFC Bank', time: 'Just now' },
      { id: 2, title: 'Supermarket Groceries', amount: 2450, type: 'EXPENSE', category: 'Groceries', account: 'HDFC Bank', time: '2 hours ago' },
      { id: 3, title: 'Tech Corp Salary', amount: 85000, type: 'INCOME', category: 'Salary', account: 'HDFC Bank', time: '1st Aug' }
    ]
  };

  // 1. Live Device Clock
  function updateClock() {
    const clockEl = document.getElementById('deviceClock');
    if (!clockEl) return;
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    clockEl.textContent = `${hours}:${minutes}`;
  }
  updateClock();
  setInterval(updateClock, 30000);

  // 2. Phone Mockup Tab Switching
  const navTabBtns = document.querySelectorAll('.nav-tab-btn');
  const tabPanes = document.querySelectorAll('.tab-pane');

  navTabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetTab = btn.getAttribute('data-tab');
      if (!targetTab) return;

      navTabBtns.forEach(b => b.classList.remove('active'));
      tabPanes.forEach(pane => pane.classList.remove('active'));

      btn.classList.add('active');
      const targetPane = document.getElementById(targetTab);
      if (targetPane) {
        targetPane.classList.add('active');
      }
    });
  });

  // 3. Privacy Mask Toggle
  const privacyBtn = document.getElementById('privacyToggleBtn');
  const eyeOpen = document.getElementById('eyeIconOpen');
  const eyeClosed = document.getElementById('eyeIconClosed');

  if (privacyBtn) {
    privacyBtn.addEventListener('click', () => {
      isPrivacyMasked = !isPrivacyMasked;
      privacyBtn.classList.toggle('masked', isPrivacyMasked);

      if (eyeOpen && eyeClosed) {
        eyeOpen.classList.toggle('hidden', isPrivacyMasked);
        eyeClosed.classList.toggle('hidden', !isPrivacyMasked);
      }

      applyFormatting();
    });
  }

  // 4. Currency Switcher
  const currencySelector = document.getElementById('currencySelector');
  const simCurrencySymbol = document.getElementById('simCurrencySymbol');

  function formatCurrencyAmount(rawInr, prefix = '', suffix = '') {
    if (isPrivacyMasked) {
      return `${currencyRates[currentCurrency].symbol} ••••••`;
    }

    const { symbol, rate, locale } = currencyRates[currentCurrency];
    const converted = rawInr * rate;

    let formattedNum;
    if (currentCurrency === 'JPY') {
      formattedNum = Math.round(converted).toLocaleString(locale);
    } else {
      formattedNum = converted.toLocaleString(locale, {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
      });
    }

    return `${prefix}${symbol}${formattedNum}${suffix}`;
  }

  function applyFormatting() {
    const maskableEls = document.querySelectorAll('.maskable-value');
    maskableEls.forEach(el => {
      const rawInr = parseFloat(el.getAttribute('data-raw-inr'));
      if (isNaN(rawInr)) return;

      const hasPlus = el.textContent.startsWith('+') || el.classList.contains('income');
      const hasMinus = el.textContent.startsWith('-') || el.classList.contains('expense');
      const prefix = hasPlus ? '+' : (hasMinus ? '-' : '');

      let suffix = '';
      if (el.textContent.includes('/mo')) suffix = '/mo';

      el.textContent = formatCurrencyAmount(rawInr, prefix, suffix);
    });

    // Update Simulator Displays
    const simNetWorthDisplay = document.getElementById('simNetWorthDisplay');
    const simExpenseDisplay = document.getElementById('simExpenseDisplay');

    if (simNetWorthDisplay) {
      simNetWorthDisplay.textContent = formatCurrencyAmount(simState.netWorthInr);
    }
    if (simExpenseDisplay) {
      simExpenseDisplay.textContent = formatCurrencyAmount(simState.monthlyExpenseInr);
    }

    if (simCurrencySymbol) {
      simCurrencySymbol.textContent = currencyRates[currentCurrency].symbol;
    }
  }

  if (currencySelector) {
    currencySelector.addEventListener('change', (e) => {
      currentCurrency = e.target.value;
      applyFormatting();
    });
  }

  // 5. Interactive Simulator
  const simForm = document.getElementById('simForm');
  const simLogList = document.getElementById('simLogList');
  const simResetBtn = document.getElementById('simResetBtn');
  const mockupTxList = document.getElementById('mockupTxList');

  const categoryIcons = {
    'Food & Dining': '🍔',
    'Groceries': '🛒',
    'Shopping': '🛍️',
    'Transportation': '🚕',
    'Entertainment': '🎬',
    'Salary': '💼',
    'Investments': '📈'
  };

  function renderSimLogs() {
    if (!simLogList) return;
    simLogList.innerHTML = '';

    simState.logs.forEach(log => {
      const item = document.createElement('div');
      item.className = 'tx-item';
      const icon = categoryIcons[log.category] || '💸';
      const isIncome = log.type === 'INCOME';
      const formattedAmt = formatCurrencyAmount(log.amount, isIncome ? '+' : '-');

      item.innerHTML = `
        <div class="tx-icon">${icon}</div>
        <div class="tx-info">
          <span class="tx-title">${log.title}</span>
          <span class="tx-date">${log.time} • ${log.account}</span>
        </div>
        <span class="tx-amount ${isIncome ? 'income' : 'expense'}">${formattedAmt}</span>
      `;
      simLogList.appendChild(item);
    });
  }

  if (simForm) {
    simForm.addEventListener('submit', (e) => {
      e.preventDefault();

      const titleInput = document.getElementById('simTitle');
      const amountInput = document.getElementById('simAmount');
      const typeInput = document.getElementById('simType');
      const categoryInput = document.getElementById('simCategory');
      const accountInput = document.getElementById('simAccount');

      const title = titleInput.value.trim();
      const amount = parseFloat(amountInput.value);
      const type = typeInput.value;
      const category = categoryInput.value;
      const account = accountInput.value;

      if (!title || isNaN(amount) || amount <= 0) return;

      // Update state
      if (type === 'EXPENSE') {
        simState.netWorthInr -= amount;
        simState.monthlyExpenseInr += amount;
      } else {
        simState.netWorthInr += amount;
        simState.monthlyIncomeInr += amount;
      }

      const newLog = {
        id: Date.now(),
        title,
        amount,
        type,
        category,
        account,
        time: 'Just now'
      };

      simState.logs.unshift(newLog);
      renderSimLogs();
      applyFormatting();

      // Prepend to phone mockup recent list as well
      if (mockupTxList) {
        const phoneItem = document.createElement('div');
        phoneItem.className = 'tx-item';
        const icon = categoryIcons[category] || '💸';
        const isIncome = type === 'INCOME';
        const formattedAmt = formatCurrencyAmount(amount, isIncome ? '+' : '-');

        phoneItem.innerHTML = `
          <div class="tx-icon">${icon}</div>
          <div class="tx-info">
            <span class="tx-title">${title}</span>
            <span class="tx-date">Today • ${account}</span>
          </div>
          <span class="tx-amount ${isIncome ? 'income' : 'expense'} maskable-value" data-raw-inr="${amount}">${formattedAmt}</span>
        `;
        mockupTxList.prepend(phoneItem);
      }

      // Visual feedback on submit button
      const submitBtn = document.getElementById('simSubmitBtn');
      if (submitBtn) {
        const origText = submitBtn.innerHTML;
        submitBtn.innerHTML = `<span>✓ Recorded!</span>`;
        submitBtn.style.background = 'linear-gradient(135deg, #059669 0%, #047857 100%)';
        setTimeout(() => {
          submitBtn.innerHTML = origText;
          submitBtn.style.background = '';
        }, 1500);
      }

      // Reset form title and amount for convenience
      titleInput.value = '';
      amountInput.value = '';
    });
  }

  if (simResetBtn) {
    simResetBtn.addEventListener('click', () => {
      simState.netWorthInr = 248500;
      simState.monthlyExpenseInr = 28450;
      simState.monthlyIncomeInr = 85000;
      simState.logs = [
        { id: 1, title: 'Blue Tokai Coffee', amount: 280, type: 'EXPENSE', category: 'Food & Dining', account: 'HDFC Bank', time: 'Just now' },
        { id: 2, title: 'Supermarket Groceries', amount: 2450, type: 'EXPENSE', category: 'Groceries', account: 'HDFC Bank', time: '2 hours ago' },
        { id: 3, title: 'Tech Corp Salary', amount: 85000, type: 'INCOME', category: 'Salary', account: 'HDFC Bank', time: '1st Aug' }
      ];
      renderSimLogs();
      applyFormatting();
    });
  }

  // 6. Copy to Clipboard for Terminal Blocks
  const copyBtns = document.querySelectorAll('.copy-code-btn');
  copyBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const textToCopy = btn.getAttribute('data-clipboard');
      if (!textToCopy) return;

      navigator.clipboard.writeText(textToCopy).then(() => {
        const originalText = btn.textContent;
        btn.textContent = 'Copied!';
        btn.classList.add('copied');
        setTimeout(() => {
          btn.textContent = originalText;
          btn.classList.remove('copied');
        }, 2000);
      }).catch(err => {
        console.error('Failed to copy', err);
      });
    });
  });

  // 7. FAQ Accordion Toggle
  const faqItems = document.querySelectorAll('.faq-item');
  faqItems.forEach(item => {
    const questionBtn = item.querySelector('.faq-question');
    if (questionBtn) {
      questionBtn.addEventListener('click', () => {
        const isActive = item.classList.contains('active');
        // Close others
        faqItems.forEach(i => i.classList.remove('active'));
        if (!isActive) {
          item.classList.add('active');
        }
      });
    }
  });

  // 8. Mobile Menu Toggle
  const mobileMenuBtn = document.getElementById('mobileMenuBtn');
  const navLinks = document.getElementById('navLinks');
  if (mobileMenuBtn && navLinks) {
    mobileMenuBtn.addEventListener('click', () => {
      const isVisible = navLinks.style.display === 'flex';
      navLinks.style.display = isVisible ? 'none' : 'flex';
      if (!isVisible) {
        navLinks.style.flexDirection = 'column';
        navLinks.style.position = 'absolute';
        navLinks.style.top = '100%';
        navLinks.style.left = '0';
        navLinks.style.width = '100%';
        navLinks.style.background = 'rgba(9, 13, 22, 0.95)';
        navLinks.style.padding = '20px';
        navLinks.style.borderBottom = '1px solid var(--border-subtle)';
      }
    });
  }

  // Initial render
  renderSimLogs();
  applyFormatting();
});
