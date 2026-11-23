import React, { useRef } from 'react';
import { Text, View } from 'react-native';
import { CameraView, useCameraPermissions, type BarcodeScanningResult } from 'expo-camera';
import { Button, Dialog } from 'heroui-native';

interface BarcodeScannerModalProps {
  readonly isOpen: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly onBarcodeScanned: (barcode: string) => void;
}

const DEBOUNCE_MS = 1200;

export function BarcodeScannerModal({
  isOpen,
  onOpenChange,
  onBarcodeScanned,
}: BarcodeScannerModalProps) {
  const [permission, requestPermission] = useCameraPermissions();
  const lastScanRef = useRef<{ code: string; at: number }>({ code: '', at: 0 });

  const handleScan = (result: BarcodeScanningResult) => {
    const code = result.data.trim();
    if (!code) return;
    const now = Date.now();
    if (
      lastScanRef.current.code === code &&
      now - lastScanRef.current.at < DEBOUNCE_MS
    ) {
      return;
    }
    lastScanRef.current = { code, at: now };
    onBarcodeScanned(code);
    onOpenChange(false);
  };

  return (
    <Dialog isOpen={isOpen} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className="p-4 gap-3 max-w-md w-full self-center">
          <Dialog.Title>Scan Barcode</Dialog.Title>
          {!permission?.granted ? (
            <View className="gap-3 py-4 items-center">
              <Text className="text-muted text-sm text-center">
                Camera permission is required to scan product and staff barcodes.
              </Text>
              <Button variant="primary" onPress={() => void requestPermission()}>
                <Button.Label>Grant Camera Permission</Button.Label>
              </Button>
            </View>
          ) : (
            <View className="h-64 w-full overflow-hidden rounded-xl bg-black">
              <CameraView
                style={{ flex: 1 }}
                facing="back"
                barcodeScannerSettings={{
                  barcodeTypes: ['ean13', 'ean8', 'upc_a', 'upc_e', 'code128', 'code39', 'qr'],
                }}
                onBarcodeScanned={handleScan}
              />
            </View>
          )}
          <Button variant="secondary" onPress={() => onOpenChange(false)}>
            <Button.Label>Close Scanner</Button.Label>
          </Button>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
