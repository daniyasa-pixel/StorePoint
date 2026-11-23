import type { PaymentSchedule, SqlExecutor } from '@/db/types';
import { roundMoney } from '@/domain/money';

interface ScheduleRow {
  id: number;
  title: string;
  amount: number;
  dueDate: string;
  category: string;
  priority: 'High' | 'Medium' | 'Low';
  isPaid: number;
  paymentMethod: string;
}

function mapSchedule(r: ScheduleRow): PaymentSchedule {
  return { ...r, isPaid: r.isPaid === 1 };
}

export class ScheduleRepository {
  constructor(private readonly db: SqlExecutor) {}

  async getAll(): Promise<PaymentSchedule[]> {
    const rows = await this.db.getAllAsync<ScheduleRow>(
      'SELECT * FROM payment_schedules ORDER BY isPaid ASC, dueDate ASC;',
    );
    return rows.map(mapSchedule);
  }

  async save(schedule: Omit<PaymentSchedule, 'id'> & { readonly id?: number }): Promise<number> {
    if (schedule.id && schedule.id > 0) {
      await this.db.runAsync(
        `UPDATE payment_schedules SET
          title = ?, amount = ?, dueDate = ?, category = ?, priority = ?, isPaid = ?, paymentMethod = ?
        WHERE id = ?;`,
        [
          schedule.title,
          roundMoney(schedule.amount),
          schedule.dueDate,
          schedule.category,
          schedule.priority,
          schedule.isPaid ? 1 : 0,
          schedule.paymentMethod,
          schedule.id,
        ],
      );
      return schedule.id;
    }
    const res = await this.db.runAsync(
      `INSERT INTO payment_schedules (
        title, amount, dueDate, category, priority, isPaid, paymentMethod
      ) VALUES (?, ?, ?, ?, ?, ?, ?);`,
      [
        schedule.title,
        roundMoney(schedule.amount),
        schedule.dueDate,
        schedule.category,
        schedule.priority,
        schedule.isPaid ? 1 : 0,
        schedule.paymentMethod,
      ],
    );
    return res.lastInsertRowId;
  }

  async setPaid(id: number, isPaid: boolean): Promise<void> {
    await this.db.runAsync('UPDATE payment_schedules SET isPaid = ? WHERE id = ?;', [
      isPaid ? 1 : 0,
      id,
    ]);
  }

  async remove(id: number): Promise<void> {
    await this.db.runAsync('DELETE FROM payment_schedules WHERE id = ?;', [id]);
  }
}
