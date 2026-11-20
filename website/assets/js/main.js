/**
 * Kanri Website Main Utility Script
 * Checksum copy helpers, mobile navigation, and graceful GitHub star counter.
 */
document.addEventListener('DOMContentLoaded', () => {
  // Mobile Nav Drawer Toggle
  const mobileToggle = document.getElementById('mobile-nav-toggle');
  const mainNav = document.getElementById('main-nav');
  if (mobileToggle && mainNav) {
    mobileToggle.addEventListener('click', () => {
      const isExpanded = mainNav.style.display === 'flex';
      mainNav.style.display = isExpanded ? '' : 'flex';
      mobileToggle.setAttribute('aria-expanded', !isExpanded);
    });
  }

  // Copy to Clipboard Helpers
  document.querySelectorAll('[data-copy-target]').forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-copy-target');
      const targetEl = document.getElementById(targetId);
      if (!targetEl) return;

      const text = targetEl.textContent.trim();
      navigator.clipboard.writeText(text).then(() => {
        const origText = btn.textContent;
        btn.textContent = 'Copied!';
        btn.classList.add('copied');
        setTimeout(() => {
          btn.textContent = origText;
          btn.classList.remove('copied');
        }, 2000);
      }).catch(() => {});
    });
  });

  // Fetch GitHub Stars Gracefully (Zero Blocking)
  const starBadge = document.getElementById('github-star-count');
  if (starBadge) {
    fetch('https://api.github.com/repos/omkarnub/Kanri')
      .then(res => res.json())
      .then(data => {
        if (data && typeof data.stargazers_count === 'number') {
          starBadge.textContent = data.stargazers_count.toLocaleString();
          starBadge.style.display = 'inline-block';
        }
      })
      .catch(() => {
        // Fallback: omit star badge silently if offline or rate-limited
        starBadge.style.display = 'none';
      });
  }
});
