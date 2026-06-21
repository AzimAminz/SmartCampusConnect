import { SessionManager } from '../utils/session.js';

export const ProfileView = {
  render() {
    const user = SessionManager.getCurrentUser() || { username: 'Guest', email: 'guest@utem.edu.my', role: 'Student' };
    
    return `
      <div class="glass-panel profile-card">
        <div class="profile-header">
          <h2>User Profile</h2>
          <p>Manage your smart campus profile account</p>
        </div>

        <div class="profile-fields">
          <div class="form-group">
            <label style="font-size: 0.85rem; font-weight: 500; color: var(--text-secondary);">Username</label>
            <input type="text" class="form-control profile-input" value="${user.username}" readonly style="cursor: not-allowed; opacity: 0.8;">
          </div>
          
          <div class="form-group">
            <label style="font-size: 0.85rem; font-weight: 500; color: var(--text-secondary);">Email Address</label>
            <input type="email" class="form-control profile-input" value="${user.email || 'N/A'}" readonly style="cursor: not-allowed; opacity: 0.8;">
          </div>

          <div class="form-group" style="margin-bottom: 2rem;">
            <label style="font-size: 0.85rem; font-weight: 500; color: var(--text-secondary);">User Role</label>
            <input type="text" class="form-control profile-input" value="${user.role || 'User'}" readonly style="cursor: not-allowed; opacity: 0.8;">
          </div>
        </div>
      </div>
    `;
  },

  afterRender(navigateTo) {
    // Component lifecycle event listener placeholder
  }
};
