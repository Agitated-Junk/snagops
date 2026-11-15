import React, { useState, useRef } from 'react';
import { Observation } from '../types';
import { db } from '../db/db';
import { compressImage } from '../utils/imageCompression';
import { Camera, Image as ImageIcon, CheckCircle2, X, AlertCircle } from 'lucide-react';

interface RectificationModalProps {
  isOpen: boolean;
  observation: Observation | null;
  onClose: () => void;
  onRectified: () => void;
}

export const RectificationModal: React.FC<RectificationModalProps> = ({
  isOpen,
  observation,
  onClose,
  onRectified
}) => {
  const [afterPhoto, setAfterPhoto] = useState<string | null>(null);
  const [closureNotes, setClosureNotes] = useState('');
  const [isCompressing, setIsCompressing] = useState(false);
  const [error, setError] = useState('');

  const cameraInputRef = useRef<HTMLInputElement>(null);
  const galleryInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen || !observation) return null;

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setIsCompressing(true);
      setError('');
      const dataUrl = await compressImage(file);
      setAfterPhoto(dataUrl);
    } catch {
      setError('Failed to compress image. Please try another photo.');
    } finally {
      setIsCompressing(false);
    }
  };

  const handleConfirm = async () => {
    if (!afterPhoto) {
      setError('A rectification after-photo is required to verify closure.');
      return;
    }

    try {
      await db.observations.update(observation.id!, {
        status: 'RECTIFIED',
        rectifiedDate: Date.now(),
        afterPhotoData: afterPhoto,
        synced: false,
        updatedAt: Date.now()
      });

      // Queue in outbox
      await db.syncOutbox.add({
        recordUuid: observation.recordUuid,
        projectId: observation.projectId,
        entityType: 'OBSERVATION',
        action: 'UPDATE_RECTIFIED',
        payload: JSON.stringify({
          recordUuid: observation.recordUuid,
          status: 'RECTIFIED',
          rectifiedDate: Date.now(),
          closureNotes
        }),
        status: 'PENDING',
        createdAt: Date.now()
      });

      onRectified();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to save rectification.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full overflow-hidden border border-slate-200">
        <div className="bg-emerald-700 p-4 px-6 text-white flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 size={22} />
            <h3 className="font-bold text-base">Mark Rectified / Verify Closure</h3>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-emerald-100 hover:text-white">
            <X size={20} />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 text-xs space-y-1">
            <div className="flex justify-between font-bold text-slate-800">
              <span>{observation.entryNumber}</span>
              <span className="text-slate-500 font-mono text-[10px]">{observation.recordUuid.slice(0, 8)}...</span>
            </div>
            <div className="text-slate-600 font-medium">
              {observation.block} &gt; {observation.floor} &gt; {observation.specificLocation}
            </div>
            <div className="text-slate-700 mt-1 line-clamp-2">
              {observation.issueDescription}
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
              Rectification Evidence Photo *
            </label>

            {/* Hidden native inputs */}
            <input
              type="file"
              accept="image/*"
              capture="environment"
              ref={cameraInputRef}
              onChange={handleFileChange}
              className="hidden"
            />
            <input
              type="file"
              accept="image/*"
              ref={galleryInputRef}
              onChange={handleFileChange}
              className="hidden"
            />

            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => cameraInputRef.current?.click()}
                disabled={isCompressing}
                className="py-3 px-3 bg-navy text-white rounded-xl font-bold text-xs flex items-center justify-center gap-2 shadow hover:bg-slate-800 transition-colors"
              >
                <Camera size={16} />
                <span>Take Photo</span>
              </button>

              <button
                type="button"
                onClick={() => galleryInputRef.current?.click()}
                disabled={isCompressing}
                className="py-3 px-3 bg-white text-slate-700 border border-slate-300 rounded-xl font-bold text-xs flex items-center justify-center gap-2 hover:bg-slate-50 transition-colors"
              >
                <ImageIcon size={16} />
                <span>From Gallery</span>
              </button>
            </div>

            {isCompressing && (
              <div className="text-xs text-navy font-semibold text-center mt-2 animate-pulse">
                Optimizing & compressing photo...
              </div>
            )}

            {afterPhoto && (
              <div className="mt-3 relative rounded-xl overflow-hidden border border-emerald-300 bg-emerald-50">
                <img
                  src={afterPhoto}
                  alt="Rectified evidence"
                  className="w-full h-40 object-cover"
                />
                <span className="absolute top-2 left-2 bg-emerald-600 text-white text-[10px] font-bold px-2 py-0.5 rounded-full shadow">
                  Rectification Photo Attached (~150 KB)
                </span>
              </div>
            )}
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
              Closure Notes / Remarks (Optional)
            </label>
            <textarea
              rows={2}
              placeholder="e.g. Honeycombing grouted with non-shrink mortar per spec."
              value={closureNotes}
              onChange={e => setClosureNotes(e.target.value)}
              className="w-full p-2.5 text-xs rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-navy"
            />
          </div>

          {error && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg flex items-center gap-2">
              <AlertCircle size={14} className="shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div className="flex gap-2 pt-2">
            <button
              onClick={onClose}
              className="flex-1 py-3 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-xl"
            >
              Cancel
            </button>
            <button
              onClick={handleConfirm}
              className="flex-1 py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow flex items-center justify-center gap-1.5"
            >
              <CheckCircle2 size={16} />
              <span>Confirm Closure</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
