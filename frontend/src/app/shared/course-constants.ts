export type DanceStyle = 'SALSA' | 'BACHATA' | 'MERENGUE' | 'KIZOMBA' | 'ZOUK' | 'AFRO' | 'OTHER';
export type CourseLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
export type CourseType = 'PARTNER' | 'SOLO';
export type CourseStatus = 'DRAFT' | 'ACTIVE' | 'FULL' | 'INACTIVE';
export type RecurrenceType = 'WEEKLY';
export type PriceModel = 'FIXED_COURSE';
export type RoleBalancingMode = 'WARN' | 'BLOCK';

export const DANCE_STYLES: { value: DanceStyle; label: string }[] = [
  { value: 'SALSA', label: 'Salsa' },
  { value: 'BACHATA', label: 'Bachata' },
  { value: 'MERENGUE', label: 'Merengue' },
  { value: 'KIZOMBA', label: 'Kizomba' },
  { value: 'ZOUK', label: 'Zouk' },
  { value: 'AFRO', label: 'Afro' },
  { value: 'OTHER', label: 'Other' },
];

export const COURSE_LEVELS: { value: CourseLevel; label: string }[] = [
  { value: 'BEGINNER', label: 'Beginner' },
  { value: 'INTERMEDIATE', label: 'Intermediate' },
  { value: 'ADVANCED', label: 'Advanced' },
];

export const COURSE_TYPES: { value: CourseType; label: string }[] = [
  { value: 'PARTNER', label: 'Partner' },
  { value: 'SOLO', label: 'Solo' },
];

export const COURSE_STATUSES: { value: CourseStatus; label: string }[] = [
  { value: 'DRAFT', label: 'Draft' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'FULL', label: 'Full' },
  { value: 'INACTIVE', label: 'Inactive' },
];

export const RECURRENCE_TYPES: { value: RecurrenceType; label: string }[] = [
  { value: 'WEEKLY', label: 'Weekly' },
];

export const PRICE_MODELS: { value: PriceModel; label: string }[] = [
  { value: 'FIXED_COURSE', label: 'Fixed Course' },
];

export const ROLE_BALANCING_MODES: { value: RoleBalancingMode; label: string }[] = [
  { value: 'WARN', label: 'Warn' },
  { value: 'BLOCK', label: 'Block' },
];
