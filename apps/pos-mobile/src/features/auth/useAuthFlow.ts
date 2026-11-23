import { useCallback, useEffect, useRef, useState } from 'react';
import { getRepositories } from '@/db/repositories';
import type { User } from '@/db/types';
import {
  authenticateWithBiometrics,
  checkBiometricAvailability,
  type BiometricAvailability,
} from '@/security/biometricAuth';
import { KioskLockout } from '@/security/kioskLockdownPolicy';
import { hashPin, isValidPin, verifyPin } from '@/security/securityHelper';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

export interface LoginOutcome {
  readonly ok: boolean;
  readonly user?: User;
  readonly requiresPinReset?: boolean;
  readonly error?: string;
}

export function useAuthFlow() {
  const [users, setUsers] = useState<User[]>([]);
  const [biometricAvailability, setBiometricAvailability] =
    useState<BiometricAvailability>('NO_HARDWARE');
  const [isLoading, setIsLoading] = useState(true);

  const lockoutStoreRef = useRef(new Map<string, { fails: number; until: number }>());
  const lockoutRef = useRef(
    new KioskLockout({
      readFailCount: (k) => lockoutStoreRef.current.get(k)?.fails ?? 0,
      readLockedUntil: (k) => lockoutStoreRef.current.get(k)?.until ?? 0,
      writeState: (k, fails, until) => lockoutStoreRef.current.set(k, { fails, until }),
    }),
  );

  const refreshUsers = useCallback(async () => {
    setIsLoading(true);
    try {
      const [repos, bio] = await Promise.all([
        getRepositories(),
        checkBiometricAvailability(),
      ]);
      const allUsers = await repos.users.getAllUsers();
      setUsers(allUsers);
      setBiometricAvailability(bio);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void refreshUsers();
  }, [refreshUsers]);

  const loginWithPin = useCallback(
    async (username: string, pin: string): Promise<LoginOutcome> => {
      const normalized = username.trim();
      if (!normalized) {
        return { ok: false, error: 'Select or enter a staff username.' };
      }
      if (!isValidPin(pin)) {
        return { ok: false, error: 'PIN must be exactly 4 digits.' };
      }

      const lockMsg = lockoutRef.current.lockoutMessage(`login:${normalized}`);
      if (lockMsg) {
        return { ok: false, error: lockMsg };
      }

      const repos = await getRepositories();
      const user = await repos.users.getByUsername(normalized);
      if (!user) {
        return { ok: false, error: 'Staff account not found.' };
      }

      if (user.pinResetRequired) {
        return {
          ok: false,
          user,
          requiresPinReset: true,
          error: 'Account requires 4-digit PIN migration before sign-in.',
        };
      }

      const verified = verifyPin(pin, user.pinHash);
      if (!verified.isMatch) {
        const failureMsg = lockoutRef.current.onFailure(`login:${normalized}`);
        return { ok: false, error: failureMsg };
      }

      lockoutRef.current.onSuccess(`login:${normalized}`);

      if (verified.needsUpgrade) {
        await repos.users.updatePinHash(user.username, hashPin(pin), true);
      }

      if (!user.biometricEnrolled && biometricAvailability === 'AVAILABLE') {
        await repos.users.setBiometricEnrolled(user.username, true);
      }

      useAppBootstrapStore.getState().setActiveUser({
        id: 1,
        username: user.username,
        fullName: user.username,
        role: user.role,
        pinResetRequired: false,
      });

      return { ok: true, user };
    },
    [biometricAvailability],
  );

  const loginWithBiometric = useCallback(async (): Promise<LoginOutcome> => {
    const repos = await getRepositories();
    const enrolled = await repos.users.getBiometricEnrolledUsers();
    if (enrolled.length !== 1) {
      return {
        ok: false,
        error:
          enrolled.length === 0
            ? 'No biometric account enrolled yet. Sign in with PIN first.'
            : 'Multiple accounts enrolled; select your account and sign in with PIN.',
      };
    }

    const target = enrolled[0]!;
    if (target.pinResetRequired) {
      return { ok: false, user: target, requiresPinReset: true };
    }

    const outcome = await authenticateWithBiometrics(`Sign in as ${target.username}`);
    if (outcome !== 'SUCCESS') {
      return { ok: false, error: 'Biometric authentication cancelled or failed.' };
    }

    useAppBootstrapStore.getState().setActiveUser({
      id: 1,
      username: target.username,
      fullName: target.username,
      role: target.role,
      pinResetRequired: false,
    });
    return { ok: true, user: target };
  }, []);

  return {
    users,
    biometricAvailability,
    isLoading,
    refreshUsers,
    loginWithPin,
    loginWithBiometric,
  };
}
