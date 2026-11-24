import React, { useState } from 'react';
import { Text, View } from 'react-native';
import {
  Button,
  Card,
  Chip,
  Input,
  Label,
  Separator,
  Spinner,
  Surface,
  Switch,
  TextField,
} from 'heroui-native';
import { PinPadEntry } from '@/components/PinPadEntry';
import { TactileButton } from '@/components/TactilePressable';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { isValidPin, isWeakPin, pinPolicyHint } from '@/security/securityHelper';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

export interface SetupSecurityFormValues {
  readonly adminName: string;
  readonly adminPhone: string;
  readonly adminUsername: string;
  readonly adminPin: string;
  readonly confirmPin: string;
}

export interface SetupStepTwoSecurityProps {
  readonly values: SetupSecurityFormValues;
  readonly isSaving: boolean;
  readonly onChange: <K extends keyof SetupSecurityFormValues>(
    field: K,
    value: SetupSecurityFormValues[K],
  ) => void;
  readonly onBack: () => void;
  readonly onComplete: () => void;
}

type PinSubstage = 'enter' | 'confirm';

function resolvePinStatusChip(adminPin: string, confirmPin: string): {
  readonly label: string;
  readonly color: 'default' | 'danger' | 'warning' | 'success';
} {
  if (adminPin.length < 4) {
    return { label: `Enter 4 Digits (${adminPin.length}/4)`, color: 'default' };
  }
  if (!isValidPin(adminPin) || isWeakPin(adminPin)) {
    return { label: 'Too Weak', color: 'danger' };
  }
  if (confirmPin.length === 0) {
    return { label: 'PIN Ready', color: 'success' };
  }
  if (confirmPin.length < 4) {
    return { label: `Confirming (${confirmPin.length}/4)`, color: 'warning' };
  }
  if (confirmPin === adminPin) {
    return { label: 'PINs Match', color: 'success' };
  }
  return { label: 'PIN Mismatch', color: 'danger' };
}

