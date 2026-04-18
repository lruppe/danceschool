import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CourseSummaryComponent, CourseSummaryData } from './course-summary';

function makeSummaryData(overrides: Partial<CourseSummaryData> = {}): CourseSummaryData {
  return {
    title: 'Bachata Fundamentals',
    danceStyle: 'BACHATA',
    level: 'BEGINNER',
    courseType: 'PARTNER',
    description: null,
    startDate: '2026-04-11',
    dayOfWeek: 'Friday',
    recurrenceType: 'WEEKLY',
    numberOfSessions: 8,
    endDate: 'May 30, 2026',
    startTime: '19:30',
    endTime: '20:45',
    location: 'Studio A',
    teachers: null,
    maxParticipants: 20,
    roleBalancingEnabled: true,
    roleBalanceThreshold: 2,
    priceModel: 'FIXED_COURSE',
    price: 166.5,
    ...overrides,
  };
}

describe('CourseSummaryComponent', () => {
  let fixture: ComponentFixture<CourseSummaryComponent>;
  let el: HTMLElement;

  function setup(data: CourseSummaryData, defaultOpen = true): void {
    TestBed.configureTestingModule({ imports: [CourseSummaryComponent] });
    fixture = TestBed.createComponent(CourseSummaryComponent);
    fixture.componentRef.setInput('data', data);
    fixture.componentRef.setInput('defaultOpen', defaultOpen);
    fixture.detectChanges();
    el = fixture.nativeElement;
  }

  function titles(): (string | undefined)[] {
    return Array.from(el.querySelectorAll('.summary-card-title')).map(e => e.textContent?.trim());
  }

  function openHeaders(): HTMLElement[] {
    return Array.from(el.querySelectorAll('.summary-card-header[aria-expanded="true"]'));
  }

  function clickHeader(title: string): void {
    const header = Array.from(el.querySelectorAll<HTMLElement>('.summary-card-header'))
      .find(h => h.querySelector('.summary-card-title')?.textContent?.trim() === title);
    header?.click();
    fixture.detectChanges();
  }

  it('should render all 5 section titles', () => {
    setup(makeSummaryData());
    expect(titles()).toEqual(['Course Details', 'Schedule', 'Registration', 'Pricing', 'Publication']);
  });

  it('should start with all sections collapsed when defaultOpen=false', () => {
    setup(makeSummaryData(), false);
    expect(openHeaders().length).toBe(0);
    expect(el.querySelectorAll('.summary-card-fields').length).toBe(0);
  });

  it('should start with all sections expanded when defaultOpen=true', () => {
    setup(makeSummaryData(), true);
    expect(openHeaders().length).toBe(5);
    expect(el.querySelectorAll('.summary-card-fields').length).toBe(5);
  });

  it('should toggle a single section independently of the others', () => {
    setup(makeSummaryData(), true);
    clickHeader('Schedule');

    const expanded = openHeaders().map(h => h.querySelector('.summary-card-title')?.textContent?.trim());
    expect(expanded).toEqual(['Course Details', 'Registration', 'Pricing', 'Publication']);
  });

  it('should re-open a collapsed section when its header is clicked again', () => {
    setup(makeSummaryData(), false);
    clickHeader('Pricing');
    expect(openHeaders().length).toBe(1);
    expect(openHeaders()[0].querySelector('.summary-card-title')?.textContent?.trim()).toBe('Pricing');
  });

  it('should display course details fields with labels', () => {
    setup(makeSummaryData());
    const labels = Array.from(el.querySelectorAll('.summary-card:first-child .summary-label')).map(e => e.textContent?.trim());
    expect(labels).toContain('Title');
    expect(labels).toContain('Dance Style');
    expect(labels).toContain('Level');
    expect(labels).toContain('Course Type');
  });

  it('should display field values correctly', () => {
    setup(makeSummaryData());
    const values = Array.from(el.querySelectorAll('.summary-value')).map(e => e.textContent?.trim());
    expect(values).toContain('Bachata Fundamentals');
    expect(values).toContain('Bachata');
    expect(values).toContain('Beginner');
    expect(values).toContain('Partner');
  });

  it('should show description when provided', () => {
    setup(makeSummaryData({ description: 'A great course' }));
    const labels = Array.from(el.querySelectorAll('.summary-label')).map(e => e.textContent?.trim());
    expect(labels).toContain('Description');
  });

  it('should hide description when null', () => {
    setup(makeSummaryData({ description: null }));
    const labels = Array.from(el.querySelectorAll('.summary-label')).map(e => e.textContent?.trim());
    expect(labels).not.toContain('Description');
  });

  it('should show role balancing fields for partner courses', () => {
    setup(makeSummaryData({ courseType: 'PARTNER', roleBalancingEnabled: true, roleBalanceThreshold: 3 }));
    const labels = Array.from(el.querySelectorAll('.summary-label')).map(e => e.textContent?.trim());
    expect(labels).toContain('Role Balancing');
    expect(labels).toContain('Max Imbalance');
  });

  it('should hide role balancing fields for solo courses', () => {
    setup(makeSummaryData({ courseType: 'SOLO' }));
    const labels = Array.from(el.querySelectorAll('.summary-label')).map(e => e.textContent?.trim());
    expect(labels).not.toContain('Role Balancing');
    expect(labels).not.toContain('Max Imbalance');
  });

  it('should show teachers when provided', () => {
    setup(makeSummaryData({ teachers: 'Maria, Carlos' }));
    const labels = Array.from(el.querySelectorAll('.summary-label')).map(e => e.textContent?.trim());
    expect(labels).toContain('Teachers');
  });

  it('should hide teachers when null', () => {
    setup(makeSummaryData({ teachers: null }));
    const labels = Array.from(el.querySelectorAll('.summary-label')).map(e => e.textContent?.trim());
    expect(labels).not.toContain('Teachers');
  });

  it('should display price formatted as CHF', () => {
    setup(makeSummaryData({ price: 166.5 }));
    const values = Array.from(el.querySelectorAll('.summary-value')).map(e => e.textContent?.trim());
    expect(values).toContain('CHF 166.5');
  });

  it('should display Publication section with status and publish date', () => {
    setup(makeSummaryData({ status: 'OPEN', publishedAt: '01.03.2026' }));
    const labels = Array.from(el.querySelectorAll('.summary-label')).map(e => e.textContent?.trim());
    expect(labels).toContain('Publish Status');
    expect(labels).toContain('Publish Date');

    const values = Array.from(el.querySelectorAll('.summary-value')).map(e => e.textContent?.trim());
    expect(values).toContain('Open');
    expect(values).toContain('01.03.2026');
  });

  it('should show Draft status when status is missing', () => {
    setup(makeSummaryData({ status: undefined }));
    const values = Array.from(el.querySelectorAll('.summary-value')).map(e => e.textContent?.trim());
    expect(values).toContain('Draft');
  });

  it('should emit edit event with correct section index when Edit is clicked', () => {
    setup(makeSummaryData());
    const editSpy = vi.fn();
    fixture.componentInstance.edit.subscribe(editSpy);

    const editButtons = el.querySelectorAll('.edit-link');
    expect(editButtons.length).toBe(4);

    (editButtons[0] as HTMLButtonElement).click();
    expect(editSpy).toHaveBeenCalledWith(0);

    (editButtons[1] as HTMLButtonElement).click();
    expect(editSpy).toHaveBeenCalledWith(1);

    (editButtons[2] as HTMLButtonElement).click();
    expect(editSpy).toHaveBeenCalledWith(2);

    (editButtons[3] as HTMLButtonElement).click();
    expect(editSpy).toHaveBeenCalledWith(3);
  });

  it('should not toggle the section when Edit is clicked', () => {
    setup(makeSummaryData(), true);
    const firstEdit = el.querySelector<HTMLButtonElement>('.edit-link');
    firstEdit?.click();
    fixture.detectChanges();

    // All 5 sections should still be expanded after clicking Edit
    expect(openHeaders().length).toBe(5);
  });
});
