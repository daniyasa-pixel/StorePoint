import AsyncStorage from '@react-native-async-storage/async-storage';
import { KioskLockout, type LockoutStorageAdapter } from '@/security/kioskLockdownPolicy';

const LOCKOUT_STORAGE_KEY = 'storepoint.security.lockout.v1';

interface LockoutEntry {
  readonly fails: number;
  readonly until: number;
}

const memoryCache = new Map<string, LockoutEntry>();
let hydrationPromise: Promise<void> | null = null;

function isValidLockoutEntry(value: unknown): value is LockoutEntry {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate.fails === 'number' &&
    Number.isFinite(candidate.fails) &&
    candidate.fails >= 0 &&
    typeof candidate.until === 'number' &&
    Number.isFinite(candidate.until) &&
    candidate.until >= 0
  );
}

function persistCache(): void {
  const payload: Record<string, LockoutEntry> = {};
  for (const [key, entry] of memoryCache.entries()) {
    if (entry.fails > 0 || entry.until > 0) {
      payload[key] = entry;
    }
  }
  void AsyncStorage.setItem(LOCKOUT_STORAGE_KEY, JSON.stringify(payload)).catch(() => {
    // Non-fatal: in-memory cache remains authoritative for the current session.
  });
}

export function hydratePersistentLockout(): Promise<void> {
  if (!hydrationPromise) {
    hydrationPromise = AsyncStorage.getItem(LOCKOUT_STORAGE_KEY)
      .then((raw) => {
        if (!raw) {
          return;
        }
        const parsed: unknown = JSON.parse(raw);
        if (typeof parsed !== 'object' || parsed === null) {
          return;
        }
        for (const [key, value] of Object.entries(parsed as Record<string, unknown>)) {
          if (!memoryCache.has(key) && isValidLockoutEntry(value)) {
            memoryCache.set(key, { fails: value.fails, until: value.until });
          }
        }
      })
      .catch(() => {
        // Ignore corrupt or unavailable storage; fail open to empty cache.
      });
  }
  return hydrationPromise;
}

const persistentLockoutAdapter: LockoutStorageAdapter = {
  readFailCount: (key) => memoryCache.get(key)?.fails ?? 0,
  readLockedUntil: (key) => memoryCache.get(key)?.until ?? 0,
  writeState: (key, failCount, lockedUntil) => {
    if (failCount <= 0 && lockedUntil <= 0) {
      memoryCache.delete(key);
    } else {
      memoryCache.set(key, { fails: failCount, until: lockedUntil });
    }
    persistCache();
  },
};

export const sharedKioskLockout = new KioskLockout(persistentLockoutAdapter);
