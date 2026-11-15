import React, { useState } from 'react';
import { EngineerProfile } from '../types';
import { saveEngineerProfile } from '../utils/profileStorage';
import { UserCheck, Shield } from 'lucide-react';

interface ProfileModalProps {
  isOpen: boolean;
  profile: EngineerProfile;
  onSave: (newProfile: EngineerProfile) => void;
  isFirstLaunch?: boolean;
}

export const ProfileModal: React.FC<ProfileModalProps> = ({
  isOpen,
  profile,
  onSave,
  isFirstLaunch = false
}) => {
  const [name, setName] = useState(profile.fullName || '');
  const [initials, setInitials] = useState(profile.initials || '');
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSave = () => {
    const cleanName = name.trim();
    const cleanInitials = initials.trim().toUpperCase();

    if (!cleanName) {
      setError('Please enter your full name.');
      return;
    }
    if (cleanInitials.length < 2 || cleanInitials.length > 4) {
      setError('Initials must be 2 to 4 letters (e.g. RK, AS).');
      return;
    }

    saveEngineerProfile(cleanName, cleanInitials);
    onSave({
      fullName: cleanName,
      initials: cleanInitials,
      isConfigured: true
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full overflow-hidden border border-slate-200">
        <div className="bg-navy p-6 text-white text-center relative">
          <div className="w-14 h-14 bg-amber-400 text-navy font-bold text-xl rounded-full flex items-center justify-center mx-auto mb-3 shadow-md">
            {initials ? initials.toUpperCase() : <UserCheck size={28} />}
          </div>
          <h2 className="text-xl font-bold">
            {isFirstLaunch ? 'Welcome to SnagOps' : 'Edit Engineer Profile'}
          </h2>
          <p className="text-xs text-slate-300 mt-1">
            Multi-User Quality & NCR Tracking. Your initials prefix all logged entries (e.g. OBS-{initials || 'XX'}-001).
          </p>
        </div>

        <div className="p-6 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Full Name *
            </label>
            <input
              type="text"
              placeholder="e.g. Rahul Kumar"
              value={name}
              onChange={e => {
                setName(e.target.value);
                if (!initials && e.target.value) {
                  const parts = e.target.value.trim().split(/\s+/);
                  if (parts.length >= 2) {
                    setInitials((parts[0][0] + parts[parts.length - 1][0]).toUpperCase());
                  }
                }
              }}
              className="w-full px-4 py-3 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-navy focus:border-transparent text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Engineer Initials (2–4 Letters) *
            </label>
            <input
              type="text"
              maxLength={4}
              placeholder="e.g. RK"
              value={initials}
              onChange={e => setInitials(e.target.value.toUpperCase())}
              className="w-full px-4 py-3 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-navy focus:border-transparent text-sm uppercase font-mono font-bold tracking-wider"
            />
            <p className="text-[11px] text-slate-500 mt-1">
              Used for collision-free IDs (OBS-{initials || 'XX'}-001, NCR-{initials || 'XX'}-001) and digital sign-offs.
            </p>
          </div>

          {error && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg">
              {error}
            </div>
          )}

          <div className="pt-2">
            <button
              onClick={handleSave}
              className="w-full py-3.5 bg-navy text-white rounded-xl font-bold text-sm shadow hover:bg-slate-800 transition-colors flex items-center justify-center gap-2"
            >
              <Shield size={16} />
              {isFirstLaunch ? 'Get Started' : 'Save Profile'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
