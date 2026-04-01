import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface GalleryImage {
  url: string;
  position: number;
}

export interface YoutubeVideo {
  url: string;
  position: number;
}

export interface SchoolDetail {
  id: number;
  name: string;
  tagline: string | null;
  about: string | null;
  streetAddress: string | null;
  city: string | null;
  postalCode: string | null;
  country: string | null;
  phone: string | null;
  email: string | null;
  website: string | null;
  coverImageUrl: string | null;
  logoUrl: string | null;
  specialties: string[];
  galleryImages: GalleryImage[];
  youtubeVideos: YoutubeVideo[];
}

@Injectable({ providedIn: 'root' })
export class SchoolService {
  private http = inject(HttpClient);

  getMySchool(): Observable<SchoolDetail> {
    return this.http.get<SchoolDetail>(`${environment.apiUrl}/api/schools/me`);
  }

  updateMySchool(data: SchoolUpdateRequest): Observable<SchoolDetail> {
    return this.http.put<SchoolDetail>(`${environment.apiUrl}/api/schools/me`, data);
  }

  uploadImage(file: File): Observable<{ url: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ url: string }>(`${environment.apiUrl}/api/images`, formData);
  }
}

export interface SchoolUpdateRequest {
  name: string;
  tagline: string | null;
  about: string | null;
  streetAddress: string | null;
  city: string | null;
  postalCode: string | null;
  country: string | null;
  phone: string | null;
  email: string | null;
  website: string | null;
  coverImageUrl: string | null;
  logoUrl: string | null;
  specialties: string[];
  galleryImages: { url: string; position: number }[];
  youtubeVideos: { url: string; position: number }[];
}
