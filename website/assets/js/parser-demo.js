/**
 * Kanri Client-Side SMS Parser Demo
 * Runs 100% in browser memory. Zero network requests. Zero telemetry.
 * Fabricated sample messages only.
 */
(function() {
  const PRESETS = {
    hdfc: "Sent Rs. 480.00 from HDFC Bank A/C **4821 to SWIGGY on 21-Nov-26 via UPI Ref 432618920192. Bal: Rs. 14,250.00",
    sbi_atm: "Dear SBI User, your A/C ending 9012 debited by Rs 2000.00 on 20Nov26 at SBI ATM CONNAUGHT PL. Avail Bal: Rs 8,340.00",
    icici_credit: "ICICI Bank Acct XX3104 credited with Rs 65,000.00 on 21-Nov-26 by NEFT-ACME CORP-SALARY. Avail Bal INR 1,12,450.00",
    otp: "Your OTP for transaction of INR 1,250.00 at AMAZON PAY is 849201. Do not share this with anyone. Valid for 10 mins."
  };

  function isOtp(text) {
    const otpKeywords = [/\bOTP\b/i, /one[\s-]time\s+password/i, /verification\s+code/i, /secret\s+code/i, /valid\s+for\s+\d+\s+min/i, /do\s+not\s+share/i];
    return otpKeywords.some(pattern => pattern.test(text));
  }

  function parseSms(text) {
    if (isOtp(text)) {
      return {
        isOtp: true,
        message: "Ignored: Kanri never stores or parses OTP messages."
      };
    }

    // 1. Transaction Type
    let type = "DEBIT";
    if (/\b(credited|credit|received|refund|deposited)\b/i.test(text)) {
      type = "CREDIT";
    }
    const isAtm = /\b(atm|cash withdrawal)\b/i.test(text);

    // 2. Amount Extraction
    let amount = "—";
    const amountMatch = text.match(/(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)/i);
    if (amountMatch) {
      amount = "₹" + amountMatch[1];
    }

    // 3. Account Extraction
    let account = "—";
    const acctMatch = text.match(/(?:A\/C|Acct|Account|ending|Card)\s*(?:no\.?)?\s*[*Xx]*(\d{4})/i);
    if (acctMatch) {
      account = "••" + acctMatch[1];
    }

    // 4. Merchant / Counterparty
    let merchant = "—";
    const toMatch = text.match(/(?:to|at|by|info\/)\s+([A-Za-z0-9\s._-]+?)(?=\s+(?:on|via|ref|bal|available|\.|$))/i);
    if (toMatch) {
      merchant = toMatch[1].trim();
    } else if (isAtm) {
      merchant = "ATM Cash Withdrawal";
    }

    // 5. Reference / UTR
    let ref = "—";
    const refMatch = text.match(/(?:UPI\s+Ref|Ref|UTR|Txn\s*ID)\s*:?\s*([A-Za-z0-9]+)/i);
    if (refMatch) {
      ref = refMatch[1];
    }

    return {
      isOtp: false,
      type: isAtm ? "ATM WITHDRAWAL" : type,
      amount: amount,
      account: account,
      merchant: merchant,
      ref: ref
    };
  }

  function renderOutput(result, container) {
    if (result.isOtp) {
      container.innerHTML = `
        <div class="otp-alert-box">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <span>${result.message}</span>
        </div>
      `;
      return;
    }

    const typeClass = result.type.includes('CREDIT') ? 'credit' : 'debit';

    container.innerHTML = `
      <div class="parser-pills-row">
        <div class="parsed-pill">
          <span class="parsed-pill-label">Action</span>
          <span class="parsed-pill-value ${typeClass}">${result.type}</span>
        </div>
        <div class="parsed-pill">
          <span class="parsed-pill-label">Amount</span>
          <span class="parsed-pill-value">${result.amount}</span>
        </div>
        <div class="parsed-pill">
          <span class="parsed-pill-label">Account</span>
          <span class="parsed-pill-value">${result.account}</span>
        </div>
        <div class="parsed-pill">
          <span class="parsed-pill-label">Merchant / Source</span>
          <span class="parsed-pill-value">${result.merchant}</span>
        </div>
        <div class="parsed-pill">
          <span class="parsed-pill-label">Reference / UTR</span>
          <span class="parsed-pill-value text-mono">${result.ref}</span>
        </div>
      </div>
    `;
  }

  document.addEventListener('DOMContentLoaded', () => {
    const textarea = document.getElementById('parser-input');
    const output = document.getElementById('parser-output');
    const presetBtns = document.querySelectorAll('[data-parser-preset]');

    if (!textarea || !output) return;

    function runParse() {
      const val = textarea.value.trim();
      if (!val) {
        output.innerHTML = '<p class="text-muted" style="font-size:0.875rem;">Paste or type a transactional SMS above to inspect real-time regex extraction.</p>';
        return;
      }
      const result = parseSms(val);
      renderOutput(result, output);
    }

    presetBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        presetBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        const key = btn.getAttribute('data-parser-preset');
        if (PRESETS[key]) {
          textarea.value = PRESETS[key];
          runParse();
        }
      });
    });

    textarea.addEventListener('input', () => {
      presetBtns.forEach(b => b.classList.remove('active'));
      runParse();
    });

    // Run initial parse on default preset
    runParse();
  });
})();
