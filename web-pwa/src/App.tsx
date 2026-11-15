import React, { useState, useEffect } from 'react';
import { Project, EngineerProfile } from './types';
import { db, seedInitialDataIfNeeded } from './db/db';
import { getEngineerProfile } from './utils/profileStorage';
import { performTwoWaySync } from './utils/googleSync';
import { TopAppBar } from './components/TopAppBar';
import { ProfileModal } from './components/ProfileModal';
import { ProjectConfigModal } from './components/ProjectConfigModal';
import { CaptureView } from './views/CaptureView';
import { ObservationsView } from './views/ObservationsView';
import { NcrView } from './views/NcrView';
import { DashboardView } from './views/DashboardView';
import { SettingsView } from './views/SettingsView';
import {
  Camera,
  CheckSquare,
  AlertTriangle,
  TrendingUp,
  Settings
} from 'lucide-react';

type Tab = 'CAPTURE' | 'OBSERVATIONS' | 'NCR' | 'DASHBOARD' | 'SETTINGS';

export const App: React.FC = () => {
  const [profile, setProfile] = useState<EngineerProfile>(getEngineerProfile());
  const [showProfileModal, setShowProfileModal] = useState<boolean>(!profile.isConfigured);
  const [isFirstLaunch, setIsFirstLaunch] = useState<boolean>(!profile.isConfigured);

  const [projects, setProjects] = useState<Project[]>([]);
  const [activeProject, setActiveProject] = useState<Project | null>(null);

  const [activeTab, setActiveTab] = useState<Tab>('CAPTURE');
  const [openNcrCount, setOpenNcrCount] = useState<number>(0);

  // Config share/join modal
  const [configModalMode, setConfigModalMode] = useState<'SHARE' | 'JOIN' | null>(null);

  // Sync state & toast
  const [isSyncing, setIsSyncing] = useState<boolean>(false);
  const [syncToast, setSyncToast] = useState<string | null>(null);

  const refreshProjects = async () => {
    await seedInitialDataIfNeeded();
    const pList = await db.projects.toArray();
    setProjects(pList);
    const active = pList.find(p => p.isActive) || pList[0] || null;
    setActiveProject(active);
  };

  const refreshOpenNcrCount = async () => {
    if (!activeProject?.id) return;
    const count = await db.observations
      .where('projectId')
      .equals(activeProject.id)
      .filter(o => o.recordType === 'NCR' && o.status === 'OPEN')
      .count();
    setOpenNcrCount(count);
  };

  useEffect(() => {
    refreshProjects();
  }, []);

  useEffect(() => {
    refreshOpenNcrCount();
  }, [activeProject?.id, activeTab]);

  const handleSelectProject = async (p: Project) => {
    if (!p.id) return;
    await db.projects.where('id').notEqual(p.id).modify({ isActive: false });
    await db.projects.update(p.id, { isActive: true });
    setActiveProject({ ...p, isActive: true });
    refreshProjects();
  };

  const handleSync = async () => {
    if (!activeProject) return;
    setIsSyncing(true);
    setSyncToast('Syncing with master Google Sheet...');
    const res = await performTwoWaySync(activeProject);
    setIsSyncing(false);
    setSyncToast(res.message);
    setTimeout(() => setSyncToast(null), 3500);
    refreshOpenNcrCount();
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col text-slate-900 select-none">
      {/* Top App Bar */}
      <TopAppBar
        project={activeProject}
        projects={projects}
        profile={profile}
        onSelectProject={handleSelectProject}
        onOpenProfile={() => {
          setIsFirstLaunch(false);
          setShowProfileModal(true);
        }}
        onOpenProjectConfig={mode => setConfigModalMode(mode)}
        onSync={handleSync}
        isSyncing={isSyncing}
      />

      {/* Sync Toast Notification */}
      {syncToast && (
        <div className="fixed top-14 left-1/2 -translate-x-1/2 z-50 bg-navy text-white text-xs font-semibold px-4 py-2 rounded-full shadow-lg border border-slate-700 animate-fadeIn flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
          <span>{syncToast}</span>
        </div>
      )}

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto">
        {activeProject ? (
          <>
            {activeTab === 'CAPTURE' && (
              <CaptureView
                project={activeProject}
                profile={profile}
                onNavigateToNcr={() => setActiveTab('NCR')}
              />
            )}
            {activeTab === 'OBSERVATIONS' && <ObservationsView project={activeProject} />}
            {activeTab === 'NCR' && <NcrView project={activeProject} />}
            {activeTab === 'DASHBOARD' && (
              <DashboardView
                project={activeProject}
                onSync={handleSync}
                isSyncing={isSyncing}
              />
            )}
            {activeTab === 'SETTINGS' && (
              <SettingsView
                project={activeProject}
                profile={profile}
                onOpenProfile={() => {
                  setIsFirstLaunch(false);
                  setShowProfileModal(true);
                }}
                onOpenProjectConfig={mode => setConfigModalMode(mode)}
                onProjectUpdated={p => setActiveProject(p)}
              />
            )}
          </>
        ) : (
          <div className="p-8 text-center text-slate-500 text-xs">
            Loading SnagOps Projects...
          </div>
        )}
      </main>

      {/* Bottom Navigation Bar */}
      <nav className="fixed bottom-0 left-0 right-0 z-40 bg-white border-t border-slate-200 shadow-lg">
        <div className="max-w-md mx-auto grid grid-cols-5 h-16">
          {/* Capture Tab */}
          <button
            onClick={() => setActiveTab('CAPTURE')}
            className={`flex flex-col items-center justify-center transition-colors relative ${
              activeTab === 'CAPTURE' ? 'text-navy font-bold' : 'text-slate-400 hover:text-slate-600'
            }`}
          >
            <Camera size={20} className={activeTab === 'CAPTURE' ? 'stroke-[2.5]' : ''} />
            <span className="text-[10px] mt-1 font-medium">Capture</span>
            {activeTab === 'CAPTURE' && (
              <span className="absolute top-0 w-8 h-1 bg-navy rounded-b-full" />
            )}
          </button>

          {/* Observations Tab */}
          <button
            onClick={() => setActiveTab('OBSERVATIONS')}
            className={`flex flex-col items-center justify-center transition-colors relative ${
              activeTab === 'OBSERVATIONS' ? 'text-navy font-bold' : 'text-slate-400 hover:text-slate-600'
            }`}
          >
            <CheckSquare size={20} className={activeTab === 'OBSERVATIONS' ? 'stroke-[2.5]' : ''} />
            <span className="text-[10px] mt-1 font-medium">Snags</span>
            {activeTab === 'OBSERVATIONS' && (
              <span className="absolute top-0 w-8 h-1 bg-navy rounded-b-full" />
            )}
          </button>

          {/* NCRs Tab */}
          <button
            onClick={() => setActiveTab('NCR')}
            className={`flex flex-col items-center justify-center transition-colors relative ${
              activeTab === 'NCR' ? 'text-red-600 font-bold' : 'text-slate-400 hover:text-slate-600'
            }`}
          >
            <div className="relative">
              <AlertTriangle size={20} className={activeTab === 'NCR' ? 'stroke-[2.5]' : ''} />
              {openNcrCount > 0 && (
                <span className="absolute -top-1 -right-2 bg-red-600 text-white text-[9px] font-bold w-4 h-4 rounded-full flex items-center justify-center shadow">
                  {openNcrCount}
                </span>
              )}
            </div>
            <span className="text-[10px] mt-1 font-medium">NCR</span>
            {activeTab === 'NCR' && (
              <span className="absolute top-0 w-8 h-1 bg-red-600 rounded-b-full" />
            )}
          </button>

          {/* Dashboard Tab */}
          <button
            onClick={() => setActiveTab('DASHBOARD')}
            className={`flex flex-col items-center justify-center transition-colors relative ${
              activeTab === 'DASHBOARD' ? 'text-navy font-bold' : 'text-slate-400 hover:text-slate-600'
            }`}
          >
            <TrendingUp size={20} className={activeTab === 'DASHBOARD' ? 'stroke-[2.5]' : ''} />
            <span className="text-[10px] mt-1 font-medium">Dashboard</span>
            {activeTab === 'DASHBOARD' && (
              <span className="absolute top-0 w-8 h-1 bg-navy rounded-b-full" />
            )}
          </button>

          {/* Settings Tab */}
          <button
            onClick={() => setActiveTab('SETTINGS')}
            className={`flex flex-col items-center justify-center transition-colors relative ${
              activeTab === 'SETTINGS' ? 'text-navy font-bold' : 'text-slate-400 hover:text-slate-600'
            }`}
          >
            <Settings size={20} className={activeTab === 'SETTINGS' ? 'stroke-[2.5]' : ''} />
            <span className="text-[10px] mt-1 font-medium">Settings</span>
            {activeTab === 'SETTINGS' && (
              <span className="absolute top-0 w-8 h-1 bg-navy rounded-b-full" />
            )}
          </button>
        </div>
      </nav>

      {/* Engineer Profile Setup Modal */}
      <ProfileModal
        isOpen={showProfileModal}
        profile={profile}
        isFirstLaunch={isFirstLaunch}
        onSave={newProf => {
          setProfile(newProf);
          setShowProfileModal(false);
        }}
      />

      {/* Project Config Modal (Share / Join) */}
      <ProjectConfigModal
        isOpen={configModalMode !== null}
        mode={configModalMode || 'SHARE'}
        project={activeProject || undefined}
        onClose={() => setConfigModalMode(null)}
        onProjectJoined={p => {
          setActiveProject(p);
          refreshProjects();
        }}
      />
    </div>
  );
};
