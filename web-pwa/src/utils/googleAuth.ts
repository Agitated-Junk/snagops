import { GoogleUser } from '../types';

export const DEFAULT_GOOGLE_CLIENT_ID = '722524210515-ul7ubarai8ppjlf8rbkgvmd203plan0e.apps.googleusercontent.com';

const STORAGE_KEY_CLIENT_ID = 'snagops_google_client_id';
const STORAGE_KEY_USER = 'snagops_google_user';

declare global {
  interface Window {
    google?: any;
  }
}

/**
 * Returns active Google Client ID (custom from localStorage or hardcoded default).
 */
export function getGoogleClientId(): string {
  const custom = localStorage.getItem(STORAGE_KEY_CLIENT_ID);
  if (custom && custom.trim().length > 0) {
    return custom.trim();
  }
  return DEFAULT_GOOGLE_CLIENT_ID;
}

/**
 * Saves a custom Client ID or removes it to revert to default.
 */
export function setGoogleClientId(clientId: string | null): void {
  if (!clientId || clientId.trim().length === 0 || clientId.trim() === DEFAULT_GOOGLE_CLIENT_ID) {
    localStorage.removeItem(STORAGE_KEY_CLIENT_ID);
  } else {
    localStorage.setItem(STORAGE_KEY_CLIENT_ID, clientId.trim());
  }
}

/**
 * Checks whether user has configured a custom Client ID.
 */
export function isCustomGoogleClientId(): boolean {
  const custom = localStorage.getItem(STORAGE_KEY_CLIENT_ID);
  return !!custom && custom.trim().length > 0 && custom.trim() !== DEFAULT_GOOGLE_CLIENT_ID;
}

/**
 * Gets currently signed-in Google user from storage.
 */
export function getStoredGoogleUser(): GoogleUser | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_USER);
    if (!raw) return null;
    return JSON.parse(raw) as GoogleUser;
  } catch {
    return null;
  }
}

/**
 * Persists Google user profile to localStorage.
 */
export function saveGoogleUser(user: GoogleUser | null): void {
  if (!user) {
    localStorage.removeItem(STORAGE_KEY_USER);
  } else {
    localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(user));
  }
}

/**
 * Decodes standard base64url encoded JWT payload.
 */
export function parseJwtPayload(token: string): any {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return {};
  }
}

/**
 * Initializes Google Identity Services (GIS) Sign-In with Google.
 */
export function initGoogleIdentityServices(
  onSuccess: (user: GoogleUser) => void,
  onError?: (err: any) => void
): boolean {
  if (typeof window === 'undefined' || !window.google?.accounts?.id) {
    return false;
  }

  const clientId = getGoogleClientId();

  try {
    window.google.accounts.id.initialize({
      client_id: clientId,
      callback: (response: any) => {
        if (response.credential) {
          const payload = parseJwtPayload(response.credential);
          const user: GoogleUser = {
            email: payload.email || '',
            name: payload.name || payload.given_name || 'Google User',
            picture: payload.picture,
            idToken: response.credential,
            expiresAt: payload.exp ? payload.exp * 1000 : Date.now() + 3600000
          };
          saveGoogleUser(user);
          onSuccess(user);
        } else if (onError) {
          onError(new Error('No credentials returned from Google Sign-In.'));
        }
      },
      auto_select: false,
      cancel_on_tap_outside: true
    });
    return true;
  } catch (err) {
    if (onError) onError(err);
    return false;
  }
}

/**
 * Renders native Google Sign-In button into a DOM container.
 */
export function renderGoogleSignInButton(
  container: HTMLElement,
  onSuccess: (user: GoogleUser) => void
): void {
  const initialized = initGoogleIdentityServices(onSuccess);
  if (initialized && window.google?.accounts?.id) {
    window.google.accounts.id.renderButton(container, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      text: 'signin_with',
      shape: 'rectangular',
      logo_alignment: 'left',
      width: 260
    });
  }
}

/**
 * Requests OAuth2 Access Token for Google Drive & Google Sheets scopes.
 */
export function requestGoogleOAuthToken(
  scopes = 'https://www.googleapis.com/auth/spreadsheets https://www.googleapis.com/auth/drive.file'
): Promise<string> {
  return new Promise((resolve, reject) => {
    if (typeof window === 'undefined' || !window.google?.accounts?.oauth2) {
      reject(new Error('Google Identity Services script is not loaded.'));
      return;
    }

    const clientId = getGoogleClientId();

    try {
      const client = window.google.accounts.oauth2.initTokenClient({
        client_id: clientId,
        scope: scopes,
        callback: (tokenResponse: any) => {
          if (tokenResponse.error) {
            reject(new Error(tokenResponse.error_description || tokenResponse.error));
            return;
          }
          if (tokenResponse.access_token) {
            // Update stored user's access token
            const currentUser = getStoredGoogleUser();
            if (currentUser) {
              currentUser.accessToken = tokenResponse.access_token;
              currentUser.expiresAt = Date.now() + (tokenResponse.expires_in || 3600) * 1000;
              saveGoogleUser(currentUser);
            }
            resolve(tokenResponse.access_token);
          } else {
            reject(new Error('No access token received from Google.'));
          }
        }
      });
      client.requestAccessToken();
    } catch (err) {
      reject(err);
    }
  });
}

/**
 * Signs out from Google and clears stored session.
 */
export function signOutGoogle(): void {
  const user = getStoredGoogleUser();
  if (user?.email && window.google?.accounts?.id) {
    try {
      window.google.accounts.id.revoke(user.email, () => {});
    } catch {}
  }
  saveGoogleUser(null);
}
