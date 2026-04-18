import { ChangeDetectionStrategy, Component, input, linkedSignal, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  DANCE_STYLES, COURSE_LEVELS, COURSE_TYPES, RECURRENCE_TYPES, PRICE_MODELS, labelOf,
} from '../../shared/course-constants';

export interface CourseSummaryData {
  title: string;
  danceStyle: string;
  level: string;
  courseType: string;
  description?: string | null;
  startDate: string;
  dayOfWeek: string;
  recurrenceType: string;
  numberOfSessions: number;
  completedSessions?: number;
  status?: string;
  publishedAt?: string | null;
  endDate?: string | null;
  startTime: string;
  endTime: string;
  location: string;
  teachers?: string | null;
  maxParticipants: number;
  roleBalancingEnabled: boolean;
  roleBalanceThreshold?: number | null;
  priceModel: string;
  price: number;
}

export type CourseSummarySection =
  | 'details' | 'schedule' | 'registration' | 'pricing' | 'publication';

const ALL_SECTIONS: readonly CourseSummarySection[] =
  ['details', 'schedule', 'registration', 'pricing', 'publication'];

/** Maps section keys to the wizard step index used by the `edit` output. */
const EDIT_STEP_INDEX: Record<Exclude<CourseSummarySection, 'publication'>, number> = {
  details: 0,
  schedule: 1,
  registration: 2,
  pricing: 3,
};

@Component({
  selector: 'app-course-summary',
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './course-summary.html',
  styleUrl: './course-summary.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CourseSummaryComponent {
  data = input.required<CourseSummaryData>();
  defaultOpen = input(false);
  edit = output<number>();

  protected openSections = linkedSignal<Set<CourseSummarySection>>(
    () => new Set(this.defaultOpen() ? ALL_SECTIONS : []),
  );

  protected isOpen(section: CourseSummarySection): boolean {
    return this.openSections().has(section);
  }

  protected toggle(section: CourseSummarySection): void {
    this.openSections.update(current => {
      const next = new Set(current);
      if (next.has(section)) next.delete(section);
      else next.add(section);
      return next;
    });
  }

  protected onEdit(event: Event, section: keyof typeof EDIT_STEP_INDEX): void {
    event.stopPropagation();
    this.edit.emit(EDIT_STEP_INDEX[section]);
  }

  protected label(items: { value: string; label: string }[], value: string): string {
    return labelOf(items, value);
  }

  protected statusLabel(status: string | undefined): string {
    if (!status) return 'Draft';
    return status.charAt(0) + status.slice(1).toLowerCase();
  }

  protected readonly danceStyles = DANCE_STYLES;
  protected readonly levels = COURSE_LEVELS;
  protected readonly courseTypes = COURSE_TYPES;
  protected readonly recurrenceTypes = RECURRENCE_TYPES;
  protected readonly priceModels = PRICE_MODELS;
}
