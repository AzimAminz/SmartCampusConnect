import { SessionManager } from '../utils/session.js';

export const ApiClient = {
  async request(url, options = {}) {
    const headers = new Headers(options.headers || {});
    
    // Auto-inject token if user is authenticated
    const token = SessionManager.getToken();
    if (token) {
      headers.set('X-Auth-Token', token);
    }
    
    if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json');
    }

    const config = {
      ...options,
      headers
    };

    try {
      const response = await fetch(url, config);
      
      // Auto logout on 401 Unauthorized
      if (response.status === 401) {
        SessionManager.clear();
        window.location.reload();
        throw new Error('Session expired. Please log in again.');
      }
      
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || `Request failed with status ${response.status}`);
      }
      return data;
    } catch (error) {
      console.error('API Request Error:', error);
      throw error;
    }
  },

  get(url, options = {}) {
    return this.request(url, { ...options, method: 'GET' });
  },

  post(url, body, options = {}) {
    return this.request(url, {
      ...options,
      method: 'POST',
      body: JSON.stringify(body)
    });
  },

  put(url, body, options = {}) {
    return this.request(url, {
      ...options,
      method: 'PUT',
      body: JSON.stringify(body)
    });
  },

  delete(url, options = {}) {
    return this.request(url, { ...options, method: 'DELETE' });
  }
};
