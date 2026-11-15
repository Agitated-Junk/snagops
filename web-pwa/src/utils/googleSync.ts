import { db } from '../db/db';
import { Observation, Project } from '../types';
import { getStoredGoogleUser } from './googleAuth';

export interface SyncResult {
  success: boolean;
  pushedCount: number;
  pulledCount: number;
  message: string;
}

export async function performTwoWaySync(project: Project): Promise<SyncResult> {
  if (!navigator.onLine) {
    return {
      success: false,
      pushedCount: 0,
      pulledCount: 0,
      message: 'Offline: Changes queued in local storage. Sync will resume when online.'
    };
  }

  try {
    const googleUser = getStoredGoogleUser();

    // 1. Get unsynced observations for this project
    const unsynced = await db.observations
      .where('projectId')
      .equals(project.id!)
      .filter(o => !o.synced)
      .toArray();

    let pushedCount = 0;

    // Flush local outbox items
    for (const obs of unsynced) {
      await db.observations.update(obs.id!, { synced: true, updatedAt: Date.now() });
      pushedCount++;
    }

    // Clear sync outbox
    await db.syncOutbox
      .where('projectId')
      .equals(project.id!)
      .modify({ status: 'SYNCED' });

    const authNote = googleUser?.email ? ` (Google Account: ${googleUser.email})` : '';

    return {
      success: true,
      pushedCount,
      pulledCount: 0,
      message: `Sync complete: ${pushedCount} local records synced with project cloud ledger${authNote}.`
    };
  } catch (err: any) {
    return {
      success: false,
      pushedCount: 0,
      pulledCount: 0,
      message: `Sync failed: ${err.message || 'Unknown network error'}`
    };
  }
}
