import React, { useState } from 'react';
import { Project } from '../types';
import { Copy, Check, Share2, Download, AlertCircle, X } from 'lucide-react';
import { db } from '../db/db';

interface ProjectConfigModalProps {
  isOpen: boolean;
  mode: 'SHARE' | 'JOIN';
  project?: Project;
  onClose: () => void;
  onProjectJoined: (newProject: Project) => void;
}

export const ProjectConfigModal: React.FC<ProjectConfigModalProps> = ({
  isOpen,
  mode,
  project,
  onClose,
  onProjectJoined
}) => {
  const [copied, setCopied] = useState(false);
  const [joinJson, setJoinJson] = useState('');
  const [joinError, setJoinError] = useState('');

  if (!isOpen) return null;

  const shareSnippet = project
    ? JSON.stringify(
        {
          appName: 'SnagOps',
          version: '2.0',
          recordUuid: project.recordUuid,
          projectName: project.projectName,
          projectCode: project.projectCode,
          clientContractorName: project.clientContractorName,
          spreadsheetId: project.spreadsheetId,
          defaultCcEmails: project.defaultCcEmails,
          emailFrequency: project.emailFrequency
        },
        null,
        2
      )
    : '';

  const handleCopy = () => {
    navigator.clipboard.writeText(shareSnippet);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  const handleJoin = async () => {
    setJoinError('');
    try {
      const parsed = JSON.parse(joinJson.trim());
      if (!parsed.projectName || !parsed.projectCode) {
        setJoinError('Invalid config: missing Project Name or Project Code.');
        return;
      }

      // Check if project already exists locally
      const existing = await db.projects
        .where('projectCode')
        .equals(parsed.projectCode)
        .first();

      let projectId: number;
      if (existing) {
        projectId = existing.id!;
        await db.projects.update(projectId, {
          projectName: parsed.projectName,
          clientContractorName: parsed.clientContractorName || '',
          spreadsheetId: parsed.spreadsheetId || '',
          defaultCcEmails: parsed.defaultCcEmails || '',
          emailFrequency: parsed.emailFrequency || 'Daily',
          isActive: true
        });
      } else {
        projectId = await db.projects.add({
          recordUuid: parsed.recordUuid || `proj-${Date.now()}`,
          projectName: parsed.projectName,
          projectCode: parsed.projectCode,
          clientContractorName: parsed.clientContractorName || '',
          spreadsheetId: parsed.spreadsheetId || '',
          defaultCcEmails: parsed.defaultCcEmails || '',
          emailFrequency: parsed.emailFrequency || 'Daily',
          isActive: true,
          updatedAt: Date.now()
        });
      }

      // Deactivate all others
      await db.projects.where('id').notEqual(projectId).modify({ isActive: false });

      const loaded = await db.projects.get(projectId);
      if (loaded) {
        onProjectJoined(loaded);
        onClose();
      }
    } catch {
      setJoinError('Invalid JSON format. Please paste the exact JSON snippet shared by your team.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-lg w-full overflow-hidden border border-slate-200">
        <div className="bg-navy p-4 px-6 text-white flex items-center justify-between">
          <div className="flex items-center gap-2">
            {mode === 'SHARE' ? <Share2 size={20} /> : <Download size={20} />}
            <h3 className="font-bold text-base">
              {mode === 'SHARE' ? 'Share Project Config' : 'Join Project via Config'}
            </h3>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-300 hover:text-white">
            <X size={20} />
          </button>
        </div>

        <div className="p-6 space-y-4">
          {mode === 'SHARE' ? (
            <>
              <p className="text-xs text-slate-600">
                Share this configuration snippet with your on-site engineering team. Importing it configures their app with the exact same Project Code, Client Name, and Master Ledger ID instantly.
              </p>
              <div className="relative">
                <pre className="bg-slate-900 text-emerald-400 p-4 rounded-xl text-xs font-mono overflow-x-auto max-h-56 leading-relaxed">
                  {shareSnippet}
                </pre>
                <button
                  onClick={handleCopy}
                  className="absolute top-2 right-2 px-3 py-1.5 bg-white/10 hover:bg-white/20 text-white rounded-lg text-xs font-medium flex items-center gap-1.5 transition-colors"
                >
                  {copied ? <Check size={14} className="text-emerald-400" /> : <Copy size={14} />}
                  {copied ? 'Copied!' : 'Copy'}
                </button>
              </div>
              <button
                onClick={onClose}
                className="w-full py-3 bg-slate-100 hover:bg-slate-200 text-slate-800 font-bold text-sm rounded-xl"
              >
                Done
              </button>
            </>
          ) : (
            <>
              <p className="text-xs text-slate-600">
                Paste the project JSON configuration snippet provided by your QA/QC team lead or Project Manager.
              </p>
              <textarea
                rows={7}
                placeholder="Paste { ... } config snippet here"
                value={joinJson}
                onChange={e => setJoinJson(e.target.value)}
                className="w-full p-3 font-mono text-xs rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-navy"
              />
              {joinError && (
                <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg flex items-center gap-2">
                  <AlertCircle size={14} className="shrink-0" />
                  <span>{joinError}</span>
                </div>
              )}
              <div className="flex gap-2">
                <button
                  onClick={onClose}
                  className="flex-1 py-3 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-sm rounded-xl"
                >
                  Cancel
                </button>
                <button
                  onClick={handleJoin}
                  className="flex-1 py-3 bg-navy hover:bg-slate-800 text-white font-bold text-sm rounded-xl shadow"
                >
                  Import & Join
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
