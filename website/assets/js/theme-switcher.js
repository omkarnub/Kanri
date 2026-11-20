/**
 * Kanri Live Theme Switcher for Interactive Phone Mockup
 * Previews Light, Dark, AMOLED, and Glass themes with authentic backdrops.
 */
document.addEventListener('DOMContentLoaded', () => {
  const themeBtns = document.querySelectorAll('[data-mockup-theme]');
  const phoneMockup = document.getElementById('phone-mockup-frame');
  const backdropIframe = document.getElementById('phone-backdrop-iframe');
  const glassBackdropSelect = document.getElementById('glass-backdrop-select');
  const glassControls = document.getElementById('glass-controls-row');

  if (!phoneMockup) return;

  function setMockupTheme(theme) {
    phoneMockup.setAttribute('data-mockup-skin', theme);
    themeBtns.forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-mockup-theme') === theme);
    });

    if (theme === 'glass') {
      if (backdropIframe) backdropIframe.style.display = 'block';
      if (glassControls) glassControls.style.display = 'flex';
    } else {
      if (backdropIframe) backdropIframe.style.display = 'none';
      if (glassControls) glassControls.style.display = 'none';
    }
  }

  themeBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const theme = btn.getAttribute('data-mockup-theme');
      setMockupTheme(theme);
    });
  });

  if (glassBackdropSelect && backdropIframe) {
    glassBackdropSelect.addEventListener('change', (e) => {
      backdropIframe.src = e.target.value;
    });
  }
});
