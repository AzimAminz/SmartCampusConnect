/**
 * Application Constant Variables
 */
export const API_BASE_URL = 'http://localhost:8080/api';

export const API_ROUTES = {
  LOGIN: `${API_BASE_URL}/auth/login`,
  LOGOUT: `${API_BASE_URL}/auth/logout`,
  USER_PROFILE: `${API_BASE_URL}/users/profile`,
  BOOKINGS: `${API_BASE_URL}/bookings`,
};

export const STORAGE_KEYS = {
  TOKEN: 'auth_token',
  USER: 'user_session',
};
