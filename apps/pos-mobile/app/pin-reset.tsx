import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView } from 'react-native';
import Animated from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Button, Card, Chip, Input, Label, Spinner, TextField } from 'heroui-native';
import { AppTopBar } from '@/components/AppTopBar';
import { PinPadEntry } from '@/components/PinPadEntry';
import { getRepositories } from '@/db/repositories';
import { hashPin, isValidPin, isWeakPin, pinPolicyHint, verifyPin } from '@/security/securityHelper';
import { enterSlideRight, exitSlideLeft } from '@/theme/motion';

type ResetStep = 1 | 2 | 3;

export default function PinResetRoute() {
  const router = useRouter();
  const [step, setStep] = useState<ResetStep>(1);
  const [username, setUsername] = useState('admin');
  const [currentPin, setCurrentPin] = useState('');
  const [newPin, setNewPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleAdvance = async () => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    try {
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
    } finally {
      setIsSubmitting(false);
    }
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

      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        className="flex-1"
      >
        <ScrollView
          contentContainerClassName="flex-grow justify-center p-5"
          keyboardShouldPersistTaps="handled"
        >
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
            <Card.Body className="pt-4">
              <Animated.View
                key={step}
                entering={enterSlideRight}
                exiting={exitSlideLeft}
                className="gap-4"
              >
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
                  isDisabled={isSubmitting}
                  errorMessage={error}
                />
              </Animated.View>
            </Card.Body>
            <Card.Footer className="flex-row justify-between gap-2 pt-4">
              {step > 1 ? (
                <Button
                  variant="secondary"
                  isDisabled={isSubmitting}
                  onPress={() => setStep((s) => (s - 1) as ResetStep)}
                >
                  <Button.Label>Back</Button.Label>
                </Button>
              ) : null}
              <Button
                variant="primary"
                className="flex-1"
                isDisabled={isSubmitting || activePin.length < 4}
                onPress={handleAdvance}
              >
                {isSubmitting ? <Spinner size="sm" /> : null}
                <Button.Label>{step === 3 ? 'Save New PIN' : 'Continue'}</Button.Label>
              </Button>
            </Card.Footer>
          </Card>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
