import { ChangeDetectionStrategy, Component, HostListener, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SchoolDetail, SchoolService, SchoolUpdateRequest } from '../school.service';

@Component({
  selector: 'app-my-school-edit',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
  ],
  templateUrl: './my-school-edit.html',
  styleUrl: './my-school-edit.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MySchoolEditComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private schoolService = inject(SchoolService);
  private snackBar = inject(MatSnackBar);

  protected saving = signal(false);
  protected loading = signal(true);

  protected form = this.fb.group({
    name: ['', Validators.required],
    tagline: [''],
    about: [''],
    streetAddress: [''],
    city: [''],
    postalCode: [''],
    country: [''],
    phone: [''],
    email: ['', Validators.email],
    website: [''],
    coverImageUrl: [''],
    logoUrl: [''],
  });

  ngOnInit(): void {
    this.schoolService.getMySchool().subscribe({
      next: (school) => {
        this.patchForm(school);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Could not load school data', 'Dismiss', { duration: 5000 });
      },
    });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const data = this.buildRequest();

    this.schoolService.updateMySchool(data).subscribe({
      next: () => {
        this.form.markAsPristine();
        this.router.navigate(['/my-school']);
      },
      error: (err) => {
        this.saving.set(false);
        if (err.status === 400 && err.error?.fieldErrors) {
          this.applyServerErrors(err.error.fieldErrors);
        } else {
          this.snackBar.open('Failed to save changes. Please try again.', 'Dismiss', { duration: 5000 });
        }
      },
    });
  }

  protected cancel(): void {
    this.router.navigate(['/my-school']);
  }

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.form.dirty) {
      event.preventDefault();
    }
  }

  canDeactivate(): boolean {
    return !this.form.dirty;
  }

  private patchForm(school: SchoolDetail): void {
    this.form.patchValue({
      name: school.name ?? '',
      tagline: school.tagline ?? '',
      about: school.about ?? '',
      streetAddress: school.streetAddress ?? '',
      city: school.city ?? '',
      postalCode: school.postalCode ?? '',
      country: school.country ?? '',
      phone: school.phone ?? '',
      email: school.email ?? '',
      website: school.website ?? '',
      coverImageUrl: school.coverImageUrl ?? '',
      logoUrl: school.logoUrl ?? '',
    });
    this.form.markAsPristine();
  }

  private buildRequest(): SchoolUpdateRequest {
    const v = this.form.getRawValue();
    return {
      name: v.name!,
      tagline: v.tagline || null,
      about: v.about || null,
      streetAddress: v.streetAddress || null,
      city: v.city || null,
      postalCode: v.postalCode || null,
      country: v.country || null,
      phone: v.phone || null,
      email: v.email || null,
      website: v.website || null,
      coverImageUrl: v.coverImageUrl || null,
      logoUrl: v.logoUrl || null,
    };
  }

  private applyServerErrors(fieldErrors: Record<string, string>): void {
    for (const [field, message] of Object.entries(fieldErrors)) {
      const control = this.form.get(field);
      if (control) {
        control.setErrors({ server: message });
      }
    }
  }
}
