import React, { useState, useEffect, useRef } from 'react';
import {
  Project,
  EngineerProfile,
  RecordType,
  Priority,
  Block,
  Floor,
  SubLocation,
  Vendor,
  CustomFieldDefinition
} from '../types';
import { db } from '../db/db';
import { compressImage } from '../utils/imageCompression';
import { generateNcrPdf } from '../utils/pdfGenerator';
import {
  Camera,
  Image as ImageIcon,
  Mic,
  MicOff,
  Check,
  AlertTriangle,
  FileText,
  Calendar,
  Sparkles,
  Download
} from 'lucide-react';

interface CaptureViewProps {
  project: Project;
  profile: EngineerProfile;
  onNavigateToNcr: () => void;
}

export const CaptureView: React.FC<CaptureViewProps> = ({
  project,
  profile,
  onNavigateToNcr
}) => {
  // Form State
  const [recordType, setRecordType] = useState<RecordType>('NORMAL_OBSERVATION');
  const [photoData, setPhotoData] = useState<string | null>(null);
  const [isCompressing, setIsCompressing] = useState(false);

  // Cascading Location Hierarchy
  const [blocks, setBlocks] = useState<Block[]>([]);
  const [floors, setFloors] = useState<Floor[]>([]);
  const [subLocations, setSubLocations] = useState<SubLocation[]>([]);

  const [selectedBlock, setSelectedBlock] = useState<string>('');
  const [selectedFloor, setSelectedFloor] = useState<string>('');
  const [selectedSubLocation, setSelectedSubLocation] = useState<string>('');
  const [customLocation, setCustomLocation] = useState<string>('');

  // Vendors & Trade
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [selectedVendor, setSelectedVendor] = useState<string>('');
  const [trade, setTrade] = useState<string>('');

  // Priority & Target Date
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [targetDateOffsetDays, setTargetDateOffsetDays] = useState<number>(7);

  // Description & NCR Fields
  const [description, setDescription] = useState<string>('');
  const [rootCause, setRootCause] = useState<string>('');
  const [correctiveAction, setCorrectiveAction] = useState<string>('');

  // Voice to text
  const [isListening, setIsListening] = useState(false);

  // Custom Fields
  const [customFieldDefs, setCustomFieldDefs] = useState<CustomFieldDefinition[]>([]);
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, string>>({});

  // Status & Saved Feedback
  const [savedSuccessId, setSavedSuccessId] = useState<string | null>(null);
  const [savedPdfDoc, setSavedPdfDoc] = useState<any | null>(null);
  const [errorMessage, setErrorMessage] = useState<string>('');

  const cameraInputRef = useRef<HTMLInputElement>(null);
  const galleryInputRef = useRef<HTMLInputElement>(null);

  // Load project master data
  useEffect(() => {
    async function loadMasterData() {
      if (!project.id) return;
      const bList = await db.blocks.where('projectId').equals(project.id).toArray();
      const vList = await db.vendors.where('projectId').equals(project.id).toArray();
      const cfList = await db.customFields.where('projectId').equals(project.id).toArray();

      setBlocks(bList);
      setVendors(vList);
      setCustomFieldDefs(cfList);

      if (bList.length > 0 && !selectedBlock) {
        setSelectedBlock(bList[0].name);
      }
      if (vList.length > 0 && !selectedVendor) {
        setSelectedVendor(vList[0].name);
        setTrade(vList[0].trade);
      }
    }
    loadMasterData();
  }, [project.id]);

  // Load floors when block changes
  useEffect(() => {
    async function loadFloors() {
      if (!project.id || !selectedBlock) return;
      const blk = blocks.find(b => b.name === selectedBlock);
      if (blk && blk.id) {
        const fList = await db.floors
          .where('blockId')
          .equals(blk.id)
          .toArray();
        setFloors(fList);
        if (fList.length > 0) {
          setSelectedFloor(fList[0].name);
        } else {
          setSelectedFloor('');
        }
      }
    }
    loadFloors();
  }, [selectedBlock, blocks, project.id]);

  // Load sublocations when floor changes
  useEffect(() => {
    async function loadSubLocations() {
      if (!project.id || !selectedFloor) return;
      const flr = floors.find(f => f.name === selectedFloor);
      if (flr && flr.id) {
        const subList = await db.subLocations
          .where('floorId')
          .equals(flr.id)
          .toArray();
        setSubLocations(subList);
        if (subList.length > 0) {
          setSelectedSubLocation(subList[0].name);
        } else {
          setSelectedSubLocation('');
        }
      }
    }
    loadSubLocations();
  }, [selectedFloor, floors, project.id]);

  // Handle Photo selection
  const handlePhotoCapture = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setIsCompressing(true);
      setErrorMessage('');
      const dataUrl = await compressImage(file);
      setPhotoData(dataUrl);
    } catch {
      setErrorMessage('Image compression failed. Please try another photo.');
    } finally {
      setIsCompressing(false);
    }
  };

  // Voice to text using Web Speech API
  const toggleSpeech = () => {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      alert('Speech Recognition is not supported by this browser.');
      return;
    }

    if (isListening) {
      setIsListening(false);
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.continuous = false;
      recognition.interimResults = false;
      recognition.lang = 'en-US';

      recognition.onstart = () => setIsListening(true);
      recognition.onend = () => setIsListening(false);
      recognition.onerror = () => setIsListening(false);
      recognition.onresult = (event: any) => {
        const transcript = event.results[0][0].transcript;
        setDescription(prev => (prev ? `${prev} ${transcript}` : transcript));
        setIsListening(false);
      };

      recognition.start();
    } catch {
      setIsListening(false);
    }
  };

  // Save observation
  const handleSave = async () => {
    setErrorMessage('');
    if (!description.trim()) {
      setErrorMessage('Please enter an issue description.');
      return;
    }

    const specificLoc = customLocation.trim() || selectedSubLocation || 'General Area';
    const initials = profile.initials || 'QA';

    try {
      // Calculate next sequence for this initials & type
      const prefix = recordType === 'NCR' ? `NCR-${initials}-` : `OBS-${initials}-`;
      const existing = await db.observations
        .where('projectId')
        .equals(project.id!)
        .filter(o => o.entryNumber.startsWith(prefix))
        .toArray();

      const nextNum = existing.length + 1;
      const entryNumber = `${prefix}${String(nextNum).padStart(3, '0')}`;
      const recordUuid = `rec-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

      const targetDate = Date.now() + targetDateOffsetDays * 86400000;

      const newObs = {
        recordUuid,
        projectId: project.id!,
        entryNumber,
        recordType,
        dateLogged: Date.now(),
        loggedBy: profile.fullName || 'Site Engineer',
        loggedByInitials: initials,
        block: selectedBlock || 'Block',
        floor: selectedFloor || 'Floor',
        specificLocation: specificLoc,
        vendorName: selectedVendor || 'General Vendor',
        trade: trade || 'Civil',
        priority,
        targetDate,
        issueDescription: description.trim(),
        rootCause: recordType === 'NCR' ? rootCause.trim() : undefined,
        correctiveAction: recordType === 'NCR' ? correctiveAction.trim() : undefined,
        customFields: customFieldValues,
        status: 'OPEN' as const,
        beforePhotoData: photoData || undefined,
        synced: false,
        updatedAt: Date.now()
      };

      // Generate PDF if NCR
      let generatedPdf: any = null;
      if (recordType === 'NCR') {
        generatedPdf = generateNcrPdf(newObs, project);
      }

      await db.observations.add(newObs);

      // Queue in sync outbox
      await db.syncOutbox.add({
        recordUuid,
        projectId: project.id!,
        entityType: 'OBSERVATION',
        action: 'CREATE',
        payload: JSON.stringify(newObs),
        status: 'PENDING',
        createdAt: Date.now()
      });

      setSavedSuccessId(entryNumber);
      setSavedPdfDoc(generatedPdf);

      // Reset form fields
      setDescription('');
      setRootCause('');
      setCorrectiveAction('');
      setPhotoData(null);
      setCustomLocation('');
      setCustomFieldValues({});
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to save record.');
    }
  };

  const downloadPdf = () => {
    if (savedPdfDoc && savedSuccessId) {
      savedPdfDoc.save(`SnagOps_${savedSuccessId}.pdf`);
    }
  };

  return (
    <div className="max-w-2xl mx-auto p-4 pb-24 space-y-4">
      {/* Top Segmented Toggle: Normal Observation vs NCR */}
      <div className="bg-slate-200 p-1 rounded-2xl flex shadow-inner">
        <button
          type="button"
          onClick={() => {
            setRecordType('NORMAL_OBSERVATION');
            setSavedSuccessId(null);
          }}
          className={`flex-1 py-3 rounded-xl font-bold text-xs transition-all flex items-center justify-center gap-1.5 ${
            recordType === 'NORMAL_OBSERVATION'
              ? 'bg-navy text-white shadow-md'
              : 'text-slate-700 hover:text-slate-900'
          }`}
        >
          <FileText size={16} />
          <span>Normal Observation</span>
        </button>

        <button
          type="button"
          onClick={() => {
            setRecordType('NCR');
            setSavedSuccessId(null);
          }}
          className={`flex-1 py-3 rounded-xl font-bold text-xs transition-all flex items-center justify-center gap-1.5 ${
            recordType === 'NCR'
              ? 'bg-red-600 text-white shadow-md'
              : 'text-slate-700 hover:text-slate-900'
          }`}
        >
          <AlertTriangle size={16} />
          <span>Non-Conformance (NCR)</span>
        </button>
      </div>

      {/* Engineer Initials Banner */}
      <div className="bg-white px-3 py-2 rounded-xl border border-slate-200 flex items-center justify-between text-xs text-slate-600">
        <div className="flex items-center gap-2">
          <span className="w-5 h-5 rounded-full bg-amber-400 text-navy font-black text-[10px] flex items-center justify-center">
            {profile.initials || 'QA'}
          </span>
          <span>Logged By: <strong className="text-slate-800">{profile.fullName || 'Site Engineer'}</strong></span>
        </div>
        <span className="font-mono font-bold text-navy">
          {recordType === 'NCR' ? `NCR-${profile.initials || 'QA'}-...` : `OBS-${profile.initials || 'QA'}-...`}
        </span>
      </div>

      {/* Success Notification Banner */}
      {savedSuccessId && (
        <div className="p-4 bg-emerald-50 border border-emerald-300 rounded-2xl text-emerald-900 space-y-2 animate-fadeIn">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 font-bold text-sm">
              <Check className="text-emerald-600" size={18} />
              <span>Record Saved: {savedSuccessId}</span>
            </div>
            <button
              onClick={() => setSavedSuccessId(null)}
              className="text-xs text-emerald-700 hover:underline"
            >
              Dismiss
            </button>
          </div>
          <p className="text-xs text-emerald-800">
            Record stored offline in IndexedDB and queued for two-way Google Sheets sync.
          </p>
          {savedPdfDoc && (
            <div className="pt-1 flex gap-2">
              <button
                onClick={downloadPdf}
                className="py-2 px-3 bg-red-600 hover:bg-red-700 text-white rounded-lg text-xs font-bold flex items-center gap-1.5 shadow"
              >
                <Download size={14} /> Download A4 NCR PDF
              </button>
              <button
                onClick={onNavigateToNcr}
                className="py-2 px-3 bg-white border border-slate-300 text-slate-800 rounded-lg text-xs font-bold hover:bg-slate-50"
              >
                View in NCR Register
              </button>
            </div>
          )}
        </div>
      )}

      {/* 1. Photo Capture Section */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
            <Camera size={15} />
            <span>Defect Photo Evidence</span>
          </label>
          <span className="text-[11px] text-slate-500">Auto-compressed ~150 KB</span>
        </div>

        {/* Hidden inputs */}
        <input
          type="file"
          accept="image/*"
          capture="environment"
          ref={cameraInputRef}
          onChange={handlePhotoCapture}
          className="hidden"
        />
        <input
          type="file"
          accept="image/*"
          ref={galleryInputRef}
          onChange={handlePhotoCapture}
          className="hidden"
        />

        <div className="grid grid-cols-2 gap-2">
          <button
            type="button"
            onClick={() => cameraInputRef.current?.click()}
            disabled={isCompressing}
            className="py-3.5 bg-navy text-white rounded-xl font-bold text-xs flex items-center justify-center gap-2 shadow hover:bg-slate-800 transition-colors"
          >
            <Camera size={16} />
            <span>Take Photo</span>
          </button>

          <button
            type="button"
            onClick={() => galleryInputRef.current?.click()}
            disabled={isCompressing}
            className="py-3.5 bg-white text-slate-700 border border-slate-300 rounded-xl font-bold text-xs flex items-center justify-center gap-2 hover:bg-slate-50 transition-colors"
          >
            <ImageIcon size={16} />
            <span>From Gallery</span>
          </button>
        </div>

        {isCompressing && (
          <div className="text-xs text-navy font-semibold text-center py-2 animate-pulse">
            Compressing & resizing photo to 1024px...
          </div>
        )}

        {photoData && (
          <div className="relative rounded-xl overflow-hidden border border-slate-200 bg-slate-900 group">
            <img src={photoData} alt="Captured preview" className="w-full h-48 object-cover" />
            <button
              onClick={() => setPhotoData(null)}
              className="absolute top-2 right-2 bg-black/70 hover:bg-black text-white px-2.5 py-1 rounded-md text-xs font-bold"
            >
              Remove
            </button>
            <span className="absolute bottom-2 left-2 bg-navy/90 text-white text-[10px] font-bold px-2 py-0.5 rounded-full backdrop-blur-sm">
              Optimized JPEG ~150 KB
            </span>
          </div>
        )}
      </div>

      {/* 2. Cascading Location Hierarchy (FlowRow Wrapping Chips) */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-4">
        <label className="text-xs font-bold text-slate-700 uppercase tracking-wider block">
          Location Hierarchy
        </label>

        {/* Blocks Chips */}
        <div>
          <span className="text-[11px] font-semibold text-slate-500 block mb-1.5">1. Block / Zone:</span>
          <div className="flex flex-wrap gap-2">
            {blocks.map(b => (
              <button
                key={b.id}
                type="button"
                onClick={() => setSelectedBlock(b.name)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                  selectedBlock === b.name
                    ? 'bg-navy text-white shadow-sm'
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                {b.name}
              </button>
            ))}
          </div>
        </div>

        {/* Floors Chips */}
        {floors.length > 0 && (
          <div>
            <span className="text-[11px] font-semibold text-slate-500 block mb-1.5">2. Floor / Level:</span>
            <div className="flex flex-wrap gap-2">
              {floors.map(f => (
                <button
                  key={f.id}
                  type="button"
                  onClick={() => setSelectedFloor(f.name)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                    selectedFloor === f.name
                      ? 'bg-navy text-white shadow-sm'
                      : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                  }`}
                >
                  {f.name}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Sub-locations Chips */}
        {subLocations.length > 0 && (
          <div>
            <span className="text-[11px] font-semibold text-slate-500 block mb-1.5">3. Presets:</span>
            <div className="flex flex-wrap gap-2">
              {subLocations.map(s => (
                <button
                  key={s.id}
                  type="button"
                  onClick={() => {
                    setSelectedSubLocation(s.name);
                    setCustomLocation('');
                  }}
                  className={`px-2.5 py-1 rounded-md text-xs transition-colors ${
                    selectedSubLocation === s.name && !customLocation
                      ? 'bg-amber-400 text-navy font-bold shadow-sm'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {s.name}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Custom Location Input */}
        <div>
          <span className="text-[11px] font-semibold text-slate-500 block mb-1">
            Specific Location (or override preset):
          </span>
          <input
            type="text"
            placeholder="e.g. Master Bedroom Ceiling, Column C4 Grid"
            value={customLocation}
            onChange={e => setCustomLocation(e.target.value)}
            className="w-full p-2.5 rounded-xl border border-slate-300 text-xs focus:ring-2 focus:ring-navy focus:outline-none"
          />
        </div>
      </div>

      {/* 3. Vendor & Trade Selection */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <label className="text-xs font-bold text-slate-700 uppercase tracking-wider block">
          Responsible Vendor & Trade
        </label>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-[11px] font-semibold text-slate-500 mb-1">Vendor Name:</label>
            <select
              value={selectedVendor}
              onChange={e => {
                setSelectedVendor(e.target.value);
                const ven = vendors.find(v => v.name === e.target.value);
                if (ven) setTrade(ven.trade);
              }}
              className="w-full p-2.5 rounded-xl border border-slate-300 text-xs bg-white font-medium"
            >
              {vendors.map(v => (
                <option key={v.id} value={v.name}>{v.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-slate-500 mb-1">Trade / Category:</label>
            <input
              type="text"
              value={trade}
              onChange={e => setTrade(e.target.value)}
              placeholder="e.g. Waterproofing, Civil"
              className="w-full p-2.5 rounded-xl border border-slate-300 text-xs font-medium"
            />
          </div>
        </div>
      </div>

      {/* 4. Priority & Target Date */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {/* Priority Chips */}
          <div>
            <label className="text-xs font-bold text-slate-700 uppercase tracking-wider block mb-2">
              Priority Level
            </label>
            <div className="grid grid-cols-3 gap-1.5">
              {(['HIGH', 'MEDIUM', 'LOW'] as Priority[]).map(p => (
                <button
                  key={p}
                  type="button"
                  onClick={() => setPriority(p)}
                  className={`py-2 rounded-lg text-xs font-bold transition-colors ${
                    priority === p
                      ? p === 'HIGH'
                        ? 'bg-red-600 text-white shadow'
                        : p === 'MEDIUM'
                        ? 'bg-amber-500 text-white shadow'
                        : 'bg-emerald-600 text-white shadow'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {p}
                </button>
              ))}
            </div>
          </div>

          {/* Target Closure Date Chips */}
          <div>
            <label className="text-xs font-bold text-slate-700 uppercase tracking-wider block mb-2 flex items-center gap-1">
              <Calendar size={13} />
              <span>Target Closure</span>
            </label>
            <div className="grid grid-cols-3 gap-1.5">
              {[3, 7, 14].map(days => (
                <button
                  key={days}
                  type="button"
                  onClick={() => setTargetDateOffsetDays(days)}
                  className={`py-2 rounded-lg text-xs font-bold transition-colors ${
                    targetDateOffsetDays === days
                      ? 'bg-navy text-white shadow'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  +{days} Days
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* 5. Issue Description + Voice Input */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-2">
        <div className="flex items-center justify-between">
          <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">
            Issue Description *
          </label>
          <button
            type="button"
            onClick={toggleSpeech}
            className={`flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold transition-colors ${
              isListening ? 'bg-red-600 text-white animate-pulse' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            {isListening ? <MicOff size={13} /> : <Mic size={13} />}
            <span>{isListening ? 'Listening...' : 'Dictate'}</span>
          </button>
        </div>

        <textarea
          rows={3}
          placeholder="Describe snag or defect observed on site..."
          value={description}
          onChange={e => setDescription(e.target.value)}
          className="w-full p-3 rounded-xl border border-slate-300 text-xs focus:ring-2 focus:ring-navy focus:outline-none"
        />
      </div>

      {/* 6. NCR Specific Fields (Visible only when NCR is active) */}
      {recordType === 'NCR' && (
        <div className="bg-red-50/70 border border-red-200 p-4 rounded-2xl shadow-sm space-y-3 animate-fadeIn">
          <div className="flex items-center gap-2 text-red-900 font-bold text-xs uppercase tracking-wider">
            <AlertTriangle size={16} className="text-red-600" />
            <span>Formal NCR Fields (Appears on Official A4 PDF)</span>
          </div>

          <div>
            <label className="block text-[11px] font-bold text-red-900 mb-1">
              Root Cause Analysis:
            </label>
            <textarea
              rows={2}
              placeholder="e.g. Inadequate curing membrane application / lack of vibration."
              value={rootCause}
              onChange={e => setRootCause(e.target.value)}
              className="w-full p-2.5 bg-white rounded-xl border border-red-200 text-xs focus:ring-2 focus:ring-red-600"
            />
          </div>

          <div>
            <label className="block text-[11px] font-bold text-red-900 mb-1">
              Corrective & Preventive Action Required:
            </label>
            <textarea
              rows={2}
              placeholder="e.g. Chip out porous concrete, apply polymer bonding agent, recast with micro-concrete."
              value={correctiveAction}
              onChange={e => setCorrectiveAction(e.target.value)}
              className="w-full p-2.5 bg-white rounded-xl border border-red-200 text-xs focus:ring-2 focus:ring-red-600"
            />
          </div>
        </div>
      )}

      {/* 7. Dynamic Custom Fields */}
      {customFieldDefs.length > 0 && (
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
          <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700 uppercase tracking-wider">
            <Sparkles size={14} className="text-amber-500" />
            <span>Project Custom Fields</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {customFieldDefs.map(cf => (
              <div key={cf.id}>
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                  {cf.name} {cf.isRequired && <span className="text-red-500">*</span>}
                </label>
                {cf.type === 'DROPDOWN' ? (
                  <select
                    value={customFieldValues[cf.id] || ''}
                    onChange={e => setCustomFieldValues({ ...customFieldValues, [cf.id]: e.target.value })}
                    className="w-full p-2 rounded-xl border border-slate-300 text-xs bg-white"
                  >
                    <option value="">Select option...</option>
                    {cf.options.map(opt => (
                      <option key={opt} value={opt}>{opt}</option>
                    ))}
                  </select>
                ) : (
                  <input
                    type={cf.type === 'NUMBER' ? 'number' : cf.type === 'DATE' ? 'date' : 'text'}
                    value={customFieldValues[cf.id] || ''}
                    onChange={e => setCustomFieldValues({ ...customFieldValues, [cf.id]: e.target.value })}
                    placeholder={`Enter ${cf.name}`}
                    className="w-full p-2 rounded-xl border border-slate-300 text-xs"
                  />
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Error Message */}
      {errorMessage && (
        <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl flex items-center gap-2">
          <AlertTriangle size={15} className="shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Submit Button */}
      <button
        type="button"
        onClick={handleSave}
        className={`w-full py-4 rounded-2xl font-bold text-sm shadow-lg flex items-center justify-center gap-2 transition-all ${
          recordType === 'NCR'
            ? 'bg-red-600 hover:bg-red-700 text-white'
            : 'bg-navy hover:bg-slate-800 text-white'
        }`}
      >
        <Check size={18} />
        <span>
          {recordType === 'NCR' ? 'Save NCR & Generate Instant A4 PDF' : 'Save Normal Observation'}
        </span>
      </button>
    </div>
  );
};
