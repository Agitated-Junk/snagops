import React, { useState } from 'react';
import { Project, EngineerProfile } from '../types';
import { HardHat, ChevronDown, RefreshCw, Plus, Check } from 'lucide-react';
import { db } from '../db/db';

interface TopAppBarProps {
  project: Project | null;
  projects: Project[];
  profile: EngineerProfile;
  onSelectProject: (project: Project) => void;
  onOpenProfile: () => void;
  onOpenProjectConfig: (mode: 'SHARE' | 'JOIN') => void;
  onSync: () => void;
  isSyncing: boolean;
}

export const TopAppBar: React.FC<TopAppBarProps> = ({
  project,
  projects,
  profile,
  onSelectProject,
  onOpenProfile,
  onOpenProjectConfig,
  onSync,
  isSyncing
}) => {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [newProjectModal, setNewProjectModal] = useState(false);
  const [newProjName, setNewProjName] = useState('');
  const [newProjCode, setNewProjCode] = useState('');
  const [newProjClient, setNewProjClient] = useState('');

  const handleCreateProject = async () => {
    if (!newProjName.trim() || !newProjCode.trim()) return;

    const id = await db.projects.add({
      recordUuid: `proj-${Date.now()}`,
      projectName: newProjName.trim(),
      projectCode: newProjCode.trim().toUpperCase(),
      clientContractorName: newProjClient.trim() || 'General Contractor',
      spreadsheetId: '',
      defaultCcEmails: '',
      emailFrequency: 'Daily',
      isActive: true,
      updatedAt: Date.now()
    });

    // Deactivate others
    await db.projects.where('id').notEqual(id).modify({ isActive: false });
    const created = await db.projects.get(id);
    if (created) {
      onSelectProject(created);
    }
    setNewProjectModal(false);
    setNewProjName('');
    setNewProjCode('');
    setNewProjClient('');
  };

  return (
    <header className="sticky top-0 z-40 bg-navy text-white shadow-md border-b border-navy-light">
      <div className="max-w-4xl mx-auto px-4 py-2.5 flex items-center justify-between gap-2">
        {/* Brand & Project Selector */}
        <div className="flex items-center gap-2 min-w-0">
          <div className="w-8 h-8 rounded-lg bg-amber-400 text-navy flex items-center justify-center font-black text-sm shrink-0 shadow">
            <HardHat size={18} className="text-navy" />
          </div>

          {/* Project Switcher Pill */}
          <div className="relative min-w-0">
            <button
              onClick={() => setDropdownOpen(!dropdownOpen)}
              className="flex items-center gap-1.5 bg-white/10 hover:bg-white/15 px-3 py-1.5 rounded-full text-left transition-colors max-w-[200px] sm:max-w-xs"
            >
              <div className="truncate text-xs">
                <span className="font-bold block truncate text-slate-100">
                  {project ? project.projectName : 'Select Project'}
                </span>
                <span className="text-[10px] text-amber-300 font-mono block">
                  {project ? project.projectCode : 'No Active Project'}
                </span>
              </div>
              <ChevronDown size={14} className="shrink-0 text-slate-300" />
            </button>

            {/* Dropdown Menu */}
            {dropdownOpen && (
              <>
                <div
                  className="fixed inset-0 z-40"
                  onClick={() => setDropdownOpen(false)}
                />
                <div className="absolute left-0 mt-2 w-72 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 text-slate-900 overflow-hidden py-1">
                  <div className="px-3 py-2 bg-slate-50 border-b border-slate-100 flex items-center justify-between">
                    <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                      Switch Project
                    </span>
                    <button
                      onClick={() => {
                        setDropdownOpen(false);
                        setNewProjectModal(true);
                      }}
                      className="text-xs text-navy font-bold flex items-center gap-1 hover:underline"
                    >
                      <Plus size={12} /> New
                    </button>
                  </div>

                  <div className="max-h-60 overflow-y-auto py-1">
                    {projects.map(p => (
                      <button
                        key={p.id}
                        onClick={() => {
                          onSelectProject(p);
                          setDropdownOpen(false);
                        }}
                        className={`w-full px-3 py-2 text-left text-xs flex items-center justify-between hover:bg-slate-50 transition-colors ${
                          project?.id === p.id ? 'bg-amber-50 font-bold text-navy' : 'text-slate-700'
                        }`}
                      >
                        <div className="truncate mr-2">
                          <p className="truncate font-semibold">{p.projectName}</p>
                          <p className="text-[10px] text-slate-500 font-mono">{p.projectCode}</p>
                        </div>
                        {project?.id === p.id && <Check size={14} className="text-navy shrink-0" />}
                      </button>
                    ))}
                  </div>

                  <div className="border-t border-slate-100 px-2 py-1.5 bg-slate-50 flex gap-1">
                    <button
                      onClick={() => {
                        setDropdownOpen(false);
                        onOpenProjectConfig('SHARE');
                      }}
                      className="flex-1 py-1 px-2 text-[11px] text-navy font-semibold hover:bg-slate-200 rounded text-center"
                    >
                      Share Config
                    </button>
                    <button
                      onClick={() => {
                        setDropdownOpen(false);
                        onOpenProjectConfig('JOIN');
                      }}
                      className="flex-1 py-1 px-2 text-[11px] text-navy font-semibold hover:bg-slate-200 rounded text-center"
                    >
                      Join via Code
                    </button>
                  </div>
                </div>
              </>
            )}
          </div>
        </div>

        {/* Right Actions: Sync Button & Engineer Initials Avatar */}
        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={onSync}
            disabled={isSyncing}
            title="Sync with Google Sheets"
            className="p-2 rounded-lg bg-white/10 hover:bg-white/20 transition-colors text-white disabled:opacity-50"
          >
            <RefreshCw size={17} className={isSyncing ? 'animate-spin text-amber-300' : ''} />
          </button>

          {/* Engineer Initials Avatar Badge */}
          <button
            onClick={onOpenProfile}
            title={`Engineer: ${profile.fullName || 'Tap to configure profile'}`}
            className="flex items-center gap-1.5 pl-1.5 pr-2.5 py-1 rounded-full bg-amber-400 hover:bg-amber-300 transition-colors text-navy shadow-sm"
          >
            <span className="w-6 h-6 rounded-full bg-navy text-amber-400 font-black text-[11px] font-mono flex items-center justify-center">
              {profile.initials || '??'}
            </span>
            <span className="text-xs font-bold font-mono tracking-wider">
              {profile.initials || 'SETUP'}
            </span>
          </button>
        </div>
      </div>

      {/* New Project Dialog */}
      {newProjectModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="bg-white rounded-2xl p-5 max-w-sm w-full text-slate-900 shadow-2xl">
            <h3 className="font-bold text-base mb-3 text-navy">Create New Project</h3>
            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold mb-1">Project Name *</label>
                <input
                  type="text"
                  placeholder="e.g. Apex High-Rise Tower"
                  value={newProjName}
                  onChange={e => setNewProjName(e.target.value)}
                  className="w-full p-2.5 rounded-lg border border-slate-300"
                />
              </div>
              <div>
                <label className="block font-semibold mb-1">Project Code *</label>
                <input
                  type="text"
                  placeholder="e.g. AHR-2026"
                  value={newProjCode}
                  onChange={e => setNewProjCode(e.target.value)}
                  className="w-full p-2.5 rounded-lg border border-slate-300 uppercase font-mono"
                />
              </div>
              <div>
                <label className="block font-semibold mb-1">Client / Contractor</label>
                <input
                  type="text"
                  placeholder="e.g. Skyline Infrastructure"
                  value={newProjClient}
                  onChange={e => setNewProjClient(e.target.value)}
                  className="w-full p-2.5 rounded-lg border border-slate-300"
                />
              </div>
            </div>
            <div className="flex gap-2 mt-4">
              <button
                onClick={() => setNewProjectModal(false)}
                className="flex-1 py-2 text-slate-600 bg-slate-100 rounded-lg font-bold text-xs"
              >
                Cancel
              </button>
              <button
                onClick={handleCreateProject}
                className="flex-1 py-2 bg-navy text-white rounded-lg font-bold text-xs"
              >
                Create
              </button>
            </div>
          </div>
        </div>
      )}
    </header>
  );
};
