import React, { useCallback, useEffect, useState } from 'react';
import { FlatList, Text, View } from 'react-native';
import { Button, Dialog, Input, Label, Surface, TextField } from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import type { Category } from '@/db/types';

export function AdminCategoriesTab() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [showAdd, setShowAdd] = useState(false);
  const [name, setName] = useState('');

  const load = useCallback(async () => {
    const repos = await getRepositories();
    setCategories(await repos.catalog.getCategories(false));
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleSave = async () => {
    if (!name.trim()) return;
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
  };

  return (
    <View className="flex-1 gap-3">
      <View className="flex-row justify-between items-center">
        <Text className="text-foreground text-base font-bold">Categories ({categories.length})</Text>
        <Button variant="primary" size="sm" onPress={() => setShowAdd(true)}>
          <Button.Label>+ Category</Button.Label>
        </Button>
      </View>

      <FlatList
        data={categories}
        keyExtractor={(c) => String(c.id)}
        contentContainerClassName="gap-2 pb-6"
        renderItem={({ item }) => (
          <Surface variant="default" className="rounded-xl p-3.5 border border-border">
            <Text className="text-foreground text-sm font-semibold">{item.name}</Text>
          </Surface>
        )}
      />

      <Dialog isOpen={showAdd} onOpenChange={setShowAdd}>
        <Dialog.Portal>
          <Dialog.Overlay />
          <Dialog.Content className="p-5 gap-3 max-w-md w-full self-center">
            <Dialog.Title>New Category</Dialog.Title>
            <TextField isRequired>
              <Label>Category Name</Label>
              <Input value={name} onChangeText={setName} placeholder="e.g. Beverages" />
            </TextField>
            <View className="flex-row justify-end gap-2 pt-2">
              <Button variant="secondary" onPress={() => setShowAdd(false)}>
                <Button.Label>Cancel</Button.Label>
              </Button>
              <Button variant="primary" onPress={() => void handleSave()}>
                <Button.Label>Create</Button.Label>
              </Button>
            </View>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog>
    </View>
  );
}
