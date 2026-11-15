import React, { useState, useEffect } from 'react';
import { Observation, Project, Status } from '../types';
import { db } from '../db/db';
import { Search, CheckCircle2, Clock, MapPin, Building, AlertCircle } from 'lucide-react';
import { RectificationModal } from '../components/RectificationModal';

interface ObservationsViewProps {
  project: Project;
}

export const ObservationsView: React.FC<ObservationsViewProps> = ({ project }) => {
  const [observations, setObservations] = useState<Observation[]>([]);
  const [filterStatus, setFilterStatus] = useState<'ALL' | Status>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedObsForRectify, setSelectedObsForRectify] = useState<Observation | null>(null);
  const [activePhotoPreview, setActivePhotoPreview] = useState<string | null>(null);

  const loadObservations = async () => {
    if (!project.id) return;
    const list = await db.observations
      .where('projectId')
      .equals(project.id)
      .filter(o => o.recordType === 'NORMAL_OBSERVATION')
      .reverse()
      .sortBy('dateLogged');
    setObservations(list);
  };

  useEffect(() => {
    loadObservations();
  }, [project.id]);

  const filtered = observations.filter(o => {
    const matchesStatus = filterStatus === 'ALL' || o.status === filterStatus;
    const q = searchQuery.toLowerCase().trim();
    const matchesSearch =
      !q ||
      o.entryNumber.toLowerCase().includes(q) ||
      o.issueDescription.toLowerCase().includes(q) ||
      o.vendorName.toLowerCase().includes(q) ||
      o.block.toLowerCase().includes(q) ||
      o.floor.toLowerCase().includes(q) ||
      o.specificLocation.toLowerCase().includes(q);
    return matchesStatus && matchesSearch;
  });

  return (
    <div className="max-w-2xl mx-auto p-4 pb-24 space-y-4">
      {/* Top Search & Filter */}
      <div className="bg-white p-3 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <div className="relative">
          <Search size={16} className="absolute left-3 top-3 text-slate-400" />
          <input
            type="text"
            placeholder="Search snags by ID, description, vendor, location..."
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-slate-200 text-xs focus:ring-2 focus:ring-navy focus:outline-none bg-slate-50"
          />
        </div>

        {/* Status Filter Chips */}
        <div className="flex gap-2">
          {(['ALL', 'OPEN', 'RECTIFIED'] as const).map(st => (
            <button
              key={st}
              onClick={() => setFilterStatus(st)}
              className={`flex-1 py-1.5 rounded-lg text-xs font-bold transition-colors ${
                filterStatus === st
                  ? 'bg-navy text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {st === 'ALL' ? `All (${observations.length})` : st}
            </button>
          ))}
        </div>
      </div>

      {/* Observation Cards List */}
      <div className="space-y-3">
        {filtered.length === 0 ? (
          <div className="bg-white p-8 rounded-2xl border border-slate-200 text-center text-slate-500 text-xs">
            No observations matching current filter.
          </div>
        ) : (
          filtered.map(obs => {
            const isOverdue = obs.status === 'OPEN' && obs.targetDate < Date.now();
            return (
              <div
                key={obs.id}
                className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3 transition-all hover:border-slate-300"
              >
                {/* Header row: ID, Priority, Status */}
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-xs bg-slate-100 text-slate-800 px-2 py-0.5 rounded-md border border-slate-200">
                      {obs.entryNumber}
                    </span>
                    <span className="text-[10px] text-slate-400 font-mono">
                      {obs.recordUuid.slice(0, 8)}
                    </span>
                  </div>

                  <div className="flex items-center gap-1.5">
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                        obs.priority === 'HIGH'
                          ? 'bg-red-100 text-red-700'
                          : obs.priority === 'MEDIUM'
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-emerald-100 text-emerald-800'
                      }`}
                    >
                      {obs.priority}
                    </span>

                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 ${
                        obs.status === 'RECTIFIED'
                          ? 'bg-emerald-600 text-white'
                          : 'bg-slate-200 text-slate-700'
                      }`}
                    >
                      {obs.status === 'RECTIFIED' ? <CheckCircle2 size={11} /> : <Clock size={11} />}
                      {obs.status}
                    </span>
                  </div>
                </div>

                {/* Location Breadcrumb */}
                <div className="flex items-center gap-1.5 text-xs text-slate-600 font-medium">
                  <MapPin size={13} className="text-navy shrink-0" />
                  <span className="truncate">
                    {obs.block} &gt; {obs.floor} &gt; <strong className="text-slate-800">{obs.specificLocation}</strong>
                  </span>
                </div>

                {/* Description */}
                <p className="text-xs text-slate-800 leading-relaxed font-normal">
                  {obs.issueDescription}
                </p>

                {/* Photos Row */}
                {(obs.beforePhotoData || obs.afterPhotoData) && (
                  <div className="flex gap-2 pt-1">
                    {obs.beforePhotoData && (
                      <div className="relative">
                        <img
                          src={obs.beforePhotoData}
                          alt="Initial snag"
                          onClick={() => setActivePhotoPreview(obs.beforePhotoData!)}
                          className="w-16 h-16 object-cover rounded-xl border border-slate-200 cursor-pointer shadow-sm hover:opacity-90"
                        />
                        <span className="absolute bottom-1 left-1 bg-black/70 text-white text-[8px] font-bold px-1 rounded">
                          Before
                        </span>
                      </div>
                    )}
                    {obs.afterPhotoData && (
                      <div className="relative">
                        <img
                          src={obs.afterPhotoData}
                          alt="Rectified evidence"
                          onClick={() => setActivePhotoPreview(obs.afterPhotoData!)}
                          className="w-16 h-16 object-cover rounded-xl border border-emerald-300 cursor-pointer shadow-sm hover:opacity-90"
                        />
                        <span className="absolute bottom-1 left-1 bg-emerald-600 text-white text-[8px] font-bold px-1 rounded">
                          After
                        </span>
                      </div>
                    )}
                  </div>
                )}

                {/* Footer Info: Vendor, Logged By, Target Date & Actions */}
                <div className="pt-2 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-500">
                  <div className="flex items-center gap-2">
                    <span className="flex items-center gap-1">
                      <Building size={12} />
                      <strong className="text-slate-700">{obs.vendorName}</strong>
                    </span>
                    <span>•</span>
                    <span>Logged by: <strong>{obs.loggedByInitials}</strong></span>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className={`font-medium ${isOverdue ? 'text-red-600 font-bold' : ''}`}>
                      Due: {new Date(obs.targetDate).toLocaleDateString('en-GB')}
                    </span>

                    {obs.status === 'OPEN' && (
                      <button
                        onClick={() => setSelectedObsForRectify(obs)}
                        className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg text-xs flex items-center gap-1 shadow-sm transition-colors"
                      >
                        <CheckCircle2 size={12} />
                        <span>Rectify</span>
                      </button>
                    )}
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
        isOpen={!!selectedObsForRectify}
        observation={selectedObsForRectify}
        onClose={() => setSelectedObsForRectify(null)}
        onRectified={() => {
          loadObservations();
        }}
      />
    </div>
  );
};
