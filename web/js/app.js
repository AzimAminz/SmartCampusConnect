import { SessionManager } from './utils/session.js';
import { LoginView } from './views/login.js';
import { DashboardView } from './views/dashboard.js';
import { ProfileView } from './views/profile.js';

// Route list
const routes = {
  login: LoginView,
  dashboard: DashboardView,
  profile: ProfileView
};

const appContainer = document.getElementById('app');

/**
 * Client-side Router
 */
function navigateTo(route) {
  // Authentication Guard
  const isAuthenticated = SessionManager.isAuthenticated();
  
  let targetRoute = route;
  
  if (!isAuthenticated && targetRoute !== 'login') {
    targetRoute = 'login';
  } else if (isAuthenticated && targetRoute === 'login') {
    targetRoute = 'dashboard';
  }

  const view = routes[targetRoute] || routes.login;
  
  // Render View HTML
  appContainer.innerHTML = view.render();
  
  // Bind View Event Listeners
  if (view.afterRender) {
    view.afterRender(navigateTo);
  }
  
  // Update browser history state
  window.location.hash = targetRoute;
}

// Listen for hashchange events
window.addEventListener('hashchange', () => {
  const route = window.location.hash.slice(1) || 'login';
  navigateTo(route);
});

// App Initialization
document.addEventListener('DOMContentLoaded', () => {
  const initialRoute = window.location.hash.slice(1) || 'login';
  navigateTo(initialRoute);
});
