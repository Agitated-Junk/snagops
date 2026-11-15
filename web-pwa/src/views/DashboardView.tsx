import React, { useState, useEffect } from 'react';
import { Project, Observation, CustomFieldDefinition } from '../types';
import { db } from '../db/db';
import { exportWorkbook } from '../utils/excelExporter';
import {
  FileSpreadsheet,
  RefreshCw,
  TrendingUp,
  AlertTriangle,
  CheckCircle2,
  ListTodo,
  Layers
} from 'lucide-react';

interface DashboardViewProps {
  project: Project;
  onSync: () => void;
  isSyncing: boolean;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  project,
  onSync,
  isSyncing
}) => {
  const [observations, setObservations] = useState<Observation[]>([]);
  const [customFields, setCustomFields] = useState<CustomFieldDefinition[]>([]);
  const [isExporting, setIsExporting] = useState(false);
  const [exportSuccess, setExportSuccess] = useState(false);

  useEffect(() => {
    async function loadData() {
      if (!project.id) return;
      const obs = await db.observations.where('projectId').equals(project.id).toArray();
      const cfs = await db.customFields.where('projectId').equals(project.id).toArray();
      setObservations(obs);
      setCustomFields(cfs);
    }
    loadData();
  }, [project.id]);

  const total = observations.length;
  const open = observations.filter(o => o.status === 'OPEN').length;
  const rectified = observations.filter(o => o.status === 'RECTIFIED').length;
  const closureRate = total > 0 ? ((rectified / total) * 100).toFixed(1) : '0.0';

  const highPriority = observations.filter(o => o.priority === 'HIGH').length;
  const medPriority = observations.filter(o => o.priority === 'MEDIUM').length;
  const lowPriority = observations.filter(o => o.priority === 'LOW').length;

  const normalCount = observations.filter(o => o.recordType === 'NORMAL_OBSERVATION').length;
  const ncrCount = observations.filter(o => o.recordType === 'NCR').length;

  const handleExport = async () => {
    try {
      setIsExporting(true);
      await exportWorkbook(project, observations, customFields);
      setExportSuccess(true);
      setTimeout(() => setExportSuccess(false), 3000);
    } catch (err) {
      alert('Failed to export workbook: ' + err);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto p-4 pb-24 space-y-4">
      {/* Project Overview Header */}
      <div className="bg-navy text-white p-5 rounded-2xl shadow-md space-y-2">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-amber-300 uppercase tracking-wider">
            Quality Management Dashboard
          </span>
          <span className="text-xs font-mono font-bold bg-white/10 px-2 py-0.5 rounded">
            {project.projectCode}
          </span>
        </div>
        <h2 className="text-xl font-bold leading-tight">{project.projectName}</h2>
        <p className="text-xs text-slate-300">
          Client / Contractor: {project.clientContractorName}
        </p>
      </div>

      {/* KPI 4-Card Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm text-center">
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider block">
            Total Logged
          </span>
          <span className="text-2xl font-black text-navy mt-1 block">{total}</span>
          <span className="text-[10px] text-slate-400">Site Snags & NCRs</span>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm text-center">
          <span className="text-[11px] font-bold text-red-600 uppercase tracking-wider block">
            Open Issues
          </span>
          <span className="text-2xl font-black text-red-600 mt-1 block">{open}</span>
          <span className="text-[10px] text-slate-400">Pending Action</span>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm text-center">
          <span className="text-[11px] font-bold text-emerald-600 uppercase tracking-wider block">
            Rectified
          </span>
          <span className="text-2xl font-black text-emerald-600 mt-1 block">{rectified}</span>
          <span className="text-[10px] text-slate-400">Closed Out</span>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm text-center">
          <span className="text-[11px] font-bold text-amber-600 uppercase tracking-wider block">
            Closure Rate
          </span>
          <span className="text-2xl font-black text-amber-600 mt-1 block">{closureRate}%</span>
          <span className="text-[10px] text-slate-400">Target: &gt;90%</span>
        </div>
      </div>

      {/* Breakdown Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {/* Record Type Breakdown */}
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-700 uppercase tracking-wider">
            <Layers size={14} className="text-navy" />
            <span>Record Type Distribution</span>
          </div>

          <div className="space-y-2 text-xs">
            <div className="flex items-center justify-between p-2 bg-slate-50 rounded-xl">
              <span className="font-semibold text-slate-700">Normal Observations</span>
              <span className="font-bold text-navy">{normalCount}</span>
            </div>
            <div className="flex items-center justify-between p-2 bg-red-50 rounded-xl">
              <span className="font-semibold text-red-700">Non-Conformance Reports (NCR)</span>
              <span className="font-bold text-red-700">{ncrCount}</span>
            </div>
          </div>
        </div>

        {/* Priority Breakdown */}
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-700 uppercase tracking-wider">
            <TrendingUp size={14} className="text-navy" />
            <span>Priority Breakdown</span>
          </div>

          <div className="space-y-2 text-xs">
            <div className="flex items-center justify-between p-2 bg-red-50 rounded-xl">
              <span className="font-semibold text-red-700">High Priority</span>
              <span className="font-bold text-red-700">{highPriority}</span>
            </div>
            <div className="flex items-center justify-between p-2 bg-amber-50 rounded-xl">
              <span className="font-semibold text-amber-800">Medium Priority</span>
              <span className="font-bold text-amber-800">{medPriority}</span>
            </div>
            <div className="flex items-center justify-between p-2 bg-emerald-50 rounded-xl">
              <span className="font-semibold text-emerald-800">Low Priority</span>
              <span className="font-bold text-emerald-800">{lowPriority}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Action Buttons: Excel Export & Cloud Sync */}
      <div className="space-y-2.5 pt-2">
        <button
          onClick={handleExport}
          disabled={isExporting}
          className="w-full py-3.5 bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-xs rounded-2xl shadow flex items-center justify-center gap-2 transition-colors disabled:opacity-50"
        >
          <FileSpreadsheet size={17} />
          <span>
            {isExporting ? 'Generating 3-Tab Excel Workbook...' : 'Export Quality Workbook (.xlsx)'}
          </span>
        </button>

        {exportSuccess && (
          <div className="text-center text-xs font-bold text-emerald-700 animate-fadeIn">
            ✓ Excel workbook downloaded (Management Dashboard, Observations & NCR Register)
          </div>
        )}

        <button
          onClick={onSync}
          disabled={isSyncing}
          className="w-full py-3.5 bg-navy hover:bg-slate-800 text-white font-bold text-xs rounded-2xl shadow flex items-center justify-center gap-2 transition-colors disabled:opacity-50"
        >
          <RefreshCw size={17} className={isSyncing ? 'animate-spin text-amber-300' : ''} />
          <span>{isSyncing ? 'Syncing with Master Google Sheet...' : 'Sync with Master Google Sheet'}</span>
        </button>
      </div>
    </div>
  );
};
