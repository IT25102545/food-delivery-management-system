document.addEventListener('DOMContentLoaded', () => {
    // Select textual elements that should be animated
    const textElements = document.querySelectorAll('h1, h2, h3, h4, p, .price, .badge, .drink-title, #drink-name-display, #drink-desc-display, #drink-price-display, #bg-drink-name');

    // Add the base class for animation
    textElements.forEach(el => {
        // Exclude elements that shouldn't be animated (e.g. within existing animations, or if we want to ignore specific ones)
        if (!el.closest('.no-reveal') && !el.classList.contains('reveal-text')) {
            el.classList.add('reveal-text');
        }
    });

    // Setup Intersection Observer
    const observerOptions = {
        root: null,
        rootMargin: '0px 0px -50px 0px',
        threshold: 0.1
    };

    const textObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('active');
                observer.unobserve(entry.target); // Only animate once
            }
        });
    }, observerOptions);

    // Start observing the newly classed elements
    document.querySelectorAll('.reveal-text').forEach(el => {
        textObserver.observe(el);
    });
});
