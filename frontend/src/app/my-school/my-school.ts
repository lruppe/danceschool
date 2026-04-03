import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { SchoolDetail, SchoolService } from './school.service';

@Component({
  selector: 'app-my-school',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './my-school.html',
  styleUrl: './my-school.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MySchoolComponent implements OnInit {
  private schoolService = inject(SchoolService);

  protected school = signal<SchoolDetail | null>(null);
  protected error = signal(false);
  protected noSchool = signal(false);

  ngOnInit(): void {
    this.schoolService.getMySchool().subscribe({
      next: (school) => this.school.set(school),
      error: (err) => {
        if (err.status === 404) {
          this.noSchool.set(true);
        } else {
          this.error.set(true);
        }
      },
    });
  }
}
