import { AuthService } from "../services/auth.js";

export const LoginView = {
  render() {
    return `
      <div class="login-shell">
        <div class="auth-container glass-panel login-card">
          <div class="login-hero">
            <div class="login-icon">🏫</div>
            <h2 class="login-title">SmartCampus Connect</h2>
          </div>
          
          <form id="login-form">
            <div class="form-group login-field">
              <label for="userId" class="login-label">Matric No / Admin ID</label>
              <input type="text" id="userId" class="form-control login-input" placeholder="e.g., B032310001 or ADMIN" required>
            </div>

            <div id="error-message" class="login-error"></div>
            
            <button type="submit" class="btn-primary login-button">
              Sign In
            </button>
          </form>

          <div class="login-branding">
            <img src="./omgosh-final.png" alt="Logo" class="login-logo">
          </div>

          <div class="login-demo">
            <p class="login-demo-title">Demo Accounts</p>
            <p>Student: <code class="login-code login-code-secondary">B032310001</code> to <code class="login-code login-code-secondary">B032310005</code></p>
            <p>Admin: <code class="login-code login-code-primary">ADMIN</code></p>
          </div>
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
