type MockThemeMode = 'system' | 'light' | 'dark';

let activeTheme: 'light' | 'dark' = 'light';
let hasAdaptiveThemes = true;

export const Uniwind = {
  setTheme: jest.fn((mode: MockThemeMode) => {
    if (mode === 'system') {
      hasAdaptiveThemes = true;
      activeTheme = 'light';
    } else {
      hasAdaptiveThemes = false;
      activeTheme = mode;
    }
  }),
};

export const useUniwind = jest.fn(() => ({
  theme: activeTheme,
  hasAdaptiveThemes,
}));
