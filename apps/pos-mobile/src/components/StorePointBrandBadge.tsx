import React from 'react';
import { Avatar } from 'heroui-native';

interface StorePointBrandBadgeProps {
  readonly size?: 'sm' | 'md' | 'lg';
}

const SIZE_CLASSES: Record<NonNullable<StorePointBrandBadgeProps['size']>, { box: string; text: string }> = {
  sm: { box: 'h-9 w-9 rounded-lg', text: 'text-sm' },
  md: { box: 'h-11 w-11 rounded-xl', text: 'text-base' },
  lg: { box: 'h-14 w-14 rounded-2xl', text: 'text-xl' },
};

export function StorePointBrandBadge({ size = 'sm' }: StorePointBrandBadgeProps) {
  const styles = SIZE_CLASSES[size];

  return (
    <Avatar
      alt="StorePoint"
      size={size}
      color="accent"
      className={`bg-accent ${styles.box}`}
    >
      <Avatar.Fallback className="bg-accent" textProps={{ className: `text-accent-foreground font-bold tracking-tight ${styles.text}` }}>
        SP
      </Avatar.Fallback>
    </Avatar>
  );
}
