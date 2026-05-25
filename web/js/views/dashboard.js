import { SessionManager } from '../utils/session.js';
import { AuthService } from '../services/auth.js';

export const DashboardView = {
  render() {
    const user = SessionManager.getCurrentUser() || { username: 'Guest' };
    
    return `
      <div>
        <header class="header-nav glass-panel" style="border-radius: 0 0 var(--radius-lg) var(--radius-lg); margin-bottom: 2rem;">
          <div class="logo" style="display: flex; align-items: center; gap: 0.5rem;">
            <div style="width: 32px; height: 32px; border-radius: 50%; background: linear-gradient(135deg, var(--primary), var(--secondary));"></div>
            <span class="logo-text" style="font-size: 1.25rem; letter-spacing: -0.5px;">SmartCampus</span>
          </div>
          <div style="display: flex; align-items: center; gap: 1.5rem;">
            <span style="font-weight: 500;">Hi, ${user.username}</span>
            <button id="logout-btn" class="btn-primary" style="background: rgba(239, 68, 68, 0.15); color: var(--danger); border: 1px solid rgba(239, 68, 68, 0.2); padding: 0.5rem 1rem; font-size: 0.85rem;">
              Logout
            </button>
          </div>
        </header>

        <main style="max-width: 1200px; margin: 0 auto; padding: 0 2rem;">
          <div style="margin-bottom: 2rem;">
            <h1 style="font-size: 2.2rem; margin-bottom: 0.5rem;">Dashboard</h1>
            <p style="color: var(--text-secondary);">Welcome back to Smart Campus Connect frontend application portal.</p>
          </div>

          <div class="dashboard-grid">
            <div class="dashboard-card glass-panel">
              <h3 style="margin-bottom: 0.5rem; color: var(--text-secondary); font-size: 0.9rem; text-transform: uppercase; letter-spacing: 0.5px;">Active Bookings</h3>
              <div style="font-size: 2.5rem; font-weight: 700; margin-bottom: 1rem;">12</div>
              <p style="color: var(--success); font-size: 0.85rem;">+2 new bookings today</p>
            </div>
            
            <div class="dashboard-card glass-panel">
              <h3 style="margin-bottom: 0.5rem; color: var(--text-secondary); font-size: 0.9rem; text-transform: uppercase; letter-spacing: 0.5px;">Available Facilities</h3>
              <div style="font-size: 2.5rem; font-weight: 700; margin-bottom: 1rem;">8 / 10</div>
              <p style="color: var(--text-secondary); font-size: 0.85rem;">2 undergoing maintenance</p>
            </div>

            <div class="dashboard-card glass-panel">
              <h3 style="margin-bottom: 0.5rem; color: var(--text-secondary); font-size: 0.9rem; text-transform: uppercase; letter-spacing: 0.5px;">Server Connection</h3>
              <div style="font-size: 1.5rem; font-weight: 600; margin: 1rem 0; color: var(--success); display: flex; align-items: center; gap: 0.5rem;">
                <span style="width: 12px; height: 12px; border-radius: 50%; background-color: var(--success); display: inline-block;"></span>
                Online
              </div>
              <p style="color: var(--text-secondary); font-size: 0.85rem;">Connected to API Backend</p>
            </div>
          </div>
        </main>
      </div>
    `;
  },

  afterRender(navigateTo) {
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', async () => {
        await AuthService.logout();
        navigateTo('login');
      });
    }
  }
};
