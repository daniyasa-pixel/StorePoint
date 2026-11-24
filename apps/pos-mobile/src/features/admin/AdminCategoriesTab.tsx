import React, { useCallback, useEffect, useState } from 'react';
import { FlatList, ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Alert, Button, Card, Dialog, Input, Label, Separator, Surface, TextField } from 'heroui-native';
import { TactileButton } from '@/components/TactilePressable';
import { getRepositories } from '@/db/repositories';
import type { Category } from '@/db/types';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { layoutTransition, staggerFadeUp } from '@/theme/motion';

export function AdminCategoriesTab() {
  const { containerPaddingClass, dialogMaxWidthClass } = useWindowLayout();
  const [categories, setCategories] = useState<Category[]>([]);
  const [showAdd, setShowAdd] = useState(false);
  const [name, setName] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    const repos = await getRepositories();
    setCategories(await repos.catalog.getCategories(false));
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleSave = async () => {
    if (isSaving) return;
    setError(null);
    if (!name.trim()) {
      setError('Category name is required.');
      return;
    }
    setIsSaving(true);
    try {
      const repos = await getRepositories();
      await repos.catalog.saveCategory({
        name: name.trim(),
        colorAccent: '#006FEE',
        iconBadge: 'tag',
        isArchived: false,
      });
      setName('');
      setShowAdd(false);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create category.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <View className={`flex-1 gap-3 ${containerPaddingClass}`}>
      <View className="flex-row justify-between items-center">
        <Text className="text-foreground text-base font-bold">Categories ({categories.length})</Text>
        <TactileButton
          feedbackVariant="stretch"
          variant="primary"
          size="sm"
          onPress={() => setShowAdd(true)}
        >
          <Button.Label>+ Category</Button.Label>
        </TactileButton>
      </View>

      <FlatList
        data={categories}
        keyExtractor={(c) => String(c.id)}
        contentContainerClassName="gap-2 pb-6"
        ListEmptyComponent={
          <Card className="p-6 items-center">
            <Card.Title>No categories yet</Card.Title>
            <Card.Description>Tap + Category to organize your catalog.</Card.Description>
          </Card>
        }
        renderItem={({ item, index }) => (
          <Animated.View entering={staggerFadeUp(index)} layout={layoutTransition}>
            <Surface variant="default" className="rounded-xl p-3.5 border border-border">
              <View className="min-w-0">
                <Text numberOfLines={1} className="text-foreground text-sm font-semibold">{item.name}</Text>
              </View>
            </Surface>
          </Animated.View>
        )}
      />

      <Dialog isOpen={showAdd} onOpenChange={setShowAdd}>
        <Dialog.Portal>
          <Dialog.Overlay />
          <Dialog.Content className={`p-5 gap-3 ${dialogMaxWidthClass} w-full self-center mx-4 max-h-[90%]`}>
            <Dialog.Title>New Category</Dialog.Title>
            <ScrollView keyboardShouldPersistTaps="handled" contentContainerClassName="gap-3">
              {error ? (
                <Alert status="danger">
                  <Alert.Indicator />
                  <Alert.Content>
                    <Alert.Description>{error}</Alert.Description>
                  </Alert.Content>
                </Alert>
              ) : null}
              <TextField isRequired>
                <Label>Category Name</Label>
                <Input value={name} onChangeText={setName} placeholder="e.g. Beverages" />
              </TextField>
            </ScrollView>
            <Separator />
            <View className="flex-row justify-end gap-2">
              <Button variant="secondary" isDisabled={isSaving} onPress={() => setShowAdd(false)}>
                <Button.Label>Cancel</Button.Label>
              </Button>
              <TactileButton
                feedbackVariant="stretch"
                variant="primary"
                isDisabled={isSaving}
                onPress={() => void handleSave()}
              >
                <Button.Label>{isSaving ? 'Creating...' : 'Create'}</Button.Label>
              </TactileButton>
            </View>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog>
    </View>
  );
}
