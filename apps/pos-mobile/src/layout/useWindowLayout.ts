import { useWindowDimensions } from 'react-native';
import {
  getAdaptiveDrawerWidth,
  getAdaptiveLayoutMetrics,
  type AdaptiveLayoutMetrics,
  type WindowLayout,
} from './adaptiveLayout';

export interface WindowLayoutMetrics extends Omit<AdaptiveLayoutMetrics, 'windowLayout'> {
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
  const metrics = getAdaptiveLayoutMetrics(width, height);
  const layout = metrics.windowLayout;

  return {
    width,
    height,
    layout,
    isCompact: layout === 'Compact',
    isMedium: layout === 'Medium',
    isExpanded: layout === 'Expanded',
    isLandscape: metrics.orientation === 'landscape',
    drawerWidth: getAdaptiveDrawerWidth(width),
    sizeClass: metrics.sizeClass,
    orientation: metrics.orientation,
    isShortHeight: metrics.isShortHeight,
    useSplitPane: metrics.useSplitPane,
    catalogColumns: metrics.catalogColumns,
    kpiColumns: metrics.kpiColumns,
    catalogFlex: metrics.catalogFlex,
    cartFlex: metrics.cartFlex,
    containerPaddingClass: metrics.containerPaddingClass,
    dialogMaxWidthClass: metrics.dialogMaxWidthClass,
  };
}
