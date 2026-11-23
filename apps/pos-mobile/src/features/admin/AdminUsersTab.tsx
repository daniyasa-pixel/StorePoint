import React, { useCallback, useEffect, useState } from 'react';
import { FlatList, Text, View } from 'react-native';
import { Alert, Button, Chip, Dialog, Input, Label, Surface, Switch, TextField } from 'heroui-native';
import type { UserRole } from '@/constants/routes';
import { getRepositories } from '@/db/repositories';
import type { User } from '@/db/types';
import { hashPin, isValidPin, isWeakPin, pinPolicyHint } from '@/security/securityHelper';

const ROLES: readonly UserRole[] = ['CASHIER', 'INVENTORY', 'ADMIN'];

export function AdminUsersTab() {
  const [users, setUsers] = useState<User[]>([]);
  const [showAdd, setShowAdd] = useState(false);
  const [username, setUsername] = useState('');
  const [pin, setPin] = useState('');
  const [role, setRole] = useState<UserRole>('CASHIER');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    const repos = await getRepositories();
    setUsers(await repos.users.getAllUsers());
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleSave = async () => {
    setError(null);
    if (!username.trim()) {
      setError('Username is required.');
      return;
    }
    if (!isValidPin(pin) || isWeakPin(pin)) {
      setError(pinPolicyHint());
      return;
    }
    const repos = await getRepositories();
    await repos.users.saveUser({
      username: username.trim(),
      pinHash: hashPin(pin),
      role,
      barcodeId: '',
      biometricEnrolled: false,
      pinResetRequired: false,
    });
    setUsername('');
    setPin('');
    setShowAdd(false);
    await load();
  };

  const toggleBiometric = async (user: User, next: boolean) => {
    const repos = await getRepositories();
    await repos.users.setBiometricEnrolled(user.username, next);
    await load();
  };

  return (
    <View className="flex-1 gap-3">
      <View className="flex-row justify-between items-center">
        <Text className="text-foreground text-base font-bold">Staff Accounts ({users.length})</Text>
        <Button variant="primary" size="sm" onPress={() => setShowAdd(true)}>
          <Button.Label>+ Staff</Button.Label>
        </Button>
      </View>

      <FlatList
        data={users}
        keyExtractor={(u) => u.username}
        contentContainerClassName="gap-2 pb-6"
        renderItem={({ item }) => (
          <Surface variant="default" className="rounded-xl p-3.5 border border-border flex-row items-center justify-between gap-2">
            <View className="gap-1">
              <Text className="text-foreground text-sm font-semibold">{item.username}</Text>
              <Chip size="sm" variant="soft" color={item.role === 'ADMIN' ? 'accent' : 'default'}>
                <Chip.Label>{item.role}</Chip.Label>
              </Chip>
            </View>
            <View className="flex-row items-center gap-2">
              <Text className="text-muted text-xs">Biometric</Text>
              <Switch
                isSelected={item.biometricEnrolled}
                onSelectedChange={(val) => void toggleBiometric(item, val)}
              />
            </View>
          </Surface>
        )}
      />

      <Dialog isOpen={showAdd} onOpenChange={setShowAdd}>
        <Dialog.Portal>
          <Dialog.Overlay />
          <Dialog.Content className="p-5 gap-3 max-w-md w-full self-center">
            <Dialog.Title>Create Staff Account</Dialog.Title>
            {error ? (
              <Alert status="danger">
                <Alert.Indicator />
                <Alert.Content>
                  <Alert.Description>{error}</Alert.Description>
                </Alert.Content>
              </Alert>
            ) : null}
            <TextField isRequired>
              <Label>Username</Label>
              <Input value={username} onChangeText={setUsername} autoCapitalize="none" />
            </TextField>
            <TextField isRequired>
              <Label>4-Digit PIN</Label>
              <Input
                value={pin}
                onChangeText={setPin}
                keyboardType="number-pad"
                maxLength={4}
                secureTextEntry
              />
            </TextField>
            <View className="flex-row gap-2">
              {ROLES.map((r) => (
                <Chip
                  key={r}
                  color={role === r ? 'accent' : 'default'}
                  variant={role === r ? 'primary' : 'soft'}
                  onPress={() => setRole(r)}
                >
                  <Chip.Label>{r}</Chip.Label>
                </Chip>
              ))}
            </View>
            <View className="flex-row justify-end gap-2 pt-2">
              <Button variant="secondary" onPress={() => setShowAdd(false)}>
                <Button.Label>Cancel</Button.Label>
              </Button>
              <Button variant="primary" onPress={() => void handleSave()}>
                <Button.Label>Create Account</Button.Label>
              </Button>
            </View>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog>
    </View>
  );
}
