import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import {
  DANCE_STYLES, COURSE_LEVELS, COURSE_TYPES, RECURRENCE_TYPES, PRICE_MODELS, labelOf, CourseStatus,
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
  status?: CourseStatus;
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

@Component({
  selector: 'app-course-summary',
  imports: [MatButtonModule],
  templateUrl: './course-summary.html',
  styleUrl: './course-summary.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CourseSummaryComponent {
  data = input.required<CourseSummaryData>();
  edit = output<number>();

  protected label(items: { value: string; label: string }[], value: string): string {
    return labelOf(items, value);
  }

  protected readonly danceStyles = DANCE_STYLES;
  protected readonly levels = COURSE_LEVELS;
  protected readonly courseTypes = COURSE_TYPES;
  protected readonly recurrenceTypes = RECURRENCE_TYPES;
  protected readonly priceModels = PRICE_MODELS;
}
