import { AuthService } from '../services/auth.js';

export const LoginView = {
  render() {
    return `
      <div class="auth-container glass-panel">
        <h2 style="margin-bottom: 0.5rem; font-size: 2rem; background: linear-gradient(135deg, #FFF, #9CA3AF); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">Sign In</h2>
        <p style="color: var(--text-secondary); margin-bottom: 2rem;">Smart Campus Connect - Frontend Portal</p>
        
        <form id="login-form">
          <div class="form-group">
            <label for="username" style="font-size: 0.85rem; font-weight: 500; color: var(--text-secondary);">Username</label>
            <input type="text" id="username" class="form-control" placeholder="Enter your username" required autocomplete="username">
          </div>
          
          <div class="form-group" style="margin-bottom: 2rem;">
            <label for="password" style="font-size: 0.85rem; font-weight: 500; color: var(--text-secondary);">Password</label>
            <input type="password" id="password" class="form-control" placeholder="••••••••" required autocomplete="current-password">
          </div>

          <div id="error-message" style="color: var(--danger); font-size: 0.9rem; margin-bottom: 1.5rem; display: none;"></div>
          
          <button type="submit" class="btn-primary" style="width: 100%;">
            Sign In
          </button>
        </form>
      </div>
    `;
  },

  afterRender(navigateTo) {
    const form = document.getElementById('login-form');
    const errorDiv = document.getElementById('error-message');

    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      
      const username = document.getElementById('username').value;
      const password = document.getElementById('password').value;
      
      errorDiv.style.display = 'none';
      
      try {
        await AuthService.login(username, password);
        navigateTo('dashboard');
      } catch (err) {
        errorDiv.textContent = err.message || 'Invalid username or password.';
        errorDiv.style.display = 'block';
      }
    });
  }
};
