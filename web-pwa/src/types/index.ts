export type RecordType = 'NORMAL_OBSERVATION' | 'NCR';
export type Priority = 'HIGH' | 'MEDIUM' | 'LOW';
export type Status = 'OPEN' | 'RECTIFIED';
export type CustomFieldType = 'TEXT' | 'NUMBER' | 'DATE' | 'DROPDOWN';

export interface EngineerProfile {
  fullName: string;
  initials: string;
  isConfigured: boolean;
}

export interface GoogleUser {
  email: string;
  name: string;
  picture?: string;
  accessToken?: string;
  idToken?: string;
  expiresAt?: number;
}

export interface Project {
  id?: number;
  recordUuid: string;
  projectName: string;
  projectCode: string;
  clientContractorName: string;
  spreadsheetId: string;
  defaultCcEmails: string;
  emailFrequency: string;
  isActive: boolean;
  updatedAt: number;
}

export interface Block {
  id?: number;
  recordUuid: string;
  projectId: number;
  name: string;
}

export interface Floor {
  id?: number;
  recordUuid: string;
  projectId: number;
  blockId: number;
  name: string;
}

export interface SubLocation {
  id?: number;
  recordUuid: string;
  projectId: number;
  floorId: number;
  name: string;
}

export interface Vendor {
  id?: number;
  recordUuid: string;
  projectId: number;
  name: string;
  trade: string;
  primaryEmail: string;
  ccEmails: string;
}

export interface CustomFieldDefinition {
  id: string;
  recordUuid: string;
  projectId: number;
  name: string;
  type: CustomFieldType;
  options: string[];
  isRequired: boolean;
  orderIndex: number;
}

export interface Observation {
  id?: number;
  recordUuid: string;
  projectId: number;
  entryNumber: string; // e.g. OBS-RK-001 or NCR-AS-001
  recordType: RecordType;
  dateLogged: number;
  loggedBy: string;
  loggedByInitials: string;
  block: string;
  floor: string;
  specificLocation: string;
  vendorName: string;
  trade: string;
  priority: Priority;
  targetDate: number;
  issueDescription: string;
  rootCause?: string;
  correctiveAction?: string;
  customFields: Record<string, string>;
  status: Status;
  rectifiedDate?: number;
  beforePhotoData?: string; // Base64 data URL
  afterPhotoData?: string;
  pdfData?: string;
  synced?: boolean;
  updatedAt: number;
}

export interface SyncOutboxItem {
  id?: number;
  recordUuid: string;
  projectId: number;
  entityType: 'OBSERVATION' | 'NCR_PDF' | 'PHOTO' | 'MASTER_DATA';
  action: 'CREATE' | 'UPDATE_RECTIFIED';
  payload: string;
  status: 'PENDING' | 'SYNCED' | 'FAILED';
  createdAt: number;
}
