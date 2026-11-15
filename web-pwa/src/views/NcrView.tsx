import React, { useState, useEffect } from 'react';
import { Observation, Project } from '../types';
import { db } from '../db/db';
import { generateNcrPdf } from '../utils/pdfGenerator';
import { RectificationModal } from '../components/RectificationModal';
import {
  AlertTriangle,
  Download,
  CheckCircle2,
  MapPin,
  Building,
  Clock,
  ShieldAlert
} from 'lucide-react';

interface NcrViewProps {
  project: Project;
}

export const NcrView: React.FC<NcrViewProps> = ({ project }) => {
  const [ncrs, setNcrs] = useState<Observation[]>([]);
  const [selectedNcrForRectify, setSelectedNcrForRectify] = useState<Observation | null>(null);
  const [activePhotoPreview, setActivePhotoPreview] = useState<string | null>(null);

  const loadNcrs = async () => {
    if (!project.id) return;
    const list = await db.observations
      .where('projectId')
      .equals(project.id)
      .filter(o => o.recordType === 'NCR')
      .reverse()
      .sortBy('dateLogged');
    setNcrs(list);
  };

  useEffect(() => {
    loadNcrs();
  }, [project.id]);

  const handleDownloadPdf = (obs: Observation) => {
    const doc = generateNcrPdf(obs, project);
    doc.save(`SnagOps_NCR_${obs.entryNumber}.pdf`);
  };

  return (
    <div className="max-w-2xl mx-auto p-4 pb-24 space-y-4">
      {/* Top Banner */}
      <div className="bg-red-700 text-white p-4 rounded-2xl shadow-md flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-white/20 flex items-center justify-center font-bold">
            <ShieldAlert size={20} />
          </div>
          <div>
            <h2 className="font-bold text-sm">Non-Conformance Register (NCR)</h2>
            <p className="text-[11px] text-red-100">
              {ncrs.filter(n => n.status === 'OPEN').length} Open NCRs requiring immediate CAPA
            </p>
          </div>
        </div>
        <span className="text-xs font-mono font-bold bg-white text-red-700 px-2.5 py-1 rounded-full shadow-sm">
          {ncrs.length} Total
        </span>
      </div>

      {/* NCR Cards List */}
      <div className="space-y-4">
        {ncrs.length === 0 ? (
          <div className="bg-white p-8 rounded-2xl border border-slate-200 text-center text-slate-500 text-xs">
            No Non-Conformance Reports logged for this project yet.
          </div>
        ) : (
          ncrs.map(ncr => {
            return (
              <div
                key={ncr.id}
                className="bg-white rounded-2xl border-2 border-red-200 shadow-sm overflow-hidden"
              >
                {/* Header Banner */}
                <div className="bg-red-50/80 px-4 py-3 border-b border-red-100 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-xs bg-red-600 text-white px-2 py-0.5 rounded-md">
                      {ncr.entryNumber}
                    </span>
                    <span className="text-[10px] text-slate-500 font-mono">
                      {ncr.recordUuid.slice(0, 8)}
                    </span>
                  </div>

                  <div className="flex items-center gap-1.5">
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-red-100 text-red-700">
                      PRIORITY: {ncr.priority}
                    </span>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 ${
                        ncr.status === 'RECTIFIED'
                          ? 'bg-emerald-600 text-white'
                          : 'bg-red-600 text-white'
                      }`}
                    >
                      {ncr.status === 'RECTIFIED' ? <CheckCircle2 size={11} /> : <AlertTriangle size={11} />}
                      {ncr.status}
                    </span>
                  </div>
                </div>

                <div className="p-4 space-y-3">
                  {/* Location & Vendor */}
                  <div className="space-y-1 text-xs">
                    <div className="flex items-center gap-1.5 text-slate-600 font-medium">
                      <MapPin size={13} className="text-red-600 shrink-0" />
                      <span>
                        {ncr.block} &gt; {ncr.floor} &gt; <strong className="text-slate-800">{ncr.specificLocation}</strong>
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5 text-slate-600">
                      <Building size={13} className="text-slate-400 shrink-0" />
                      <span>
                        Responsible: <strong className="text-slate-800">{ncr.vendorName}</strong> ({ncr.trade})
                      </span>
                    </div>
                  </div>

                  {/* Defect Description */}
                  <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                    <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">
                      1. Defect Description
                    </span>
                    <p className="text-xs text-slate-800 font-medium leading-relaxed">
                      {ncr.issueDescription}
                    </p>
                  </div>

                  {/* Root Cause & Corrective Action */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                    <div className="bg-amber-50/70 p-3 rounded-xl border border-amber-200">
                      <span className="text-[10px] font-bold text-amber-800 uppercase tracking-wider block mb-0.5">
                        2. Root Cause Analysis
                      </span>
                      <p className="text-[11px] text-slate-700">
                        {ncr.rootCause || 'Under investigation by contractor.'}
                      </p>
                    </div>

                    <div className="bg-blue-50/70 p-3 rounded-xl border border-blue-200">
                      <span className="text-[10px] font-bold text-blue-800 uppercase tracking-wider block mb-0.5">
                        3. Corrective Action Required
                      </span>
                      <p className="text-[11px] text-slate-700">
                        {ncr.correctiveAction || 'Rectify per technical specs.'}
                      </p>
                    </div>
                  </div>

                  {/* Photographic Evidence */}
                  {(ncr.beforePhotoData || ncr.afterPhotoData) && (
                    <div className="flex gap-2 pt-1">
                      {ncr.beforePhotoData && (
                        <div className="relative">
                          <img
                            src={ncr.beforePhotoData}
                            alt="NCR defect photo"
                            onClick={() => setActivePhotoPreview(ncr.beforePhotoData!)}
                            className="w-20 h-20 object-cover rounded-xl border border-red-200 cursor-pointer shadow-sm hover:opacity-90"
                          />
                          <span className="absolute bottom-1 left-1 bg-red-600 text-white text-[8px] font-bold px-1 rounded">
                            Defect
                          </span>
                        </div>
                      )}
                      {ncr.afterPhotoData && (
                        <div className="relative">
                          <img
                            src={ncr.afterPhotoData}
                            alt="NCR rectification photo"
                            onClick={() => setActivePhotoPreview(ncr.afterPhotoData!)}
                            className="w-20 h-20 object-cover rounded-xl border border-emerald-300 cursor-pointer shadow-sm hover:opacity-90"
                          />
                          <span className="absolute bottom-1 left-1 bg-emerald-600 text-white text-[8px] font-bold px-1 rounded">
                            Closure
                          </span>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Sign-off & Actions Bar */}
                  <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2">
                    <div className="text-[11px] text-slate-500">
                      <span>Issued By: <strong>{ncr.loggedBy} ({ncr.loggedByInitials})</strong></span>
                      <span className="mx-1">•</span>
                      <span>Target: <strong>{new Date(ncr.targetDate).toLocaleDateString('en-GB')}</strong></span>
                    </div>

                    <div className="flex gap-2">
                      <button
                        onClick={() => handleDownloadPdf(ncr)}
                        className="py-1.5 px-3 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-xl shadow-sm flex items-center gap-1.5 transition-colors"
                      >
                        <Download size={13} />
                        <span>A4 PDF</span>
                      </button>

                      {ncr.status === 'OPEN' && (
                        <button
                          onClick={() => setSelectedNcrForRectify(ncr)}
                          className="py-1.5 px-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-sm flex items-center gap-1.5 transition-colors"
                        >
                          <CheckCircle2 size={13} />
                          <span>Rectify</span>
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Photo Preview Modal */}
      {activePhotoPreview && (
        <div
          className="fixed inset-0 z-50 bg-black/90 flex items-center justify-center p-4"
          onClick={() => setActivePhotoPreview(null)}
        >
          <img
            src={activePhotoPreview}
            alt="Full size preview"
            className="max-w-full max-h-[85vh] rounded-2xl object-contain"
          />
        </div>
      )}

      {/* Rectification Modal */}
      <RectificationModal
        isOpen={!!selectedNcrForRectify}
        observation={selectedNcrForRectify}
        onClose={() => setSelectedNcrForRectify(null)}
        onRectified={() => {
          loadNcrs();
        }}
      />
    </div>
  );
};
