import React from 'react';
import { Redirect } from 'expo-router';
import { APP_ROUTES, resolveHomeRouteForRole } from '@/constants/routes';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

export default function IndexGate() {
  const isOnboardingCompleted = useAppBootstrapStore((s) => s.isOnboardingCompleted);
  const isSetupCompleted = useAppBootstrapStore((s) => s.isSetupCompleted);
  const activeUser = useAppBootstrapStore((s) => s.activeUser);

  if (!isOnboardingCompleted) {
    return <Redirect href={APP_ROUTES.ONBOARDING} />;
  }

  if (!isSetupCompleted) {
    return <Redirect href={APP_ROUTES.SETUP} />;
  }

  if (activeUser) {
    return <Redirect href={resolveHomeRouteForRole(activeUser.role)} />;
  }

  return <Redirect href={APP_ROUTES.LOGIN} />;
}
