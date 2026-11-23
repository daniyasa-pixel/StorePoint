import React, { useState } from 'react';
import { ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Button, Card, Chip, Input, Label, TextField } from 'heroui-native';
import { AppTopBar } from '@/components/AppTopBar';
import { PinPadEntry } from '@/components/PinPadEntry';
import { getRepositories } from '@/db/repositories';
import { hashPin, isValidPin, isWeakPin, pinPolicyHint, verifyPin } from '@/security/securityHelper';

type ResetStep = 1 | 2 | 3;

export default function PinResetRoute() {
  const router = useRouter();
  const [step, setStep] = useState<ResetStep>(1);
  const [username, setUsername] = useState('admin');
  const [currentPin, setCurrentPin] = useState('');
  const [newPin, setNewPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleAdvance = async () => {
    setError(null);
    const repos = await getRepositories();

    if (step === 1) {
      const user = await repos.users.getByUsername(username.trim());
      if (!user || !verifyPin(currentPin, user.pinHash).isMatch) {
        setError('Current PIN verification failed.');
        setCurrentPin('');
        return;
      }
      setStep(2);
      return;
    }

    if (step === 2) {
      if (!isValidPin(newPin) || isWeakPin(newPin)) {
        setError(`Choose a stronger PIN. ${pinPolicyHint()}`);
        return;
      }
      setStep(3);
      return;
    }

    if (newPin !== confirmPin) {
      setError('PIN confirmation does not match.');
      setConfirmPin('');
      return;
    }

    await repos.users.updatePinHash(username.trim(), hashPin(newPin), true);
    router.back();
  };

  const activePin = step === 1 ? currentPin : step === 2 ? newPin : confirmPin;
  const setActivePin = step === 1 ? setCurrentPin : step === 2 ? setNewPin : setConfirmPin;

  return (
    <SafeAreaView className="bg-background flex-1">
      <AppTopBar
        title="4-Digit PIN Migration"
        subtitle={`Step ${step} of 3`}
        onBack={() => router.back()}
        rightActions={
          <Chip color="accent" variant="soft" size="sm">
            <Chip.Label>{step}/3</Chip.Label>
          </Chip>
        }
      />

      <ScrollView contentContainerClassName="flex-grow justify-center p-5">
        <Card className="w-full max-w-md self-center">
          <Card.Header>
            <Card.Title>
              {step === 1
                ? 'Verify Existing Credential'
                : step === 2
                  ? 'Choose New 4-Digit PIN'
                  : 'Confirm New 4-Digit PIN'}
            </Card.Title>
            <Card.Description>
              {step === 1
                ? 'Enter your username and current PIN to prove ownership.'
                : pinPolicyHint()}
            </Card.Description>
          </Card.Header>
          <Card.Body className="gap-4 pt-4">
            {step === 1 ? (
              <TextField>
                <Label>Username</Label>
                <Input value={username} onChangeText={setUsername} autoCapitalize="none" />
              </TextField>
            ) : null}

            <PinPadEntry
              pin={activePin}
              onPinChange={(v) => {
                setActivePin(v);
                setError(null);
              }}
              errorMessage={error}
            />
          </Card.Body>
          <Card.Footer className="flex-row justify-between gap-2 pt-4">
            {step > 1 ? (
              <Button variant="secondary" onPress={() => setStep((s) => (s - 1) as ResetStep)}>
                <Button.Label>Back</Button.Label>
              </Button>
            ) : (
              <View />
            )}
            <Button
              variant="primary"
              className="flex-1"
              isDisabled={activePin.length < 4}
              onPress={handleAdvance}
            >
              <Button.Label>{step === 3 ? 'Save New PIN' : 'Continue'}</Button.Label>
            </Button>
          </Card.Footer>
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}
