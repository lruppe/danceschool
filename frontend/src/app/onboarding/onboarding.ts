import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { environment } from '../../environments/environment';
import { AuthService } from '../shared/auth/auth.service';

@Component({
  selector: 'app-onboarding',
  imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './onboarding.html',
  styleUrl: './onboarding.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OnboardingComponent {
  private http = inject(HttpClient);
  private router = inject(Router);
  private auth = inject(AuthService);
  private fb = inject(FormBuilder);

  protected submitting = signal(false);

  protected form = this.fb.group({
    name: ['', Validators.required],
    address: [''],
    phone: [''],
    email: [''],
  });

  submit(): void {
    if (this.form.invalid) return;

    this.submitting.set(true);
    this.http.post(`${environment.apiUrl}/api/schools`, this.form.value).subscribe({
      next: () => {
        this.auth.checkAuth();
        this.router.navigate(['/dashboard']);
      },
      error: () => this.submitting.set(false),
    });
  }
}
