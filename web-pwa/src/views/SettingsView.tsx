import React, { useState, useEffect, useRef } from 'react';
import {
  Project,
  EngineerProfile,
  Block,
  Floor,
  SubLocation,
  Vendor,
  CustomFieldDefinition,
  GoogleUser
} from '../types';
import { db } from '../db/db';
import {
  DEFAULT_GOOGLE_CLIENT_ID,
  getGoogleClientId,
  setGoogleClientId,
  isCustomGoogleClientId,
  getStoredGoogleUser,
  signOutGoogle,
  renderGoogleSignInButton,
  requestGoogleOAuthToken
} from '../utils/googleAuth';
import {
  User,
  Share2,
  Download,
  Plus,
  Trash2,
  Layers,
  Building,
  Sparkles,
  Settings,
  Mail,
  Check,
  ShieldCheck,
  Key,
  LogIn,
  LogOut,
  AlertCircle
} from 'lucide-react';

interface SettingsViewProps {
  project: Project;
  profile: EngineerProfile;
  onOpenProfile: () => void;
  onOpenProjectConfig: (mode: 'SHARE' | 'JOIN') => void;
  onProjectUpdated: (project: Project) => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  project,
  profile,
  onOpenProfile,
  onOpenProjectConfig,
  onProjectUpdated
}) => {
  // Master Data state
  const [blocks, setBlocks] = useState<Block[]>([]);
  const [floors, setFloors] = useState<Floor[]>([]);
  const [subLocations, setSubLocations] = useState<SubLocation[]>([]);
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [customFields, setCustomFields] = useState<CustomFieldDefinition[]>([]);

  // Selected Block/Floor for filtering hierarchy editor
  const [activeBlockId, setActiveBlockId] = useState<number | null>(null);
  const [activeFloorId, setActiveFloorId] = useState<number | null>(null);

  // New item inputs
  const [newBlockName, setNewBlockName] = useState('');
  const [newFloorName, setNewFloorName] = useState('');
  const [newPresetName, setNewPresetName] = useState('');

  // New Vendor inputs
  const [newVendorName, setNewVendorName] = useState('');
  const [newVendorTrade, setNewVendorTrade] = useState('');
  const [newVendorEmail, setNewVendorEmail] = useState('');

  // Project settings editing
  const [clientName, setClientName] = useState(project.clientContractorName || '');
  const [spreadsheetId, setSpreadsheetId] = useState(project.spreadsheetId || '');
  const [ccEmails, setCcEmails] = useState(project.defaultCcEmails || '');
  const [emailFrequency, setEmailFrequency] = useState(project.emailFrequency || 'Daily');
  const [saveSettingsSuccess, setSaveSettingsSuccess] = useState(false);

  // Google OAuth State
  const [googleUser, setGoogleUser] = useState<GoogleUser | null>(getStoredGoogleUser());
  const [activeClientId, setActiveClientId] = useState<string>(getGoogleClientId());
  const [isCustomClient, setIsCustomClient] = useState<boolean>(isCustomGoogleClientId());
  const [showOverrideInput, setShowOverrideInput] = useState<boolean>(false);
  const [customInputVal, setCustomInputVal] = useState<string>('');
  const [authSuccessMsg, setAuthSuccessMsg] = useState<string>('');
  const [authErrorMsg, setAuthErrorMsg] = useState<string>('');
  const googleBtnContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (googleBtnContainerRef.current && !googleUser) {
      renderGoogleSignInButton(googleBtnContainerRef.current, (user) => {
        setGoogleUser(user);
        setAuthSuccessMsg(`Signed in as ${user.name}`);
        setTimeout(() => setAuthSuccessMsg(''), 3000);
      });
    }
  }, [googleUser, activeClientId]);

  const handleManualGoogleSignIn = async () => {
    setAuthErrorMsg('');
    setAuthSuccessMsg('');
    try {
      const token = await requestGoogleOAuthToken();
      const updated = getStoredGoogleUser();
      if (updated) {
        setGoogleUser(updated);
        setAuthSuccessMsg(`Signed in as ${updated.name || updated.email}`);
      } else {
        const fallbackUser: GoogleUser = {
          name: 'Google Authorized User',
          email: 'Authenticated',
          accessToken: token
        };
        setGoogleUser(fallbackUser);
        setAuthSuccessMsg('Google OAuth authorized successfully.');
      }
      setTimeout(() => setAuthSuccessMsg(''), 3000);
    } catch (err: any) {
      setAuthErrorMsg(err?.message || 'Google Sign-In was cancelled or unavailable.');
    }
  };

  const handleSignOutGoogle = () => {
    signOutGoogle();
    setGoogleUser(null);
    setAuthSuccessMsg('Signed out of Google account.');
    setTimeout(() => setAuthSuccessMsg(''), 2500);
  };

  const handleSaveCustomClientId = () => {
    const clean = customInputVal.trim();
    if (!clean) {
      setGoogleClientId(null);
    } else {
      setGoogleClientId(clean);
    }
    setActiveClientId(getGoogleClientId());
    setIsCustomClient(isCustomGoogleClientId());
    setShowOverrideInput(false);
    setCustomInputVal('');
    setAuthSuccessMsg('Google Client ID configuration updated.');
    setTimeout(() => setAuthSuccessMsg(''), 2500);
  };

  const handleResetDefaultClientId = () => {
    setGoogleClientId(null);
    setActiveClientId(DEFAULT_GOOGLE_CLIENT_ID);
    setIsCustomClient(false);
    setShowOverrideInput(false);
    setCustomInputVal('');
    setAuthSuccessMsg('Reset to default hardcoded Web Client ID.');
    setTimeout(() => setAuthSuccessMsg(''), 2500);
  };

  const loadData = async () => {
    if (!project.id) return;
    const bList = await db.blocks.where('projectId').equals(project.id).toArray();
    const fList = await db.floors.where('projectId').equals(project.id).toArray();
    const sList = await db.subLocations.where('projectId').equals(project.id).toArray();
    const vList = await db.vendors.where('projectId').equals(project.id).toArray();
    const cList = await db.customFields.where('projectId').equals(project.id).toArray();

    setBlocks(bList);
    setFloors(fList);
    setSubLocations(sList);
    setVendors(vList);
    setCustomFields(cList);

    if (bList.length > 0 && !activeBlockId) {
      setActiveBlockId(bList[0].id!);
    }
  };

  useEffect(() => {
    loadData();
    setClientName(project.clientContractorName || '');
    setSpreadsheetId(project.spreadsheetId || '');
    setCcEmails(project.defaultCcEmails || '');
    setEmailFrequency(project.emailFrequency || 'Daily');
  }, [project.id]);

  useEffect(() => {
    if (activeBlockId) {
      const blockFloors = floors.filter(f => f.blockId === activeBlockId);
      if (blockFloors.length > 0) {
        setActiveFloorId(blockFloors[0].id!);
      } else {
        setActiveFloorId(null);
      }
    }
  }, [activeBlockId, floors]);

  // Master Data Add Handlers
  const handleAddBlock = async () => {
    if (!newBlockName.trim() || !project.id) return;
    await db.blocks.add({
      recordUuid: `blk-${Date.now()}`,
      projectId: project.id,
      name: newBlockName.trim()
    });
    setNewBlockName('');
    loadData();
  };

  const handleAddFloor = async () => {
    if (!newFloorName.trim() || !project.id || !activeBlockId) return;
    await db.floors.add({
      recordUuid: `flr-${Date.now()}`,
      projectId: project.id,
      blockId: activeBlockId,
      name: newFloorName.trim()
    });
    setNewFloorName('');
    loadData();
  };

  const handleAddPreset = async () => {
    if (!newPresetName.trim() || !project.id || !activeFloorId) return;
    await db.subLocations.add({
      recordUuid: `sub-${Date.now()}`,
      projectId: project.id,
      floorId: activeFloorId,
      name: newPresetName.trim()
    });
    setNewPresetName('');
    loadData();
  };

  const handleAddVendor = async () => {
    if (!newVendorName.trim() || !project.id) return;
    await db.vendors.add({
      recordUuid: `ven-${Date.now()}`,
      projectId: project.id,
      name: newVendorName.trim(),
      trade: newVendorTrade.trim() || 'General',
      primaryEmail: newVendorEmail.trim(),
      ccEmails: ''
    });
    setNewVendorName('');
    setNewVendorTrade('');
    setNewVendorEmail('');
    loadData();
  };

  const handleSaveProjectSettings = async () => {
    if (!project.id) return;
    await db.projects.update(project.id, {
      clientContractorName: clientName.trim(),
      spreadsheetId: spreadsheetId.trim(),
      defaultCcEmails: ccEmails.trim(),
      emailFrequency,
      updatedAt: Date.now()
    });
    const updated = await db.projects.get(project.id);
    if (updated) {
      onProjectUpdated(updated);
    }
    setSaveSettingsSuccess(true);
    setTimeout(() => setSaveSettingsSuccess(false), 2500);
  };

  return (
    <div className="max-w-2xl mx-auto p-4 pb-24 space-y-4">
      {/* 1. Engineer Profile Card */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-full bg-amber-400 text-navy font-bold text-base flex items-center justify-center font-mono shadow-sm">
            {profile.initials || '??'}
          </div>
          <div>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
              Device Engineer Profile
            </span>
            <h3 className="font-bold text-sm text-slate-800">
              {profile.fullName || 'Not Configured'}
            </h3>
            <p className="text-xs text-slate-500 font-mono">
              Initials: {profile.initials || '--'} (Prefix for OBS & NCR IDs)
            </p>
          </div>
        </div>

        <button
          onClick={onOpenProfile}
          className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-xl"
        >
          Edit
        </button>
      </div>

      {/* 2. Team Collaboration & Config Sharing */}
      <div className="bg-navy text-white p-4 rounded-2xl shadow-sm space-y-3">
        <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider">
          <Share2 size={16} className="text-amber-400" />
          <span>Team Collaboration & Project Sync</span>
        </div>
        <p className="text-xs text-slate-300">
          Share your project config code with on-site colleagues so they can join this project in one tap without manually retyping IDs.
        </p>
        <div className="grid grid-cols-2 gap-2 pt-1">
          <button
            onClick={() => onOpenProjectConfig('SHARE')}
            className="py-2.5 px-3 bg-white/10 hover:bg-white/20 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-colors"
          >
            <Share2 size={14} />
            <span>Share Config Code</span>
          </button>
          <button
            onClick={() => onOpenProjectConfig('JOIN')}
            className="py-2.5 px-3 bg-amber-400 hover:bg-amber-300 text-navy rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-colors shadow"
          >
            <Download size={14} />
            <span>Join via Code</span>
          </button>
        </div>
      </div>

      {/* Google Cloud & OAuth Integration */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-700 uppercase tracking-wider">
            <ShieldCheck size={16} className="text-navy" />
            <span>Google Cloud & OAuth Integration</span>
          </div>
          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
            isCustomClient ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
          }`}>
            {isCustomClient ? 'Custom Client ID' : 'Default Client (Hardcoded)'}
          </span>
        </div>

        <p className="text-xs text-slate-500">
          Enables one-tap Google Sign-In for team members on iOS Safari and web browsers, authorizing Google Drive & Sheets synchronization.
        </p>

        {/* User Sign-In Status Card */}
        {googleUser ? (
          <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center justify-between">
            <div className="flex items-center gap-3">
              {googleUser.picture ? (
                <img src={googleUser.picture} alt={googleUser.name} className="w-10 h-10 rounded-full border border-emerald-300" />
              ) : (
                <div className="w-10 h-10 rounded-full bg-emerald-600 text-white font-bold flex items-center justify-center text-sm">
                  {googleUser.name.charAt(0) || 'G'}
                </div>
              )}
              <div className="text-xs">
                <p className="font-bold text-emerald-950">{googleUser.name}</p>
                <p className="text-emerald-700 text-[11px]">{googleUser.email}</p>
                <span className="text-[10px] text-emerald-600 font-medium">✓ Google Account Connected</span>
              </div>
            </div>
            <button
              onClick={handleSignOutGoogle}
              className="px-3 py-1.5 bg-white border border-emerald-300 text-red-600 hover:bg-red-50 text-xs font-bold rounded-xl flex items-center gap-1 shadow-sm"
            >
              <LogOut size={13} /> Sign Out
            </button>
          </div>
        ) : (
          <div className="space-y-2">
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl flex flex-col sm:flex-row items-center justify-between gap-3">
              <div className="text-xs text-center sm:text-left">
                <p className="font-bold text-slate-800">Google Sign-In Ready</p>
                <p className="text-[11px] text-slate-500">Sign in to authorize sheets and cloud outbox sync.</p>
              </div>
              <div className="flex flex-col sm:flex-row items-center gap-2">
                <div ref={googleBtnContainerRef} />
                <button
                  onClick={handleManualGoogleSignIn}
                  className="px-3 py-2 bg-navy text-white text-xs font-bold rounded-xl hover:bg-slate-800 flex items-center gap-1.5 shadow"
                >
                  <LogIn size={14} /> Tap to Authorize Google
                </button>
              </div>
            </div>
          </div>
        )}

        {authSuccessMsg && (
          <div className="p-2.5 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-xl flex items-center gap-1.5 animate-fadeIn">
            <Check size={14} className="text-emerald-600" />
            <span>{authSuccessMsg}</span>
          </div>
        )}

        {authErrorMsg && (
          <div className="p-2.5 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl flex items-center gap-1.5 animate-fadeIn">
            <AlertCircle size={14} className="text-red-600 shrink-0" />
            <span>{authErrorMsg}</span>
          </div>
        )}

        {/* Client ID Details & Manual Override */}
        <div className="pt-2 border-t border-slate-100 text-xs space-y-2">
          <div className="flex items-center justify-between">
            <span className="font-semibold text-slate-600">Active Web OAuth Client ID:</span>
            <button
              onClick={() => setShowOverrideInput(!showOverrideInput)}
              className="text-navy font-bold hover:underline text-[11px]"
            >
              {showOverrideInput ? 'Cancel Override' : 'Manual Override'}
            </button>
          </div>

          <div className="p-2 bg-slate-100 rounded-xl font-mono text-[11px] text-slate-700 break-all select-all">
            {activeClientId}
          </div>

          {showOverrideInput && (
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl space-y-2 animate-fadeIn">
              <label className="block text-[11px] font-bold text-amber-900">
                Custom Google Web Client ID:
              </label>
              <input
                type="text"
                placeholder="Paste custom OAuth Client ID (...apps.googleusercontent.com)"
                value={customInputVal}
                onChange={e => setCustomInputVal(e.target.value)}
                className="w-full p-2 rounded-lg border border-amber-300 font-mono text-xs bg-white"
              />
              <div className="flex gap-2">
                <button
                  onClick={handleResetDefaultClientId}
                  className="flex-1 py-1.5 bg-white border border-slate-300 text-slate-700 font-bold rounded-lg text-xs"
                >
                  Reset to Default
                </button>
                <button
                  onClick={handleSaveCustomClientId}
                  className="flex-1 py-1.5 bg-navy text-white font-bold rounded-lg text-xs"
                >
                  Save Custom ID
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 3. Location Hierarchy Management (FlowRow Wrapping Chips) */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-4">
        <div className="flex items-center gap-2 text-xs font-bold text-slate-700 uppercase tracking-wider">
          <Layers size={15} className="text-navy" />
          <span>Dynamic Location Hierarchy</span>
        </div>

        {/* Blocks Chips */}
        <div>
          <span className="text-[11px] font-bold text-slate-500 block mb-1.5">
            1. Blocks / Zones (Wrap smoothly without clipping)
          </span>
          <div className="flex flex-wrap gap-2 mb-2">
            {blocks.map(b => (
              <button
                key={b.id}
                onClick={() => setActiveBlockId(b.id!)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                  activeBlockId === b.id
                    ? 'bg-navy text-white shadow-sm'
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                {b.name}
              </button>
            ))}
          </div>
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="Add Block (e.g. Tower C)"
              value={newBlockName}
              onChange={e => setNewBlockName(e.target.value)}
              className="flex-1 p-2 rounded-xl border border-slate-300 text-xs"
            />
            <button
              onClick={handleAddBlock}
              className="px-3 py-2 bg-navy text-white rounded-xl text-xs font-bold flex items-center gap-1"
            >
              <Plus size={14} /> Add
            </button>
          </div>
        </div>

        {/* Floors Chips */}
        {activeBlockId && (
          <div className="pt-2 border-t border-slate-100">
            <span className="text-[11px] font-bold text-slate-500 block mb-1.5">
              2. Floors / Levels for Selected Block
            </span>
            <div className="flex flex-wrap gap-2 mb-2">
              {floors
                .filter(f => f.blockId === activeBlockId)
                .map(f => (
                  <button
                    key={f.id}
                    onClick={() => setActiveFloorId(f.id!)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                      activeFloorId === f.id
                        ? 'bg-navy text-white shadow-sm'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    {f.name}
                  </button>
                ))}
            </div>
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="Add Floor (e.g. 3rd Floor)"
                value={newFloorName}
                onChange={e => setNewFloorName(e.target.value)}
                className="flex-1 p-2 rounded-xl border border-slate-300 text-xs"
              />
              <button
                onClick={handleAddFloor}
                className="px-3 py-2 bg-navy text-white rounded-xl text-xs font-bold flex items-center gap-1"
              >
                <Plus size={14} /> Add
              </button>
            </div>
          </div>
        )}

        {/* Presets Chips */}
        {activeFloorId && (
          <div className="pt-2 border-t border-slate-100">
            <span className="text-[11px] font-bold text-slate-500 block mb-1.5">
              3. Preset Sub-locations for Selected Floor
            </span>
            <div className="flex flex-wrap gap-2 mb-2">
              {subLocations
                .filter(s => s.floorId === activeFloorId)
                .map(s => (
                  <span
                    key={s.id}
                    className="px-2.5 py-1 bg-amber-50 border border-amber-200 text-navy font-semibold text-xs rounded-md"
                  >
                    {s.name}
                  </span>
                ))}
            </div>
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="Add Sub-location (e.g. Flat 301 - Kitchen)"
                value={newPresetName}
                onChange={e => setNewPresetName(e.target.value)}
                className="flex-1 p-2 rounded-xl border border-slate-300 text-xs"
              />
              <button
                onClick={handleAddPreset}
                className="px-3 py-2 bg-navy text-white rounded-xl text-xs font-bold flex items-center gap-1"
              >
                <Plus size={14} /> Add
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 4. Vendor Directory Management */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <div className="flex items-center gap-2 text-xs font-bold text-slate-700 uppercase tracking-wider">
          <Building size={15} className="text-navy" />
          <span>Vendor Directory</span>
        </div>

        <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
          {vendors.map(v => (
            <div
              key={v.id}
              className="p-2.5 bg-slate-50 rounded-xl border border-slate-100 flex items-center justify-between text-xs"
            >
              <div>
                <span className="font-bold text-slate-800">{v.name}</span>
                <span className="text-slate-500 ml-2">({v.trade})</span>
                <p className="text-[11px] text-slate-400">{v.primaryEmail}</p>
              </div>
            </div>
          ))}
        </div>

        <div className="pt-2 border-t border-slate-100 space-y-2">
          <span className="text-[11px] font-bold text-slate-500 block">Add New Vendor:</span>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
            <input
              type="text"
              placeholder="Vendor Name"
              value={newVendorName}
              onChange={e => setNewVendorName(e.target.value)}
              className="p-2 rounded-xl border border-slate-300 text-xs"
            />
            <input
              type="text"
              placeholder="Trade / Category"
              value={newVendorTrade}
              onChange={e => setNewVendorTrade(e.target.value)}
              className="p-2 rounded-xl border border-slate-300 text-xs"
            />
            <input
              type="email"
              placeholder="Primary Email"
              value={newVendorEmail}
              onChange={e => setNewVendorEmail(e.target.value)}
              className="p-2 rounded-xl border border-slate-300 text-xs"
            />
          </div>
          <button
            onClick={handleAddVendor}
            className="w-full py-2 bg-navy text-white rounded-xl text-xs font-bold flex items-center justify-center gap-1 shadow-sm"
          >
            <Plus size={14} /> Add Vendor to Project
          </button>
        </div>
      </div>

      {/* 5. Project Metadata & Google Sheet Sync Settings */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <div className="flex items-center gap-2 text-xs font-bold text-slate-700 uppercase tracking-wider">
          <Settings size={15} className="text-navy" />
          <span>Project & Sync Configuration</span>
        </div>

        <div className="space-y-3 text-xs">
          <div>
            <label className="block text-slate-600 font-semibold mb-1">
              Client / Main Contractor (on NCR PDF header)
            </label>
            <input
              type="text"
              value={clientName}
              onChange={e => setClientName(e.target.value)}
              className="w-full p-2.5 rounded-xl border border-slate-300"
            />
          </div>

          <div>
            <label className="block text-slate-600 font-semibold mb-1">
              Google Spreadsheet ID
            </label>
            <input
              type="text"
              placeholder="e.g. 1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms"
              value={spreadsheetId}
              onChange={e => setSpreadsheetId(e.target.value)}
              className="w-full p-2.5 rounded-xl border border-slate-300 font-mono text-[11px]"
            />
          </div>

          <div>
            <label className="block text-slate-600 font-semibold mb-1">
              Default CC Emails (comma-separated)
            </label>
            <input
              type="text"
              placeholder="qaqc@client.com, pm@generalcontractor.com"
              value={ccEmails}
              onChange={e => setCcEmails(e.target.value)}
              className="w-full p-2.5 rounded-xl border border-slate-300"
            />
          </div>

          <div>
            <label className="block text-slate-600 font-semibold mb-1">
              Automated Email Summary Frequency
            </label>
            <select
              value={emailFrequency}
              onChange={e => setEmailFrequency(e.target.value)}
              className="w-full p-2.5 rounded-xl border border-slate-300 bg-white"
            >
              <option value="Daily">Daily (Every morning 8:00 AM)</option>
              <option value="Alternate Days">Alternate Days</option>
              <option value="Weekly">Weekly (Every Monday)</option>
            </select>
          </div>
        </div>

        <button
          onClick={handleSaveProjectSettings}
          className="w-full py-3 bg-navy hover:bg-slate-800 text-white rounded-xl font-bold text-xs shadow flex items-center justify-center gap-1.5 transition-colors"
        >
          <Check size={16} /> Save Project Settings
        </button>

        {saveSettingsSuccess && (
          <p className="text-center text-xs font-bold text-emerald-600">
            ✓ Project settings updated successfully.
          </p>
        )}
      </div>
    </div>
  );
};
