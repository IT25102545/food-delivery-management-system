document.addEventListener('DOMContentLoaded', () => {
    initAdminAuth();
});

function initAdminAuth() {
    const userStr = localStorage.getItem('adminUser');
    if (!userStr) {
        if (!window.location.pathname.includes('admin.html')) {
            window.location.href = '/admin.html';
        }
        return;
    }

    try {
        const user = JSON.parse(userStr);
        const name = user.full_name || 'Admin User';
        const initial = name.charAt(0).toUpperCase();
        
        // update top right header
        const profileWidget = document.querySelector('header .flex.items-center.gap-4');
        if (profileWidget) {
            const avatarHtml = user.profile_pic 
                ? `<img src="${user.profile_pic}" class="w-10 h-10 rounded-full object-cover custom-shadow border-2 border-white">`
                : `<div class="w-10 h-10 rounded-full bg-donezo-light flex items-center justify-center text-white font-bold text-lg custom-shadow border-2 border-white">${initial}</div>`;
            
            // rebuild the profile widget to ensure consistency
            let existingProfile = profileWidget.querySelector('.border-l');
            if (existingProfile) {
                existingProfile.innerHTML = `
                    ${avatarHtml}
                    <div class="hidden md:block">
                        <p class="text-sm font-bold text-gray-800">${name}</p>
                        <p class="text-xs text-gray-500">${user.username}</p>
                    </div>
                `;
            }
        }
        
        // wire up logout button in sidebar
        const logoutBtn = document.querySelector('a[href="/admin.html"]');
        if (logoutBtn) {
            logoutBtn.onclick = (e) => {
                e.preventDefault();
                localStorage.removeItem('adminUser');
                window.location.href = '/admin.html';
            };
        }

        // fetch fresh profile data in background
        if (user.id) {
            fetch(`/api/users/${user.id}`)
                .then(res => {
                    if(res.ok) return res.json();
                    throw new Error('Failed to fetch user');
                })
                .then(freshUser => {
                    // update local storage
                    const mergedUser = { ...user, ...freshUser };
                    localStorage.setItem('adminUser', JSON.stringify(mergedUser));
                })
                .catch(err => console.error(err));
        }

    } catch (e) {
        console.error("Error parsing admin data", e);
        localStorage.removeItem('adminUser');
        window.location.href = '/admin.html';
    }
}

// global functions for the modal
function openAdminSettings() {
    const userStr = localStorage.getItem('adminUser');
    if (!userStr) return;
    
    const user = JSON.parse(userStr);
    const modal = document.getElementById('admin-settings-modal');
    if (modal) {
        document.getElementById('admin-setting-name').value = user.full_name || '';
        document.getElementById('admin-setting-password').value = user.password || ''; // fallback password saved at login
        modal.classList.remove('hidden');
    }
}

function closeAdminSettings() {
    const modal = document.getElementById('admin-settings-modal');
    if (modal) modal.classList.add('hidden');
}

async function saveAdminSettings(e) {
    e.preventDefault();
    const userStr = localStorage.getItem('adminUser');
    if (!userStr) return;
    const user = JSON.parse(userStr);
    
    const name = document.getElementById('admin-setting-name').value;
    const password = document.getElementById('admin-setting-password').value;
    const picFile = document.getElementById('admin-setting-pic').files[0];
    
    const fd = new FormData();
    fd.append('fullName', name);
    if (password) fd.append('password', password);
    if (user.profile_pic) fd.append('existingProfilePic', user.profile_pic);
    if (picFile) fd.append('profilePic', picFile);
    
    const btn = document.getElementById('admin-setting-save-btn');
    btn.innerText = "Saving...";
    btn.disabled = true;
    
    try {
        const res = await fetch(`/api/users/${user.id}/settings`, {
            method: 'PUT',
            body: fd
        });
        
        if (res.ok) {
            alert('Settings updated successfully!');
            // refresh to apply new data from background fetch next time
            window.location.reload();
        } else {
            alert('Failed to update settings');
        }
    } catch(err) {
        console.error(err);
        alert('Network error');
    } finally {
        btn.innerText = "Save Changes";
        btn.disabled = false;
    }
}
