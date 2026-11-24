import {
  FadeIn,
  FadeInDown,
  FadeInRight,
  FadeInUp,
  FadeOut,
  FadeOutLeft,
  FadeOutUp,
  LinearTransition,
  ZoomIn,
} from 'react-native-reanimated';

/**
 * HeroUI v3 / StorePoint POS motion language.
 * Standardized, tactile, and fast (180–260ms) animations tuned for POS terminals
 * and mobile screens using react-native-reanimated.
 */
export const MOTION_DURATIONS = {
  fast: 150,
  normal: 220,
  gentle: 280,
} as const;

export const MOTION_SPRINGS = {
  snappy: { damping: 20, stiffness: 280, mass: 0.8 },
  gentle: { damping: 22, stiffness: 200, mass: 0.9 },
  bouncy: { damping: 14, stiffness: 320, mass: 0.7 },
} as const;

export const layoutTransition = LinearTransition.springify()
  .damping(22)
  .stiffness(240);

export const enterFade = FadeIn.duration(MOTION_DURATIONS.normal);
export const exitFade = FadeOut.duration(MOTION_DURATIONS.fast);

export const enterFadeUp = FadeInUp.duration(MOTION_DURATIONS.normal).springify().damping(22).stiffness(240);
export const exitFadeUp = FadeOutUp.duration(MOTION_DURATIONS.fast);

export const enterFadeDown = FadeInDown.duration(MOTION_DURATIONS.normal).springify().damping(22).stiffness(240);

export const enterSlideRight = FadeInRight.duration(MOTION_DURATIONS.normal).springify().damping(22).stiffness(240);
export const exitSlideLeft = FadeOutLeft.duration(MOTION_DURATIONS.fast);

export const enterPop = ZoomIn.duration(MOTION_DURATIONS.fast).springify().damping(16).stiffness(300);

export function staggerFadeUp(index: number, baseDelay = 35) {
  return FadeInUp.delay(Math.min(index, 8) * baseDelay)
    .duration(MOTION_DURATIONS.normal)
    .springify()
    .damping(22)
    .stiffness(240);
}
