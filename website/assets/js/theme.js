/**
 * Kanri Theme Engine
 * Manages Light (Cream), Dark (Ink), and AMOLED themes with system synchronization.
 */
(function() {
  const STORAGE_KEY = 'kanri_theme_preference';
  
  function getSystemPreference() {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  }
  
  function getSavedTheme() {
    try {
      return localStorage.getItem(STORAGE_KEY) || 'system';
    } catch (e) {
      return 'system';
    }
  }
  
  function applyTheme(theme) {
    const active = theme === 'system' ? getSystemPreference() : theme;
    document.documentElement.setAttribute('data-theme', active);
    
    // Update active button indicator if present
    document.querySelectorAll('[data-theme-value]').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-theme-value') === theme);
    });
  }
  
  // Initialize theme immediately to prevent flash of wrong theme
  const initialTheme = getSavedTheme();
  applyTheme(initialTheme);
  
  // Listen for system theme changes if set to system
  if (window.matchMedia) {
    window.matchMedia('(prefers-color-scheme: light)').addEventListener('change', () => {
      if (getSavedTheme() === 'system') {
        applyTheme('system');
      }
    });
  }
  
  // Public theme toggler
  window.KanriTheme = {
    set: function(theme) {
      try {
        localStorage.setItem(STORAGE_KEY, theme);
      } catch (e) {}
      applyTheme(theme);
    },
    cycle: function() {
      const current = getSavedTheme();
      const themes = ['dark', 'light', 'amoled', 'system'];
      const next = themes[(themes.indexOf(current) + 1) % themes.length];
      this.set(next);
      return next;
    },
    get: getSavedTheme
  };
  
  document.addEventListener('DOMContentLoaded', () => {
    const toggleBtn = document.getElementById('theme-toggle-btn');
    if (toggleBtn) {
      toggleBtn.addEventListener('click', () => {
        const next = window.KanriTheme.cycle();
        toggleBtn.setAttribute('title', `Theme: ${next}`);
      });
    }
  });
})();
