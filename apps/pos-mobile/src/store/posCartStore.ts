import { create } from 'zustand';
import type { CartItemDetails, Product, UomOption } from '@/db/types';
import type { DiscountType } from '@/domain/posCheckoutCoordinator';
import { roundMoney } from '@/domain/money';
import { sessionStateCache } from '@/storage/sessionStateCache';

export interface PosCartLine extends CartItemDetails {
  readonly checked: boolean;
  readonly itemDiscount: number;
}

interface PosCartState {
  readonly lines: readonly PosCartLine[];
  readonly orderDiscountType: DiscountType;
  readonly orderDiscountValue: number;
  readonly restoredNoticeCount: number;

  addProduct: (product: Product, uom?: UomOption, qty?: number) => void;
  updateQuantity: (productId: number, uomName: string, delta: number) => void;
  toggleChecked: (productId: number, uomName: string) => void;
  setItemDiscount: (productId: number, uomName: string, discount: number) => void;
  setOrderDiscount: (type: DiscountType, value: number) => void;
  removeLine: (productId: number, uomName: string) => void;
  clearCart: () => void;
  clearCheckedLines: () => void;
  restoreFromDraft: (products: readonly Product[]) => Promise<void>;
  acknowledgeRestoreNotice: () => void;
}

const PERSIST_DEBOUNCE_MS = 250;
let persistTimer: ReturnType<typeof setTimeout> | null = null;

function flushPersistLines(lines: readonly PosCartLine[]): void {
  if (persistTimer !== null) {
    clearTimeout(persistTimer);
    persistTimer = null;
  }
  void sessionStateCache.saveCartDraft(
    lines.map((l) => ({
      productId: l.product.id,
      quantity: l.quantity,
      uomName: l.uomName,
      multiplier: l.multiplier,
      selectedPrice: l.selectedPrice,
      discount: l.itemDiscount,
    })),
  );
}

function persistLines(lines: readonly PosCartLine[]): void {
  if (lines.length === 0) {
    flushPersistLines(lines);
    return;
  }
  if (persistTimer !== null) {
    clearTimeout(persistTimer);
  }
  persistTimer = setTimeout(() => {
    persistTimer = null;
    flushPersistLines(lines);
  }, PERSIST_DEBOUNCE_MS);
}

export const usePosCartStore = create<PosCartState>((set, get) => ({
  lines: [],
  orderDiscountType: 'NONE',
  orderDiscountValue: 0,
  restoredNoticeCount: 0,

  addProduct: (product, uom, qty = 1) => {
    const unit: UomOption = uom ?? {
      name: product.baseUom || 'pc',
      multiplier: 1,
      price: product.price,
    };
    const existing = get().lines;
    const idx = existing.findIndex(
      (l) => l.product.id === product.id && l.uomName === unit.name,
    );
    const nextLines: PosCartLine[] =
      idx >= 0
        ? existing.map((l, i) =>
            i === idx ? { ...l, quantity: l.quantity + qty } : l,
          )
        : [
            ...existing,
            {
              product,
              quantity: qty,
              uomName: unit.name,
              multiplier: unit.multiplier,
              selectedPrice: unit.price,
              checked: true,
              itemDiscount: 0,
            },
          ];
    set({ lines: nextLines });
    persistLines(nextLines);
  },

  updateQuantity: (productId, uomName, delta) => {
    const nextLines = get()
      .lines.map((l) =>
        l.product.id === productId && l.uomName === uomName
          ? { ...l, quantity: l.quantity + delta }
          : l,
      )
      .filter((l) => l.quantity > 0);
    set({ lines: nextLines });
    persistLines(nextLines);
  },

  toggleChecked: (productId, uomName) => {
    set({
      lines: get().lines.map((l) =>
        l.product.id === productId && l.uomName === uomName
          ? { ...l, checked: !l.checked }
          : l,
      ),
    });
  },

  setItemDiscount: (productId, uomName, discount) => {
    const safe = roundMoney(Math.max(0, discount));
    const nextLines = get().lines.map((l) =>
      l.product.id === productId && l.uomName === uomName
        ? { ...l, itemDiscount: Math.min(safe, l.selectedPrice) }
        : l,
    );
    set({ lines: nextLines });
    persistLines(nextLines);
  },

  setOrderDiscount: (type, value) =>
    set({ orderDiscountType: type, orderDiscountValue: value }),

  removeLine: (productId, uomName) => {
    const nextLines = get().lines.filter(
      (l) => !(l.product.id === productId && l.uomName === uomName),
    );
    set({ lines: nextLines });
    persistLines(nextLines);
  },

  clearCart: () => {
    if (persistTimer !== null) {
      clearTimeout(persistTimer);
      persistTimer = null;
    }
    set({ lines: [], orderDiscountType: 'NONE', orderDiscountValue: 0 });
    void sessionStateCache.clearCartDraft();
  },

  clearCheckedLines: () => {
    const nextLines = get().lines.filter((l) => !l.checked);
    set({ lines: nextLines, orderDiscountType: 'NONE', orderDiscountValue: 0 });
    persistLines(nextLines);
  },

  restoreFromDraft: async (products) => {
    if (get().lines.length > 0) return;
    const draft = await sessionStateCache.loadCartDraft();
    if (draft.length === 0) return;

    const byId = new Map(products.map((p) => [p.id, p]));
    const restored: PosCartLine[] = [];
    for (const d of draft) {
      const live = byId.get(d.productId);
      if (!live) continue;
      if (live.trackInventory && live.stockCount < d.quantity * d.multiplier) continue;
      restored.push({
        product: live,
        quantity: d.quantity,
        uomName: d.uomName,
        multiplier: d.multiplier,
        selectedPrice: d.selectedPrice,
        checked: true,
        itemDiscount: d.discount,
      });
    }
    if (restored.length > 0) {
      set({ lines: restored, restoredNoticeCount: restored.length });
    }
  },

  acknowledgeRestoreNotice: () => set({ restoredNoticeCount: 0 }),
}));
