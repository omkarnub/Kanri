/**
 * Kanri Website Hero Interactive Depth & Sheen Engine
 * Handles dynamic mouse coordinates for radial sheen and smooth 3D perspective tilt.
 */
document.addEventListener('DOMContentLoaded', () => {
  const heroCard = document.getElementById('hero-depth-card');
  if (!heroCard) return;

  let rafId = null;

  const onMouseMove = (e) => {
    if (rafId) cancelAnimationFrame(rafId);

    rafId = requestAnimationFrame(() => {
      const rect = heroCard.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;

      heroCard.style.setProperty('--mouse-x', `${x}px`);
      heroCard.style.setProperty('--mouse-y', `${y}px`);

      // Gentle 3D perspective tilt (-6 deg to +6 deg)
      const centerX = rect.width / 2;
      const centerY = rect.height / 2;
      const rotateX = ((y - centerY) / centerY) * -6;
      const rotateY = ((x - centerX) / centerX) * 6;

      heroCard.style.transform = `perspective(1000px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg) scale3d(1.01, 1.01, 1.01)`;
    });
  };

  const onMouseLeave = () => {
    if (rafId) cancelAnimationFrame(rafId);
    heroCard.style.setProperty('--mouse-x', '50%');
    heroCard.style.setProperty('--mouse-y', '50%');
    heroCard.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
  };

  heroCard.addEventListener('mousemove', onMouseMove);
  heroCard.addEventListener('mouseleave', onMouseLeave);
});
