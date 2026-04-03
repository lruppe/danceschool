import { ChangeDetectionStrategy, Component, DestroyRef, HostListener, inject, signal, OnInit, WritableSignal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { GalleryImage, SchoolDetail, SchoolService, SchoolUpdateRequest } from '../school.service';
import { AuthService } from '../../shared/auth/auth.service';

@Component({
  selector: 'app-my-school-edit',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
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
  private auth = inject(AuthService);
  private destroyRef = inject(DestroyRef);

  protected saving = signal(false);
  protected loading = signal(true);
  protected creationMode = signal(false);
  protected specialties = signal<string[]>([]);
  protected addingSpecialty = signal(false);
  protected youtubeVideos = signal<string[]>([]);
  protected coverImageUrl = signal<string | null>(null);
  protected logoUrl = signal<string | null>(null);
  protected galleryImages = signal<GalleryImage[]>([]);
  protected uploadingCover = signal(false);
  protected uploadingLogo = signal(false);
  protected uploadingGallery = signal(false);
  private specialtiesDirty = signal(false);
  private youtubeVideosDirty = signal(false);
  private imagesDirty = signal(false);

  protected readonly MAX_GALLERY_IMAGES = 12;
  private static readonly ACCEPTED_IMAGE_TYPES = 'image/jpeg,image/png,image/webp';

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
  });

  ngOnInit(): void {
    const user = this.auth.user();
    if (user && user.memberships.length === 0) {
      this.creationMode.set(true);
      this.loading.set(false);
      return;
    }

    this.schoolService.getMySchool().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
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

    const invalidVideos = this.youtubeVideos().some(url => url.trim() && !this.isValidYoutubeUrl(url));
    if (invalidVideos) {
      this.snackBar.open('Please enter valid YouTube URLs', 'Dismiss', { duration: 5000 });
      return;
    }

    this.saving.set(true);
    const data = this.buildRequest();

    const save$ = this.creationMode()
      ? this.schoolService.createSchool(data)
      : this.schoolService.updateMySchool(data);

    save$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.form.markAsPristine();
        this.specialtiesDirty.set(false);
        this.youtubeVideosDirty.set(false);
        this.imagesDirty.set(false);
        if (this.creationMode()) {
          this.auth.checkAuth();
        }
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

  protected removeSpecialty(index: number): void {
    this.specialties.update(s => s.filter((_, i) => i !== index));
    this.specialtiesDirty.set(true);
  }

  protected confirmSpecialty(value: string): void {
    const trimmed = value.trim();
    if (trimmed) {
      this.specialties.update(s => [...s, trimmed]);
      this.specialtiesDirty.set(true);
    }
    this.addingSpecialty.set(false);
  }

  protected addVideo(): void {
    this.youtubeVideos.update(v => [...v, '']);
    this.youtubeVideosDirty.set(true);
  }

  protected updateVideoUrl(index: number, url: string): void {
    this.youtubeVideos.update(v => v.map((u, i) => i === index ? url : u));
    this.youtubeVideosDirty.set(true);
  }

  protected removeVideo(index: number): void {
    this.youtubeVideos.update(v => v.filter((_, i) => i !== index));
    this.youtubeVideosDirty.set(true);
  }

  protected get acceptedImageTypes(): string {
    return MySchoolEditComponent.ACCEPTED_IMAGE_TYPES;
  }

  protected onCoverFileSelected(event: Event): void {
    this.uploadFile(event, this.uploadingCover, 'Failed to upload cover image', (url) => {
      this.coverImageUrl.set(url);
    });
  }

  protected onLogoFileSelected(event: Event): void {
    this.uploadFile(event, this.uploadingLogo, 'Failed to upload logo', (url) => {
      this.logoUrl.set(url);
    });
  }

  protected onGalleryFileSelected(event: Event): void {
    if (this.galleryImages().length >= this.MAX_GALLERY_IMAGES) return;
    this.uploadFile(event, this.uploadingGallery, 'Failed to upload image', (url) => {
      this.galleryImages.update(imgs => [...imgs, { url, position: imgs.length }]);
    });
  }

  private uploadFile(event: Event, uploading: WritableSignal<boolean>, errorMsg: string, onSuccess: (url: string) => void): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    uploading.set(true);
    this.schoolService.uploadImage(file).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => {
        onSuccess(res.url);
        this.imagesDirty.set(true);
        uploading.set(false);
      },
      error: () => {
        this.snackBar.open(errorMsg, 'Dismiss', { duration: 5000 });
        uploading.set(false);
      },
    });
    input.value = '';
  }

  protected removeGalleryImage(index: number): void {
    this.galleryImages.update(imgs =>
      imgs.filter((_, i) => i !== index).map((img, i) => ({ ...img, position: i }))
    );
    this.imagesDirty.set(true);
  }

  protected isValidYoutubeUrl(url: string): boolean {
    if (!url) return true;
    try {
      const parsed = new URL(url);
      return ['youtube.com', 'www.youtube.com', 'youtu.be'].includes(parsed.hostname);
    } catch {
      return false;
    }
  }

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.isDirty()) {
      event.preventDefault();
    }
  }

  canDeactivate(): boolean {
    return !this.isDirty();
  }

  private isDirty(): boolean {
    return this.form.dirty || this.specialtiesDirty() || this.youtubeVideosDirty() || this.imagesDirty();
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
    });
    this.specialties.set([...(school.specialties ?? [])]);
    this.youtubeVideos.set((school.youtubeVideos ?? []).map(v => v.url));
    this.coverImageUrl.set(school.coverImageUrl ?? null);
    this.logoUrl.set(school.logoUrl ?? null);
    this.galleryImages.set([...(school.galleryImages ?? [])]);
    this.form.markAsPristine();
    this.specialtiesDirty.set(false);
    this.youtubeVideosDirty.set(false);
    this.imagesDirty.set(false);
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
      coverImageUrl: this.coverImageUrl() || null,
      logoUrl: this.logoUrl() || null,
      specialties: this.specialties(),
      galleryImages: this.galleryImages().map((img, i) => ({ url: img.url, position: i })),
      youtubeVideos: this.youtubeVideos()
        .filter(url => url.trim())
        .map((url, i) => ({ url, position: i })),
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
