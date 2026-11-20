/**
 * Kanri Client-Side Instant Documentation & FAQ Search
 * 100% in-browser keyword filtering. Zero external API calls.
 */
document.addEventListener('DOMContentLoaded', () => {
  const searchInput = document.getElementById('docs-search-input');
  const navLinks = document.querySelectorAll('.docs-nav-link');
  const docSections = document.querySelectorAll('.docs-searchable-section');

  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      const query = e.target.value.toLowerCase().trim();

      // Filter sidebar nav items
      navLinks.forEach(link => {
        const text = link.textContent.toLowerCase();
        const parentLi = link.parentElement;
        if (!query || text.includes(query)) {
          if (parentLi) parentLi.style.display = '';
        } else {
          if (parentLi) parentLi.style.display = 'none';
        }
      });

      // Filter in-page sections if present
      docSections.forEach(section => {
        const content = section.textContent.toLowerCase();
        if (!query || content.includes(query)) {
          section.style.display = '';
        } else {
          section.style.display = 'none';
        }
      });
    });
  }

  // FAQ Page Search
  const faqSearch = document.getElementById('faq-search-input');
  const faqItems = document.querySelectorAll('.faq-item');
  if (faqSearch && faqItems.length > 0) {
    faqSearch.addEventListener('input', (e) => {
      const query = e.target.value.toLowerCase().trim();
      faqItems.forEach(item => {
        const question = item.querySelector('.faq-question')?.textContent.toLowerCase() || '';
        const answer = item.querySelector('.faq-answer')?.textContent.toLowerCase() || '';
        if (!query || question.includes(query) || answer.includes(query)) {
          item.style.display = '';
          if (query) item.setAttribute('open', 'true');
        } else {
          item.style.display = 'none';
          item.removeAttribute('open');
        }
      });
    });
  }
});
