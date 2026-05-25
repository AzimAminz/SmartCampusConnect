import { ApiClient } from './api.js';
import { API_ROUTES } from '../utils/constants.js';
import { SessionManager } from '../utils/session.js';

export const AuthService = {
  async login(username, password) {
    try {
      const response = await ApiClient.post(API_ROUTES.LOGIN, { username, password });
      if (response && response.token) {
        SessionManager.setToken(response.token);
        SessionManager.setCurrentUser(response.user);
        return response.user;
      }
      throw new Error('Authentication failed: No token returned');
    } catch (error) {
      throw error;
    }
  },

  async logout() {
    try {
      // Notify backend if there is an endpoint
      await ApiClient.post(API_ROUTES.LOGOUT, {}).catch(() => {});
    } finally {
      SessionManager.clear();
      window.location.reload();
    }
  }
};
