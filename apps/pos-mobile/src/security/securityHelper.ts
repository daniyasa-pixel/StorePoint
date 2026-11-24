import { pbkdf2 } from '@noble/hashes/pbkdf2.js';
import { sha256 } from '@noble/hashes/sha2.js';
import { bytesToHex, hexToBytes, randomBytes } from '@noble/hashes/utils.js';

export const PIN_LENGTH = 4;
const PBKDF2_ITERATIONS = 120_000;
const MIN_PBKDF2_ITERATIONS = 1_000;
const MAX_PBKDF2_ITERATIONS = 500_000;
const KEY_LENGTH_BYTES = 32; // 256 bits
const SALT_BYTES_LENGTH = 16;

export interface VerificationResult {
  readonly isMatch: boolean;
  readonly needsUpgrade: boolean;
}

export function isValidPin(pin: string): boolean {
  return pin.length === PIN_LENGTH && /^\d{4}$/.test(pin);
}

function isAscendingRun(pin: string): boolean {
  for (let i = 0; i < pin.length - 1; i += 1) {
    if (pin.charCodeAt(i + 1) - pin.charCodeAt(i) !== 1) {
      return false;
    }
  }
  return true;
}

function isDescendingRun(pin: string): boolean {
  for (let i = 0; i < pin.length - 1; i += 1) {
    if (pin.charCodeAt(i) - pin.charCodeAt(i + 1) !== 1) {
      return false;
    }
  }
  return true;
}

/**
 * Rejects trivially guessable 4-digit PINs at creation time.
 * Exact parity with Kotlin SecurityHelper.isWeakPin().
 */
export function isWeakPin(pin: string): boolean {
  if (!isValidPin(pin)) return true;
  if (new Set(pin.split('')).size === 1) return true;
  if (isAscendingRun(pin) || isDescendingRun(pin)) return true;
  if (pin === '1212' || pin === '2121') return true;
  if (pin === '1000' || pin === '0001' || pin === '1010') return true;
  if (pin === '2580' || pin === '0852') return true;
  return false;
}

export function pinPolicyHint(): string {
  return `Must be exactly ${PIN_LENGTH} digits. Avoid repeats (0000), runs (1234), and patterns (2580, 1212).`;
}

function constantTimeEquals(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i += 1) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return diff === 0;
}

/**
 * Generates a salted PBKDF2-HMAC-SHA256 hash wire-compatible with Kotlin SecurityHelper:
 *   pbkdf2$120000$<saltHex>$<hashHex>
 */
export function hashPin(pin: string, iterations = PBKDF2_ITERATIONS): string {
  const salt = randomBytes(SALT_BYTES_LENGTH);
  const derived = pbkdf2(sha256, pin, salt, {
    c: iterations,
    dkLen: KEY_LENGTH_BYTES,
  });
  return `pbkdf2$${iterations}$${bytesToHex(salt)}$${bytesToHex(derived)}`;
}

/**
 * Verifies a candidate PIN/password against modern PBKDF2, legacy SHA-256, or legacy plaintext.
 */
export function verifyPin(pin: string, storedHash: string): VerificationResult {
  if (!storedHash || storedHash.trim().length === 0) {
    return { isMatch: false, needsUpgrade: false };
  }

  if (storedHash.startsWith('pbkdf2$')) {
    const parts = storedHash.split('$');
    if (parts.length !== 4) {
      return { isMatch: false, needsUpgrade: false };
    }
    const iterStr = parts[1] ?? '';
    const saltHex = parts[2] ?? '';
    const expectedHex = parts[3] ?? '';
    if (!/^\d+$/.test(iterStr)) {
      return { isMatch: false, needsUpgrade: false };
    }
    const iterations = Number(iterStr);
    if (
      !Number.isSafeInteger(iterations) ||
      iterations < MIN_PBKDF2_ITERATIONS ||
      iterations > MAX_PBKDF2_ITERATIONS
    ) {
      return { isMatch: false, needsUpgrade: false };
    }
    try {
      const salt = hexToBytes(saltHex);
      const computed = pbkdf2(sha256, pin, salt, {
        c: iterations,
        dkLen: KEY_LENGTH_BYTES,
      });
      const computedHex = bytesToHex(computed);
      const isMatch = constantTimeEquals(computedHex.toLowerCase(), expectedHex.toLowerCase());
      return {
        isMatch,
        needsUpgrade: isMatch && iterations < PBKDF2_ITERATIONS,
      };
    } catch {
      return { isMatch: false, needsUpgrade: false };
    }
  }

  // Legacy 64-char SHA-256 hex check
  if (/^[0-9a-fA-F]{64}$/.test(storedHash)) {
    const candidateHex = bytesToHex(sha256(new TextEncoder().encode(pin)));
    if (constantTimeEquals(candidateHex.toLowerCase(), storedHash.toLowerCase())) {
      return { isMatch: true, needsUpgrade: true };
    }
  }

  // Legacy plaintext fallback check
  if (constantTimeEquals(pin, storedHash)) {
    return { isMatch: true, needsUpgrade: true };
  }

  return { isMatch: false, needsUpgrade: false };
}
