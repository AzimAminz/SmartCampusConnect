import { ApiClient } from './api.js';
import { API_ROUTES } from '../utils/constants.js';
import { SessionManager } from '../utils/session.js';

export const AuthService = {
  async login(userId) {
    try {
      const response = await ApiClient.post(API_ROUTES.LOGIN, { userId });
      if (response && response.token) {
        SessionManager.setToken(response.token);
        const userObj = {
          userId: response.userId,
          role: response.role,
          fullName: response.fullName
        };
        SessionManager.setCurrentUser(userObj);
        return userObj;
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
