/**
 * Kanri Interactive Calculators
 * 1. Bill Splitting Calculator
 * 2. Safe-to-Spend Daily Allowance Calculator
 * 3. 4-Pillar Financial Health Score Simulator (0–100)
 */

document.addEventListener('DOMContentLoaded', () => {
  // ------------------------------------------------------------------------
  // 1. Bill Splitting Calculator
  // ------------------------------------------------------------------------
  const splitAmountInput = document.getElementById('split-amount');
  const splitPeopleSlider = document.getElementById('split-people');
  const splitPeopleCount = document.getElementById('split-people-count');
  const splitResult = document.getElementById('split-result');

  function updateBillSplit() {
    if (!splitAmountInput || !splitPeopleSlider || !splitResult) return;
    const amount = parseFloat(splitAmountInput.value) || 0;
    const people = parseInt(splitPeopleSlider.value, 10) || 1;
    if (splitPeopleCount) splitPeopleCount.textContent = people;

    const perPerson = people > 0 ? (amount / people).toFixed(2) : '0.00';
    // Format to Indian Currency format
    const formatted = Number(perPerson).toLocaleString('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    splitResult.textContent = `₹${formatted}`;
  }

  if (splitAmountInput && splitPeopleSlider) {
    splitAmountInput.addEventListener('input', updateBillSplit);
    splitPeopleSlider.addEventListener('input', updateBillSplit);
    updateBillSplit();
  }

  // ------------------------------------------------------------------------
  // 2. Safe-to-Spend Calculator
  // ------------------------------------------------------------------------
  const budgetInput = document.getElementById('budget-total');
  const spentInput = document.getElementById('budget-spent');
  const daysSlider = document.getElementById('budget-days');
  const daysVal = document.getElementById('budget-days-val');
  const safeSpendResult = document.getElementById('safe-spend-result');

  function updateSafeSpend() {
    if (!budgetInput || !spentInput || !daysSlider || !safeSpendResult) return;
    const budget = parseFloat(budgetInput.value) || 0;
    const spent = parseFloat(spentInput.value) || 0;
    const days = parseInt(daysSlider.value, 10) || 1;
    if (daysVal) daysVal.textContent = `${days} days`;

    const remaining = Math.max(0, budget - spent);
    const dailyAllowance = (remaining / days).toFixed(2);
    const formatted = Number(dailyAllowance).toLocaleString('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    safeSpendResult.textContent = `₹${formatted} / day`;
  }

  if (budgetInput && spentInput && daysSlider) {
    budgetInput.addEventListener('input', updateSafeSpend);
    spentInput.addEventListener('input', updateSafeSpend);
    daysSlider.addEventListener('input', updateSafeSpend);
    updateSafeSpend();
  }

  // ------------------------------------------------------------------------
  // 3. 4-Pillar Financial Health Score Engine (0–100)
  // Matching FinancialHealthCalculator.kt logic
  // ------------------------------------------------------------------------
  const sliderSavings = document.getElementById('score-savings');
  const sliderAdherence = document.getElementById('score-adherence');
  const sliderVelocity = document.getElementById('score-velocity');
  const sliderDebt = document.getElementById('score-debt');

  const valSavings = document.getElementById('val-savings');
  const valAdherence = document.getElementById('val-adherence');
  const valVelocity = document.getElementById('val-velocity');
  const valDebt = document.getElementById('val-debt');

  const healthScoreDisplay = document.getElementById('health-score-number');
  const healthGradeDisplay = document.getElementById('health-grade-text');

  function calculateHealthScore() {
    if (!sliderSavings || !sliderAdherence || !sliderVelocity || !sliderDebt) return;

    const savingsRate = parseInt(sliderSavings.value, 10);      // 0 - 50%
    const adherence = parseInt(sliderAdherence.value, 10);        // 0 - 120%
    const velocity = parseInt(sliderVelocity.value, 10);          // 0 - 100% (speed vs calendar)
    const debtRatio = parseInt(sliderDebt.value, 10);            // 0 - 100%

    if (valSavings) valSavings.textContent = `${savingsRate}%`;
    if (valAdherence) valAdherence.textContent = `${adherence}%`;
    if (valVelocity) valVelocity.textContent = `${velocity}% pace`;
    if (valDebt) valDebt.textContent = `${debtRatio}%`;

    // Pillar 1: Savings Rate (25 pts max; >= 20% earns full 25)
    let p1 = Math.min(25, (savingsRate / 20) * 25);

    // Pillar 2: Budget Adherence (25 pts max; <= 90% earns full 25; decays towards 100% and 0 at 120%)
    let p2 = 25;
    if (adherence > 90) {
      p2 = Math.max(0, 25 - ((adherence - 90) / 30) * 25);
    }

    // Pillar 3: Spend Velocity vs Calendar (25 pts max; <= 100% earns full 25)
    let p3 = 25;
    if (velocity > 100) {
      p3 = Math.max(0, 25 - ((velocity - 100) / 40) * 25);
    }

    // Pillar 4: Debt Ratio (25 pts max; >= 50% net positive)
    let p4 = (debtRatio / 100) * 25;

    const totalScore = Math.round(p1 + p2 + p3 + p4);

    let grade = 'D';
    let gradeLabel = 'Critical Action Needed';
    let gradeColor = 'var(--expense-red)';

    if (totalScore >= 90) {
      grade = 'A+';
      gradeLabel = 'Optimal Sovereignty';
      gradeColor = 'var(--accent)';
    } else if (totalScore >= 75) {
      grade = 'A';
      gradeLabel = 'Strong Financial Health';
      gradeColor = 'var(--accent)';
    } else if (totalScore >= 60) {
      grade = 'B';
      gradeLabel = 'Balanced Pacing';
      gradeColor = 'var(--warning-amber)';
    } else if (totalScore >= 40) {
      grade = 'C';
      gradeLabel = 'Caution Advised';
      gradeColor = 'var(--warning-amber)';
    }

    if (healthScoreDisplay) {
      healthScoreDisplay.textContent = totalScore;
    }
    if (healthGradeDisplay) {
      healthGradeDisplay.textContent = `Grade ${grade} · ${gradeLabel}`;
      healthGradeDisplay.style.color = gradeColor;
    }
  }

  [sliderSavings, sliderAdherence, sliderVelocity, sliderDebt].forEach(slider => {
    if (slider) slider.addEventListener('input', calculateHealthScore);
  });

  calculateHealthScore();
});
