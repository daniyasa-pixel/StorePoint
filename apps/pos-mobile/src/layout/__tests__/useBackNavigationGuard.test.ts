jest.mock('react-native', () => ({
  BackHandler: {
    addEventListener: jest.fn(),
    exitApp: jest.fn(),
  },
  Platform: { OS: 'android' },
}));

jest.mock('heroui-native', () => ({
  useToast: jest.fn(() => ({ toast: { show: jest.fn() } })),
}));

import {
  DOUBLE_BACK_EXIT_WINDOW_MS,
  evaluateBackPress,
} from '../useBackNavigationGuard';

describe('evaluateBackPress', () => {
  it('short-circuits with custom action when onBack returns true', () => {
    // Arrange
    const onBack = jest.fn(() => true);

    // Act
    const result = evaluateBackPress({
      onBack,
      isKioskModeActive: true,
      blockExit: true,
      requireDoubleBackToExit: true,
      lastBackPressAt: 500,
      now: 1_000,
    });

    // Assert
    expect(onBack).toHaveBeenCalledTimes(1);
    expect(result).toEqual({
      decision: { handled: true, action: 'custom' },
      nextLastBackPressAt: 500,
    });
  });

  it('blocks exit with kiosk-blocked when kiosk mode is active and onBack returns false', () => {
    // Arrange
    const onBack = jest.fn(() => false);

    // Act
    const result = evaluateBackPress({
      onBack,
      isKioskModeActive: true,
      blockExit: false,
      requireDoubleBackToExit: true,
      lastBackPressAt: 1_000,
      now: 1_500,
    });

    // Assert
    expect(result).toEqual({
      decision: { handled: true, action: 'kiosk-blocked' },
      nextLastBackPressAt: 0,
    });
  });

  it('blocks exit silently when blockExit is true outside kiosk mode', () => {
    // Act
    const result = evaluateBackPress({
      isKioskModeActive: false,
      blockExit: true,
      requireDoubleBackToExit: true,
      lastBackPressAt: 0,
      now: 2_000,
    });

    // Assert
    expect(result).toEqual({
      decision: { handled: true, action: 'blocked' },
      nextLastBackPressAt: 0,
    });
  });

  it('prompts on first swipe and exits only if swiped again within 2000ms', () => {
    // Act: first press
    const first = evaluateBackPress({
      isKioskModeActive: false,
      blockExit: false,
      requireDoubleBackToExit: true,
      lastBackPressAt: 0,
      now: 10_000,
    });

    // Assert: first press prompts
    expect(first).toEqual({
      decision: { handled: true, action: 'prompt-double-back' },
      nextLastBackPressAt: 10_000,
    });

    // Act: second press after window expired (>= 2000ms)
    const expired = evaluateBackPress({
      isKioskModeActive: false,
      blockExit: false,
      requireDoubleBackToExit: true,
      lastBackPressAt: first.nextLastBackPressAt,
      now: 10_000 + DOUBLE_BACK_EXIT_WINDOW_MS,
    });

    // Assert: re-prompts instead of exiting
    expect(expired).toEqual({
      decision: { handled: true, action: 'prompt-double-back' },
      nextLastBackPressAt: 12_000,
    });

    // Act: second press within 2000ms
    const confirmed = evaluateBackPress({
      isKioskModeActive: false,
      blockExit: false,
      requireDoubleBackToExit: true,
      lastBackPressAt: expired.nextLastBackPressAt,
      now: 13_200,
    });

    // Assert: exits app and resets timestamp
    expect(confirmed).toEqual({
      decision: { handled: true, action: 'exit-app' },
      nextLastBackPressAt: 0,
    });
  });

  it('passes through unhandled when no guard options apply', () => {
    // Act
    const result = evaluateBackPress({
      isKioskModeActive: false,
      blockExit: false,
      requireDoubleBackToExit: false,
      lastBackPressAt: 0,
      now: 5_000,
    });

    // Assert
    expect(result).toEqual({
      decision: { handled: false, action: 'passthrough' },
      nextLastBackPressAt: 0,
    });
  });
});
