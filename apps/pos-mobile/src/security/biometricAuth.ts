import * as LocalAuthentication from 'expo-local-authentication';

export type BiometricAvailability = 'AVAILABLE' | 'NOT_ENROLLED' | 'NO_HARDWARE';
export type BiometricOutcome = 'SUCCESS' | 'FALLBACK' | 'RECOVERABLE_ERROR' | 'UNAVAILABLE';

export async function checkBiometricAvailability(): Promise<BiometricAvailability> {
  try {
    const hasHardware = await LocalAuthentication.hasHardwareAsync();
    if (!hasHardware) return 'NO_HARDWARE';
    const isEnrolled = await LocalAuthentication.isEnrolledAsync();
    return isEnrolled ? 'AVAILABLE' : 'NOT_ENROLLED';
  } catch {
    return 'NO_HARDWARE';
  }
}

export async function authenticateWithBiometrics(
  promptMessage = 'Authenticate to unlock StorePoint POS',
): Promise<BiometricOutcome> {
  const availability = await checkBiometricAvailability();
  if (availability !== 'AVAILABLE') {
    return 'UNAVAILABLE';
  }

  try {
    const result = await LocalAuthentication.authenticateAsync({
      promptMessage,
      fallbackLabel: 'Use 4-Digit PIN',
      cancelLabel: 'Cancel',
      disableDeviceFallback: false,
    });
    if (result.success) {
      return 'SUCCESS';
    }
    if (result.error === 'user_cancel' || result.error === 'user_fallback') {
      return 'FALLBACK';
    }
    return 'RECOVERABLE_ERROR';
  } catch {
    return 'RECOVERABLE_ERROR';
  }
}