export function SetupStepTwoSecurity({
  values,
  isSaving,
  onChange,
  onBack,
  onComplete,
}: SetupStepTwoSecurityProps) {
  const { isExpanded } = useWindowLayout();
  const isKioskModeActive = useAppBootstrapStore((s) => s.isKioskModeActive);
  const setKioskModeActive = useAppBootstrapStore((s) => s.setKioskModeActive);
  const isAlwaysOnEnabled = useAppBootstrapStore((s) => s.isAlwaysOnEnabled);
  const setAlwaysOnEnabled = useAppBootstrapStore((s) => s.setAlwaysOnEnabled);

  const [pinSubstage, setPinSubstage] = useState<PinSubstage>('enter');
  const statusChip = resolvePinStatusChip(values.adminPin, values.confirmPin);

  const handleEnterPinChange = (next: string) => {
    onChange('adminPin', next);
    if (next.length === 4 && isValidPin(next) && !isWeakPin(next)) {
      setPinSubstage('confirm');
    }
  };

  const handleConfirmPinChange = (next: string) => {
    onChange('confirmPin', next);
  };

  const canSubmit =
    !isSaving &&
    values.adminUsername.trim().length > 0 &&
    isValidPin(values.adminPin) &&
    !isWeakPin(values.adminPin) &&
    values.confirmPin === values.adminPin;

  return (
    <Card>
      <Card.Header>
        <View className="flex-row items-center justify-between gap-2 flex-wrap">
          <Card.Title>2. Security & Owner Admin</Card.Title>
          <Chip size="sm" variant="soft" color={statusChip.color}>
            <Chip.Label>{statusChip.label}</Chip.Label>
          </Chip>
        </View>
        <Card.Description>{pinPolicyHint()}</Card.Description>
      </Card.Header>

      <Card.Body className="gap-4 pt-4">
        <View className={isExpanded ? 'flex-row gap-3' : 'gap-3'}>
          <View className={isExpanded ? 'flex-1' : 'w-full'}>
            <TextField>
              <Label>Admin / Owner Full Name</Label>
              <Input
                value={values.adminName}
                onChangeText={(v) => onChange('adminName', v)}
                placeholder="e.g. Maria Santos"
              />
            </TextField>
          </View>
          <View className={isExpanded ? 'flex-1' : 'w-full'}>
            <TextField>
              <Label>Admin Phone Number (Cashier Support)</Label>
              <Input
                value={values.adminPhone}
                onChangeText={(v) => onChange('adminPhone', v)}
                keyboardType="phone-pad"
                placeholder="e.g. +63 917 555 0199"
              />
            </TextField>
          </View>
        </View>

        <TextField isRequired>
          <Label>Admin Username</Label>
          <Input
            value={values.adminUsername}
            onChangeText={(v) => onChange('adminUsername', v)}
            autoCapitalize="none"
            placeholder="admin"
          />
        </TextField>

        <Surface variant="secondary" className="rounded-xl p-3.5 gap-3 border border-border/60">
          <View className="flex-row items-center justify-between gap-2 flex-wrap">
            <Text className="text-foreground text-sm font-semibold">4-Digit Master PIN</Text>
            <View className="flex-row items-center gap-2">
              <Chip
                size="sm"
                color={pinSubstage === 'enter' ? 'accent' : 'default'}
                variant={pinSubstage === 'enter' ? 'primary' : 'soft'}
                accessibilityRole="tab"
                accessibilityState={{ selected: pinSubstage === 'enter' }}
                onPress={() => setPinSubstage('enter')}
              >
                <Chip.Label>1. Enter PIN</Chip.Label>
              </Chip>
              <Chip
                size="sm"
                color={pinSubstage === 'confirm' ? 'accent' : 'default'}
                variant={pinSubstage === 'confirm' ? 'primary' : 'soft'}
                accessibilityRole="tab"
                accessibilityState={{ selected: pinSubstage === 'confirm' }}
                onPress={() => setPinSubstage('confirm')}
              >
                <Chip.Label>2. Confirm PIN</Chip.Label>
              </Chip>
            </View>
          </View>

          <PinPadEntry
            pin={pinSubstage === 'enter' ? values.adminPin : values.confirmPin}
            onPinChange={pinSubstage === 'enter' ? handleEnterPinChange : handleConfirmPinChange}
            isDisabled={isSaving}
            helperText={
              pinSubstage === 'enter'
                ? 'Tap 4 digits to set your Owner / Admin PIN'
                : 'Re-enter the same 4-digit PIN to confirm'
            }
          />
        </Surface>

        <Surface variant="secondary" className="rounded-xl p-3.5 gap-3 border border-border/60">
          <Text className="text-foreground text-sm font-semibold">
            Terminal Lockdown & Display
          </Text>
          <View className="flex-row items-center justify-between gap-3">
            <View className="flex-1 gap-0.5">
              <Text className="text-foreground text-sm font-medium">Terminal Kiosk Lockdown</Text>
              <Text className="text-muted text-xs leading-4">
                Hides Android navigation bars and blocks accidental exit gestures on counter devices.
              </Text>
            </View>
            <Switch
              isSelected={isKioskModeActive}
              onSelectedChange={setKioskModeActive}
              isDisabled={isSaving}
            />
          </View>
          <Separator />
          <View className="flex-row items-center justify-between gap-3">
            <View className="flex-1 gap-0.5">
              <Text className="text-foreground text-sm font-medium">Keep Screen Always On</Text>
              <Text className="text-muted text-xs leading-4">
                Prevents counter display timeout during active cashier shifts.
              </Text>
            </View>
            <Switch
              isSelected={isAlwaysOnEnabled}
              onSelectedChange={setAlwaysOnEnabled}
              isDisabled={isSaving}
            />
          </View>
        </Surface>
      </Card.Body>

      <Card.Footer className="flex-row items-center justify-between gap-3 pt-4">
        <TactileButton variant="secondary" onPress={onBack} isDisabled={isSaving}>
          <Button.Label>← Back</Button.Label>
        </TactileButton>
        <TactileButton
          feedbackVariant="stretch"
          variant="primary"
          containerClassName="flex-1"
          fullWidth
          isDisabled={!canSubmit}
          onPress={onComplete}
        >
          {isSaving ? <Spinner size="sm" /> : null}
          <Button.Label>{isSaving ? 'Completing Setup...' : 'Complete Store Setup'}</Button.Label>
        </TactileButton>
      </Card.Footer>
    </Card>
  );
}
