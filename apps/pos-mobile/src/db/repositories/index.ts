import { getDatabase } from '@/db/database';
import type { SqlExecutor } from '@/db/types';
import { CatalogRepository } from './catalogRepository';
import { CheckoutRepository } from './checkoutRepository';
import { ProductionRepository } from './productionRepository';
import { PurchasingRepository } from './purchasingRepository';
import { ReturnRepository } from './returnRepository';
import { ScheduleRepository } from './scheduleRepository';
import { ShiftRepository } from './shiftRepository';
import { StoreConfigRepository } from './storeConfigRepository';
import { UserRepository } from './userRepository';

export interface StorePointRepositories {
  readonly storeConfig: StoreConfigRepository;
  readonly users: UserRepository;
  readonly catalog: CatalogRepository;
  readonly checkout: CheckoutRepository;
  readonly shifts: ShiftRepository;
  readonly returns: ReturnRepository;
  readonly purchasing: PurchasingRepository;
  readonly production: ProductionRepository;
  readonly schedules: ScheduleRepository;
}

export function createRepositories(db: SqlExecutor): StorePointRepositories {
  return {
    storeConfig: new StoreConfigRepository(db),
    users: new UserRepository(db),
    catalog: new CatalogRepository(db),
    checkout: new CheckoutRepository(db),
    shifts: new ShiftRepository(db),
    returns: new ReturnRepository(db),
    purchasing: new PurchasingRepository(db),
    production: new ProductionRepository(db),
    schedules: new ScheduleRepository(db),
  };
}

let singletonRepos: StorePointRepositories | null = null;

export async function getRepositories(): Promise<StorePointRepositories> {
  if (singletonRepos) {
    return singletonRepos;
  }
  const db = await getDatabase();
  singletonRepos = createRepositories(db);
  return singletonRepos;
}
