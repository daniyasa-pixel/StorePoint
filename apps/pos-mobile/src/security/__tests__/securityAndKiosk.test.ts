import { hashPin, isValidPin, isWeakPin, verifyPin } from '../securityHelper';
import { KioskLockout, lockoutWindowFor } from '../kioskLockdownPolicy';
import { enterKioskPinRejection, verifyKioskPinRejection } from '../kioskPinSetupFlow';

describe('SecurityHelper PIN policy and PBKDF2 hashing', () => {
  it('validates strict 4-digit PINs and rejects weak patterns', () => {
    expect(isValidPin('4829')).toBe(true);
    expect(isValidPin('123')).toBe(false);
    expect(isValidPin('123456')).toBe(false);

    expect(isWeakPin('0000')).toBe(true);
    expect(isWeakPin('1234')).toBe(true);
    expect(isWeakPin('4321')).toBe(true);
    expect(isWeakPin('2580')).toBe(true);
    expect(isWeakPin('1212')).toBe(true);
    expect(isWeakPin('4829')).toBe(false);
  });

  it('hashes with salted PBKDF2-HMAC-SHA256 format and verifies correctly', () => {
    // Use lower iteration count in unit test for fast execution while verifying format & upgrade flag
    const fastHash = hashPin('4829', 1000);
    expect(fastHash.startsWith('pbkdf2$1000$')).toBe(true);
    expect(verifyPin('4829', fastHash)).toEqual({ isMatch: true, needsUpgrade: true });
    expect(verifyPin('9999', fastHash)).toEqual({ isMatch: false, needsUpgrade: false });
  });
});

describe('KioskLockdownPolicy & KioskPinSetupFlow', () => {
  it('enforces escalating backoff windows after 2 free attempts', () => {
    expect(lockoutWindowFor(1)).toBe(0);
    expect(lockoutWindowFor(2)).toBe(0);
    expect(lockoutWindowFor(3)).toBe(30_000);
    expect(lockoutWindowFor(4)).toBe(60_000);
    expect(lockoutWindowFor(5)).toBe(120_000);
    expect(lockoutWindowFor(6)).toBe(300_000);
    expect(lockoutWindowFor(10)).toBe(300_000);

    let now = 1_000_000;
    const state = new Map<string, { fails: number; until: number }>();
    const lockout = new KioskLockout({
      readFailCount: (k) => state.get(k)?.fails ?? 0,
      readLockedUntil: (k) => state.get(k)?.until ?? 0,
      writeState: (k, fails, until) => state.set(k, { fails, until }),
      now: () => now,
    });

    expect(lockout.onFailure('kiosk_pin')).toContain('1 attempt remaining');
    expect(lockout.onFailure('kiosk_pin')).toContain('last attempt');
    expect(lockout.onFailure('kiosk_pin')).toContain('Locked for 30s');
    expect(lockout.remainingMs('kiosk_pin')).toBe(30_000);

    now += 31_000;
    expect(lockout.remainingMs('kiosk_pin')).toBe(0);
    lockout.onSuccess('kiosk_pin');
    expect(lockout.lockoutMessage('kiosk_pin')).toBeNull();
  });

  it('validates 2-stage kiosk PIN setup flow', () => {
    expect(enterKioskPinRejection('1234')).toContain('too easy to guess');
    expect(enterKioskPinRejection('4829')).toBeNull();
    expect(verifyKioskPinRejection('4829', '4820')).toContain('do not match');
    expect(verifyKioskPinRejection('4829', '4829')).toBeNull();
  });
});
