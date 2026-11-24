import React, { useState } from 'react';
import { ScrollView, View } from 'react-native';
import { Button, Dialog, Spinner } from 'heroui-native';
import { PinPadEntry } from '@/components/PinPadEntry';
import { getRepositories } from '@/db/repositories';
import { hydratePersistentLockout, sharedKioskLockout } from '@/security/persistentLockout';
import { isValidPin, verifyPin } from '@/security/securityHelper';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

const KIOSK_UNLOCK_LOCKOUT_KEY = 'kiosk_emergency_unlock';

interface KioskEmergencyUnlockDialogProps {
  readonly isOpen: boolean;
  readonly onOpenChange: (open: boolean) => void;
}

export function KioskEmergencyUnlockDialog({
  isOpen,
  onOpenChange,
}: KioskEmergencyUnlockDialogProps) {
  const [pin, setPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState(false);

  const handleClose = () => {
    setPin('');
    setError(null);
    onOpenChange(false);
  };

  const handleUnlock = async () => {
    if (!isValidPin(pin)) {
      setError('Enter a 4-digit Admin PIN.');
      return;
    }
    setIsVerifying(true);
    try {
      await hydratePersistentLockout();
      const lockMsg = sharedKioskLockout.lockoutMessage(KIOSK_UNLOCK_LOCKOUT_KEY);
      if (lockMsg) {
        setError(lockMsg);
        setPin('');
        return;
      }

      const repos = await getRepositories();
      const users = await repos.users.getAllUsers();
      const matchedAdmin = users.find(
        (u) => u.role === 'ADMIN' && verifyPin(pin, u.pinHash).isMatch,
      );
      if (!matchedAdmin) {
        const failureMsg = sharedKioskLockout.onFailure(KIOSK_UNLOCK_LOCKOUT_KEY);
        setError(failureMsg);
        setPin('');
        return;
      }
      sharedKioskLockout.onSuccess(KIOSK_UNLOCK_LOCKOUT_KEY);
      useAppBootstrapStore.getState().setKioskModeActive(false);
      handleClose();
    } finally {
      setIsVerifying(false);
    }
  };

  return (
    <Dialog isOpen={isOpen} onOpenChange={(open) => (open ? onOpenChange(true) : handleClose())}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className="mx-4 max-h-[90%] p-5 max-w-md w-full self-center">
          <ScrollView keyboardShouldPersistTaps="handled" contentContainerClassName="gap-4">
            <Dialog.Title>Kiosk Emergency Unlock</Dialog.Title>
            <Dialog.Description>
              Enter an Admin 4-digit PIN to immediately stop Lock Task mode and return to normal operation.
            </Dialog.Description>

            <PinPadEntry
              pin={pin}
              onPinChange={(next) => {
                setPin(next);
                setError(null);
              }}
              isDisabled={isVerifying}
              errorMessage={error}
            />

            <View className="flex-row justify-end gap-2 pt-2">
              <Button variant="secondary" onPress={handleClose} isDisabled={isVerifying}>
                <Button.Label>Cancel</Button.Label>
              </Button>
              <Button variant="danger" onPress={handleUnlock} isDisabled={isVerifying || pin.length < 4}>
                {isVerifying ? <Spinner size="sm" /> : null}
                <Button.Label>{isVerifying ? 'Verifying...' : 'Unlock Kiosk'}</Button.Label>
              </Button>
            </View>
          </ScrollView>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
