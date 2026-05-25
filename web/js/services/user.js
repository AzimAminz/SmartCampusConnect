import { ApiClient } from './api.js';
import { API_ROUTES } from '../utils/constants.js';
import { SessionManager } from '../utils/session.js';

export const UserService = {
  async fetchUserProfile() {
    try {
      const profile = await ApiClient.get(API_ROUTES.USER_PROFILE);
      SessionManager.setCurrentUser(profile);
      return profile;
    } catch (error) {
      throw error;
    }
  }
};
