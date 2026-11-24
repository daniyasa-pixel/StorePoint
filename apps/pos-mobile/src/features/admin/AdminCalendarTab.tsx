import React, { useCallback, useEffect, useState } from 'react';
import { FlatList, ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Button, Card, Chip, Dialog, Input, Label, Separator, Surface, TextField } from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import type { PaymentSchedule } from '@/db/types';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { layoutTransition, staggerFadeUp } from '@/theme/motion';

export function AdminCalendarTab() {
  const currency = useAppBootstrapStore((s) => s.currencySymbol);
  const [schedules, setSchedules] = useState<PaymentSchedule[]>([]);
  const [showAdd, setShowAdd] = useState(false);
  const [title, setTitle] = useState('');
  const [amount, setAmount] = useState('');
  const [dueDate, setDueDate] = useState('2026-12-01');

  const load = useCallback(async () => {
    const repos = await getRepositories();
    setSchedules(await repos.schedules.getAll());
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleSave = async () => {
    if (!title.trim()) return;
    const repos = await getRepositories();
    await repos.schedules.save({
      title: title.trim(),
      amount: Number.parseFloat(amount) || 0,
      dueDate: dueDate.trim(),
      category: 'Supplier',
      priority: 'High',
      isPaid: false,
      paymentMethod: 'CASH',
    });
    setTitle('');
    setAmount('');
    setShowAdd(false);
    await load();
  };

  const togglePaid = async (item: PaymentSchedule) => {
    const repos = await getRepositories();
    await repos.schedules.setPaid(item.id, !item.isPaid);
    await load();
  };

  return (
    <View className="flex-1 gap-3">
      <View className="flex-row justify-between items-center">
        <Text className="text-foreground text-base font-bold">Payment Calendar</Text>
        <Button variant="primary" size="sm" onPress={() => setShowAdd(true)}>
          <Button.Label>+ Schedule Bill</Button.Label>
        </Button>
      </View>

      <FlatList
        data={schedules}
        keyExtractor={(s) => String(s.id)}
        contentContainerClassName="gap-2 pb-6"
        ListEmptyComponent={
          <Card className="p-6 items-center">
            <Card.Title>No scheduled payments</Card.Title>
            <Card.Description>Tap + Schedule Bill to track upcoming payables.</Card.Description>
          </Card>
        }
        renderItem={({ item, index }) => (
          <Animated.View entering={staggerFadeUp(index)} layout={layoutTransition}>
            <Surface variant="default" className="rounded-xl p-3.5 border border-border flex-row items-center justify-between gap-2">
              <View className="flex-1 min-w-0">
                <Text numberOfLines={1} className="text-foreground text-sm font-semibold">{item.title}</Text>
                <Text numberOfLines={1} className="text-muted text-xs">
                  Due {item.dueDate} • {currency}
                  {item.amount.toFixed(2)}
                </Text>
              </View>
              <Chip
                size="sm"
                variant="soft"
                color={item.isPaid ? 'success' : 'warning'}
                onPress={() => void togglePaid(item)}
              >
                <Chip.Label>{item.isPaid ? 'Paid' : 'Mark Paid'}</Chip.Label>
              </Chip>
            </Surface>
          </Animated.View>
        )}
      />

      <Dialog isOpen={showAdd} onOpenChange={setShowAdd}>
        <Dialog.Portal>
          <Dialog.Overlay />
          <Dialog.Content className="p-5 gap-3 max-w-md w-full self-center mx-4 max-h-[90%]">
            <Dialog.Title>Schedule Payment</Dialog.Title>
            <ScrollView keyboardShouldPersistTaps="handled" contentContainerClassName="gap-3">
              <TextField isRequired>
                <Label>Title</Label>
                <Input value={title} onChangeText={setTitle} placeholder="e.g. Electric Bill / Rent" />
              </TextField>
              <TextField>
                <Label>Amount ({currency})</Label>
                <Input value={amount} onChangeText={setAmount} keyboardType="decimal-pad" />
              </TextField>
              <TextField>
                <Label>Due Date (YYYY-MM-DD)</Label>
                <Input value={dueDate} onChangeText={setDueDate} />
              </TextField>
            </ScrollView>
            <Separator />
            <View className="flex-row justify-end gap-2">
              <Button variant="secondary" onPress={() => setShowAdd(false)}>
                <Button.Label>Cancel</Button.Label>
              </Button>
              <Button variant="primary" onPress={() => void handleSave()}>
                <Button.Label>Save</Button.Label>
              </Button>
            </View>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog>
    </View>
  );
}
