import { EngineerProfile } from '../types';

const STORAGE_KEY_NAME = 'snagops_profile_name';
const STORAGE_KEY_INITIALS = 'snagops_profile_initials';

export function getEngineerProfile(): EngineerProfile {
  const name = localStorage.getItem(STORAGE_KEY_NAME) || '';
  const initials = localStorage.getItem(STORAGE_KEY_INITIALS) || '';
  return {
    fullName: name,
    initials: initials.toUpperCase(),
    isConfigured: name.trim().length > 0 && initials.trim().length >= 2
  };
}

export function saveEngineerProfile(fullName: string, initials: string): void {
  const cleanInitials = initials.trim().toUpperCase().slice(0, 4);
  localStorage.setItem(STORAGE_KEY_NAME, fullName.trim());
  localStorage.setItem(STORAGE_KEY_INITIALS, cleanInitials);
}
