// global auth script for zestro customer flow

document.addEventListener('DOMContentLoaded', () => {
    initAuthUI();
});

function initAuthUI() {
    const userStr = localStorage.getItem('customerUser');
    const accountBtns = document.querySelectorAll('button[aria-label="Account"], #account-btn, #nav-account-btn');
    
    if (accountBtns.length === 0) return;

    if (userStr) {
        try {
            const user = JSON.parse(userStr);
            const name = user.full_name || user.username || 'Customer';
            
            accountBtns.forEach(btn => {
                btn.innerHTML = `
                    <div style="display: flex; align-items: center; gap: 8px; font-family: 'Inter', sans-serif;">
                        <span style="font-size: 14px; font-weight: 600;">${name}</span>
                        <div style="width: 32px; height: 32px; border-radius: 50%; background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; font-weight: bold; border: 1.5px solid #fff;">
                            ${name.charAt(0).toUpperCase()}
                        </div>
                    </div>
                `;
                btn.onclick = (e) => {
                    e.preventDefault();
                    window.location.href = '/customer-dashboard.html';
                };
            });
        } catch(e) {
            console.error("Error parsing user data", e);
        }
    } else {
        accountBtns.forEach(btn => {
            btn.onclick = (e) => {
                e.preventDefault();
                window.location.href = '/customer-login.html';
            };
        });
    }
}
