/**
 * Progressive backoff for failed high-value local PIN attempts.
 * Direct TypeScript port of Kotlin util/KioskLockdownPolicy.kt.
 */
export const KIOSK_LOCKDOWN_POLICY = {
  FREE_ATTEMPTS: 2,
  MAX_LOCKOUT_MS: 300_000,
  WINDOWS_MS: [30_000, 60_000, 120_000, 300_000] as const,
} as const;

export function lockoutWindowFor(failCount: number): number {
  if (failCount <= KIOSK_LOCKDOWN_POLICY.FREE_ATTEMPTS) {
    return 0;
  }
  const index = Math.min(
    failCount - KIOSK_LOCKDOWN_POLICY.FREE_ATTEMPTS - 1,
    KIOSK_LOCKDOWN_POLICY.WINDOWS_MS.length - 1,
  );
  return KIOSK_LOCKDOWN_POLICY.WINDOWS_MS[index] ?? KIOSK_LOCKDOWN_POLICY.MAX_LOCKOUT_MS;
}

export function remainingLockoutMs(lockedUntil: number, now: number): number {
  return now < lockedUntil ? lockedUntil - now : 0;
}

export function formatLockoutMessage(lockedUntil: number, now: number): string {
  const remainSec = Math.max(1, Math.floor(remainingLockoutMs(lockedUntil, now) / 1000));
  return `Too many failed attempts. Locked for ${remainSec}s.`;
}

export interface LockoutStorageAdapter {
  readFailCount: (key: string) => number;
  readLockedUntil: (key: string) => number;
  writeState: (key: string, failCount: number, lockedUntil: number) => void;
  now?: () => number;
}

export class KioskLockout {
  private readonly now: () => number;

  constructor(private readonly adapter: LockoutStorageAdapter) {
    this.now = adapter.now ?? (() => Date.now());
  }

  remainingMs(key: string): number {
    return remainingLockoutMs(this.adapter.readLockedUntil(key), this.now());
  }

  lockoutMessage(key: string): string | null {
    const remain = this.remainingMs(key);
    if (remain <= 0) {
      return null;
    }
    const remainSec = Math.max(1, Math.floor(remain / 1000));
    return `Too many failed attempts. Locked for ${remainSec}s.`;
  }

  onSuccess(key: string): void {
    this.adapter.writeState(key, 0, 0);
  }

  onFailure(key: string): string {
    const fails = this.adapter.readFailCount(key) + 1;
    const windowMs = lockoutWindowFor(fails);
    if (windowMs > 0) {
      const currentNow = this.now();
      const lockedUntil = currentNow + windowMs;
      this.adapter.writeState(key, 0, lockedUntil);
      return formatLockoutMessage(lockedUntil, currentNow);
    }
    this.adapter.writeState(key, fails, 0);
    const remainingFree = KIOSK_LOCKDOWN_POLICY.FREE_ATTEMPTS - fails;
    if (remainingFree > 0) {
      const unit = remainingFree === 1 ? 'attempt' : 'attempts';
      return `Incorrect PIN. ${remainingFree} ${unit} remaining before lockout.`;
    }
    return 'Incorrect PIN. This is your last attempt before a lockout.';
  }
}
