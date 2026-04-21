import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, OnInit, ViewChild, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { AuthService } from '../shared/auth/auth.service';
import { StudentListItem, StudentService } from './student.service';

interface TabConfig {
  label: string;
}

const COLUMNS = ['name', 'email', 'phone', 'activeCourses'];

const TAB_CONFIGS: TabConfig[] = [
  { label: 'Active' },
  { label: 'Inactive' },
];

const ACTIVE_TAB_INDEX = 0;
const INACTIVE_TAB_INDEX = 1;

@Component({
  selector: 'app-students',
  imports: [
    MatTableModule, MatSortModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatTabsModule,
    FormsModule, RouterLink,
  ],
  templateUrl: './students.html',
  styleUrl: './students.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentsComponent implements OnInit {
  private auth = inject(AuthService);
  private studentService = inject(StudentService);
  private destroyRef = inject(DestroyRef);

  protected hasSchool = computed(() => {
    const u = this.auth.user();
    return u ? u.memberships.length > 0 : false;
  });

  protected tabs = TAB_CONFIGS;
  protected activeTabIndex = signal(ACTIVE_TAB_INDEX);
  protected loaded = signal(false);
  protected error = signal(false);

  protected tabData: MatTableDataSource<StudentListItem>[] = TAB_CONFIGS.map(() => new MatTableDataSource<StudentListItem>([]));
  protected tabCounts = signal<number[]>(TAB_CONFIGS.map(() => 0));

  protected searchText = '';

  protected columns = COLUMNS;
  protected activeDataSource = computed(() => this.tabData[this.activeTabIndex()]);

  private matSort?: MatSort;

  @ViewChild(MatSort) set sort(sort: MatSort) {
    if (sort) {
      this.matSort = sort;
      this.tabData[this.activeTabIndex()].sort = sort;
    }
  }

  ngOnInit(): void {
    if (!this.hasSchool()) return;

    this.tabData.forEach(ds => {
      ds.filterPredicate = this.createFilterPredicate();
    });

    this.studentService.getStudents().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (students) => {
        this.partitionStudents(students);
        this.loaded.set(true);
      },
      error: () => {
        this.error.set(true);
        this.loaded.set(true);
      },
    });
  }

  protected selectTab(index: number): void {
    this.activeTabIndex.set(index);
    if (this.matSort) {
      this.tabData[index].sort = this.matSort;
    }
  }

  private partitionStudents(students: StudentListItem[]): void {
    const active: StudentListItem[] = [];
    const inactive: StudentListItem[] = [];
    for (const s of students) {
      if (s.activeCoursesCount > 0) active.push(s);
      else inactive.push(s);
    }
    this.tabData[ACTIVE_TAB_INDEX].data = active;
    this.tabData[INACTIVE_TAB_INDEX].data = inactive;

    const counts = this.tabCounts().slice();
    counts[ACTIVE_TAB_INDEX] = active.length;
    counts[INACTIVE_TAB_INDEX] = inactive.length;
    this.tabCounts.set(counts);
  }

  protected applyFilter(): void {
    const text = this.searchText.trim().toLowerCase();
    this.tabData.forEach(ds => ds.filter = text);
  }

  private createFilterPredicate(): (data: StudentListItem, filter: string) => boolean {
    return (data: StudentListItem, filter: string): boolean => {
      if (!filter) return true;
      const searchable = [data.name, data.email].join(' ').toLowerCase();
      return searchable.includes(filter);
    };
  }
}
