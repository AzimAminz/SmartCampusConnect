import { AuthService } from "../services/auth.js";

export const LoginView = {
  render() {
    return `
      <div class="auth-container glass-panel" style="padding: 3rem 2.5rem; max-width: 460px; margin: auto; display: flex; flex-direction: column; gap: 1.5rem;">
        <div style="display: flex; justify-content: center; align-items: center; gap: 10px; margin-bottom: 1rem;">
        <img src="./omgosh-final.png" alt="Logo" style="height: 80px; width: auto;">
      </div>
        
        <form id="login-form">
          <div class="form-group" style="margin-bottom: 1.5rem;">
            <label for="userId" style="font-size: 0.85rem; font-weight: 600; color: var(--text-secondary); margin-bottom: 0.5rem; display: block;">Matric No / Admin ID</label>
            <input type="text" id="userId" class="form-control" placeholder="e.g., B032310001 or ADMIN" required style="width: 100%; text-transform: uppercase;">
          </div>

          <div id="error-message" style="color: var(--danger); font-size: 0.85rem; margin-bottom: 1.5rem; display: none; padding: 0.75rem; background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.2); border-radius: var(--radius-sm); text-align: center;"></div>
          
          <button type="submit" class="btn-primary" style="width: 100%; padding: 0.9rem; font-size: 1rem; border-radius: var(--radius-md);">
            Sign In
          </button>
        </form>

        <div style="margin-top: 1.5rem; padding-top: 1.5rem; border-top: 1px solid rgba(255, 255, 255, 0.05); text-align: center; color: var(--text-secondary); font-size: 0.8rem; line-height: 1.6;">
          <p style="font-weight: 600; margin-bottom: 0.25rem; color: rgba(255,255,255,0.7);">Demo Accounts</p>
          <p>Student: <code style="color: var(--secondary); background: rgba(6, 182, 212, 0.1); padding: 2px 6px; border-radius: 4px;">B032310001</code> to <code style="color: var(--secondary); background: rgba(6, 182, 212, 0.1); padding: 2px 6px; border-radius: 4px;">B032310005</code></p>
          <p>Admin: <code style="color: var(--primary); background: rgba(79, 70, 229, 0.1); padding: 2px 6px; border-radius: 4px;">ADMIN</code></p>
        </div>
      </div>
    `;
  },

  afterRender(navigateTo) {
    const form = document.getElementById("login-form");
    const errorDiv = document.getElementById("error-message");

    form.addEventListener("submit", async (e) => {
      e.preventDefault();

      const userId = document.getElementById("userId").value.trim();
      errorDiv.style.display = "none";

      try {
        await AuthService.login(userId);
        navigateTo("dashboard");
      } catch (err) {
        errorDiv.textContent =
          err.message || "Invalid username or credentials.";
        errorDiv.style.display = "block";
      }
    });
  },
};
