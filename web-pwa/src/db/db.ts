import Dexie, { type Table } from 'dexie';
import {
  Project,
  Block,
  Floor,
  SubLocation,
  Vendor,
  CustomFieldDefinition,
  Observation,
  SyncOutboxItem,
  EngineerProfile
} from '../types';

export class SnagOpsDatabase extends Dexie {
  projects!: Table<Project, number>;
  blocks!: Table<Block, number>;
  floors!: Table<Floor, number>;
  subLocations!: Table<SubLocation, number>;
  vendors!: Table<Vendor, number>;
  customFields!: Table<CustomFieldDefinition, string>;
  observations!: Table<Observation, number>;
  syncOutbox!: Table<SyncOutboxItem, number>;

  constructor() {
    super('SnagOpsDB');
    this.version(1).stores({
      projects: '++id, recordUuid, isActive, projectCode',
      blocks: '++id, recordUuid, projectId',
      floors: '++id, recordUuid, projectId, blockId',
      subLocations: '++id, recordUuid, projectId, floorId',
      vendors: '++id, recordUuid, projectId',
      customFields: 'id, recordUuid, projectId',
      observations: '++id, recordUuid, projectId, entryNumber, recordType, status, loggedByInitials',
      syncOutbox: '++id, recordUuid, projectId, status'
    });
  }
}

export const db = new SnagOpsDatabase();

// Seed initial projects and master data if empty
export async function seedInitialDataIfNeeded() {
  const count = await db.projects.count();
  if (count > 0) return;

  // Project 1
  const p1Id = await db.projects.add({
    recordUuid: 'proj-metro-001',
    projectName: 'Metro Residences - Tower 1 & 2',
    projectCode: 'MR-2026',
    clientContractorName: 'Skyline Construction Corp / Apex Dev',
    spreadsheetId: '1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms',
    defaultCcEmails: 'qaqc.lead@skyline.com, pm@skyline.com',
    emailFrequency: 'Daily',
    isActive: true,
    updatedAt: Date.now()
  });

  // Project 2
  await db.projects.add({
    recordUuid: 'proj-bayfront-002',
    projectName: 'Grand Bayfront Complex',
    projectCode: 'GBC-PHASE-1',
    clientContractorName: 'Skyline Developers Group',
    spreadsheetId: '2CyjNWt1YSB6oGNeLwCeCakhVVRrqumct85PhwF3vqnt',
    defaultCcEmails: 'pmc@skyline.com',
    emailFrequency: 'Alternate Days',
    isActive: false,
    updatedAt: Date.now()
  });

  // Blocks for Project 1
  const bTowerA = await db.blocks.add({ recordUuid: 'blk-ta', projectId: p1Id, name: 'Tower A' });
  const bTowerB = await db.blocks.add({ recordUuid: 'blk-tb', projectId: p1Id, name: 'Tower B' });
  const bPodium = await db.blocks.add({ recordUuid: 'blk-pod', projectId: p1Id, name: 'Podium & Amenities' });
  await db.blocks.add({ recordUuid: 'blk-ext', projectId: p1Id, name: 'External / Civil Works' });

  // Floors for Tower A
  await db.floors.add({ recordUuid: 'flr-b1', projectId: p1Id, blockId: bTowerA, name: 'Basement 1 - Parking' });
  await db.floors.add({ recordUuid: 'flr-g', projectId: p1Id, blockId: bTowerA, name: 'Ground Floor Lobby' });
  const fLvl1 = await db.floors.add({ recordUuid: 'flr-l1', projectId: p1Id, blockId: bTowerA, name: '1st Floor Residential' });
  await db.floors.add({ recordUuid: 'flr-l2', projectId: p1Id, blockId: bTowerA, name: '2nd Floor Residential' });

  // Floors for Tower B & Podium
  await db.floors.add({ recordUuid: 'flr-tb-g', projectId: p1Id, blockId: bTowerB, name: 'Ground Floor' });
  await db.floors.add({ recordUuid: 'flr-pod-deck', projectId: p1Id, blockId: bPodium, name: 'Swimming Pool Deck' });

  // Sub-locations for Floor 1
  await db.subLocations.add({ recordUuid: 'sub-101-mb', projectId: p1Id, floorId: fLvl1, name: 'Flat 101 - Master Bedroom' });
  await db.subLocations.add({ recordUuid: 'sub-101-ba', projectId: p1Id, floorId: fLvl1, name: 'Flat 101 - Master Bath' });
  await db.subLocations.add({ recordUuid: 'sub-102-lv', projectId: p1Id, floorId: fLvl1, name: 'Flat 102 - Living Area' });
  await db.subLocations.add({ recordUuid: 'sub-corridor', projectId: p1Id, floorId: fLvl1, name: 'Common Corridor - Grid B2' });

  // Vendors
  await db.vendors.add({
    recordUuid: 'ven-apex',
    projectId: p1Id,
    name: 'Apex Waterproofing Ltd',
    trade: 'Waterproofing',
    primaryEmail: 'site.manager@apexwaterproofing.com',
    ccEmails: 'operations@apexwaterproofing.com'
  });

  await db.vendors.add({
    recordUuid: 'ven-mep',
    projectId: p1Id,
    name: 'Prime MEP Engineers',
    trade: 'MEP / Plumbing & Electrical',
    primaryEmail: 'project.lead@primemep.com',
    ccEmails: 'qa@primemep.com'
  });

  await db.vendors.add({
    recordUuid: 'ven-finishing',
    projectId: p1Id,
    name: 'Global Finishing & Cladding',
    trade: 'Finishing & Tiling',
    primaryEmail: 'site@globalfinishing.com',
    ccEmails: 'supervisor@globalfinishing.com'
  });

  // Custom Fields
  await db.customFields.add({
    id: 'field_dwg_ref',
    recordUuid: 'cf-dwg',
    projectId: p1Id,
    name: 'Drawing / Specification Ref',
    type: 'TEXT',
    options: [],
    isRequired: false,
    orderIndex: 1
  });

  await db.customFields.add({
    id: 'field_insp_stage',
    recordUuid: 'cf-stage',
    projectId: p1Id,
    name: 'Inspection Stage',
    type: 'DROPDOWN',
    options: ['Pre-Pour Inspection', 'Post-Demolding', 'First Fix MEP', 'Handover Punch'],
    isRequired: false,
    orderIndex: 2
  });
}
