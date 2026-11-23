import React from 'react';
import { View } from 'react-native';
import { Button, Card } from 'heroui-native';

interface InventoryQuickShareTabProps {
  readonly onExportCatalog?: () => void;
}

export function InventoryQuickShareTab({ onExportCatalog }: InventoryQuickShareTabProps) {
  return (
    <View className="flex-1 gap-3">
      <Card className="p-5 gap-3">
        <Card.Title>Offline Quick Share Hub</Card.Title>
        <Card.Description>
          Export your full StorePoint SQLite catalog and audit snapshot as a portable JSON file for
          Android Nearby / Quick Share transfer to another counter terminal.
        </Card.Description>
        <Button variant="primary" onPress={onExportCatalog} isDisabled={!onExportCatalog}>
          <Button.Label>Export & Share Snapshot</Button.Label>
        </Button>
      </Card>
    </View>
  );
}
