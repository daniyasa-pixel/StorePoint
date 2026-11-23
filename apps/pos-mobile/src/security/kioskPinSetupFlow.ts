import { isValidPin, isWeakPin, PIN_LENGTH, pinPolicyHint } from './securityHelper';

export type KioskPinSetupStage = 'ENTER' | 'VERIFY';

export function isKioskPinComplete(pin: string): boolean {
  return isValidPin(pin);
}

export function enterKioskPinRejection(pin: string): string | null {
  if (!isValidPin(pin)) {
    return `Kiosk PIN must be exactly ${PIN_LENGTH} digits.`;
  }
  if (isWeakPin(pin)) {
    return `That Kiosk PIN is too easy to guess. ${pinPolicyHint()}`;
  }
  return null;
}

export function verifyKioskPinRejection(entered: string, confirmed: string): string | null {
  if (entered !== confirmed) {
    return `PINs do not match. Re-enter the same ${PIN_LENGTH} digits.`;
  }
  if (!isValidPin(entered)) {
    return `Kiosk PIN must be exactly ${PIN_LENGTH} digits.`;
  }
  return null;
}
