import { useWindowDimensions } from 'react-native';
import {
  getAdaptiveDrawerWidth,
  getWindowLayout,
  type WindowLayout,
} from './adaptiveLayout';

export interface WindowLayoutMetrics {
  readonly width: number;
  readonly height: number;
  readonly layout: WindowLayout;
  readonly isCompact: boolean;
  readonly isMedium: boolean;
  readonly isExpanded: boolean;
  readonly isLandscape: boolean;
  readonly drawerWidth: number;
}

export function useWindowLayout(): WindowLayoutMetrics {
  const { width, height } = useWindowDimensions();
  const layout = getWindowLayout(width);

  return {
    width,
    height,
    layout,
    isCompact: layout === 'Compact',
    isMedium: layout === 'Medium',
    isExpanded: layout === 'Expanded',
    isLandscape: width > height,
    drawerWidth: getAdaptiveDrawerWidth(width),
  };
}
