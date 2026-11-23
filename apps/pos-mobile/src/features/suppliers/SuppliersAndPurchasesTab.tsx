import React, { useCallback, useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { Button, Card, Chip, Dialog, Input, Label, Surface, TextField } from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import type { PurchaseOrder, Supplier } from '@/db/types';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

export function SuppliersAndPurchasesTab() {
  const currency = useAppBootstrapStore((s) => s.currencySymbol);
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [orders, setOrders] = useState<PurchaseOrder[]>([]);
  const [showAddSupplier, setShowAddSupplier] = useState(false);

  const [name, setName] = useState('');
  const [contact, setContact] = useState('');
  const [phone, setPhone] = useState('');
  const [terms, setTerms] = useState('Cash on Delivery');

  const load = useCallback(async () => {
    const repos = await getRepositories();
    const [sup, pos] = await Promise.all([
      repos.purchasing.getSuppliers(),
      repos.purchasing.getPurchaseOrders(),
    ]);
    setSuppliers(sup);
    setOrders(pos);
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleSaveSupplier = async () => {
    if (!name.trim()) return;
    const repos = await getRepositories();
    await repos.purchasing.saveSupplier({
      name: name.trim(),
      contactPerson: contact.trim(),
      phone: phone.trim(),
      email: '',
      address: '',
      deliverySchedule: '',
      paymentTerms: terms.trim() || 'Cash on Delivery',
      notes: '',
    });
    setName('');
    setContact('');
    setPhone('');
    setShowAddSupplier(false);
    await load();
  };

  const handleReceivePo = async (po: PurchaseOrder) => {
    const repos = await getRepositories();
    await repos.purchasing.receivePurchaseOrder(po.id, activeUser?.username ?? 'admin', true);
    await load();
  };

  return (
    <ScrollView contentContainerClassName="gap-4 pb-6">
      <Card className="p-4 gap-3">
        <View className="flex-row justify-between items-center">
          <Card.Title>Suppliers ({suppliers.length})</Card.Title>
          <Button variant="primary" size="sm" onPress={() => setShowAddSupplier(true)}>
            <Button.Label>+ Supplier</Button.Label>
          </Button>
        </View>

        {suppliers.length === 0 ? (
          <Text className="text-muted text-xs">No suppliers added yet.</Text>
        ) : (
          suppliers.map((sup) => (
            <Surface key={sup.id} variant="secondary" className="rounded-xl p-3 gap-0.5">
              <Text className="text-foreground text-sm font-semibold">{sup.name}</Text>
              <Text className="text-muted text-xs">
                {sup.contactPerson || 'No contact'} • {sup.phone || 'No phone'} • {sup.paymentTerms}
              </Text>
            </Surface>
          ))
        )}
      </Card>

      <Card className="p-4 gap-3">
        <Card.Title>Purchase Orders ({orders.length})</Card.Title>
        {orders.length === 0 ? (
          <Text className="text-muted text-xs">No purchase orders recorded yet.</Text>
        ) : (
          orders.map((po) => (
            <Surface
              key={po.id}
              variant="secondary"
              className="rounded-xl p-3 flex-row items-center justify-between gap-2"
            >
              <View className="flex-1">
                <Text className="text-foreground text-sm font-semibold">
                  {po.poNumber} • {po.supplierName}
                </Text>
                <Text className="text-muted text-xs">
                  {currency}
                  {po.totalCost.toFixed(2)} • {po.paymentStatus}
                </Text>
              </View>
              {po.status === 'ORDERED' ? (
                <Button variant="primary" size="sm" onPress={() => void handleReceivePo(po)}>
                  <Button.Label>Receive</Button.Label>
                </Button>
              ) : (
                <Chip size="sm" variant="soft" color={po.status === 'RECEIVED' ? 'success' : 'danger'}>
                  <Chip.Label>{po.status}</Chip.Label>
                </Chip>
              )}
            </Surface>
          ))
        )}
      </Card>

      <Dialog isOpen={showAddSupplier} onOpenChange={setShowAddSupplier}>
        <Dialog.Portal>
          <Dialog.Overlay />
          <Dialog.Content className="p-5 gap-3 max-w-md w-full self-center">
            <Dialog.Title>New Supplier</Dialog.Title>
            <TextField isRequired>
              <Label>Supplier Name</Label>
              <Input value={name} onChangeText={setName} placeholder="e.g. Metro Beverage Distributors" />
            </TextField>
            <TextField>
              <Label>Contact Person</Label>
              <Input value={contact} onChangeText={setContact} />
            </TextField>
            <TextField>
              <Label>Phone</Label>
              <Input value={phone} onChangeText={setPhone} keyboardType="phone-pad" />
            </TextField>
            <TextField>
              <Label>Payment Terms</Label>
              <Input value={terms} onChangeText={setTerms} />
            </TextField>
            <View className="flex-row justify-end gap-2 pt-2">
              <Button variant="secondary" onPress={() => setShowAddSupplier(false)}>
                <Button.Label>Cancel</Button.Label>
              </Button>
              <Button variant="primary" onPress={() => void handleSaveSupplier()}>
                <Button.Label>Save Supplier</Button.Label>
              </Button>
            </View>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog>
    </ScrollView>
  );
}
