# PRD: My School Edit Page

## Problem Statement

School administrators can view their school's profile on the "My School" page but have no way to edit it. The only data that exists is what was entered during onboarding (name, address, phone, email). All the richer profile fields — tagline, about, specialties, website, cover image, logo, gallery, and YouTube videos — remain empty with no way to populate them. Admins need a dedicated edit page to manage their school's complete profile.

## Design Reference

- **Figma design page (edit):** `https://www.figma.com/design/uQoQs1ZZKO8fGIwFsaLStB/Dance-School-Admin?node-id=61-257`
- **Figma design page (view — for context):** `https://www.figma.com/design/uQoQs1ZZKO8fGIwFsaLStB/Dance-School-Admin?node-id=46-174`

## Solution

An edit page at `/my-school/edit` that lets the school admin modify all profile fields. The page mirrors the view page's section layout but replaces read-only content with form inputs. Changes are saved via a single "Save Changes" button; "Cancel" discards changes and returns to the view page.

This page also doubles as the **school creation form**. When a user has no school yet, they are redirected to `/my-school/edit` in creation mode. This replaces the existing `/onboarding` page.

## User Stories

1. As a school admin, I want to edit my school's name and tagline inline in the hero section, so that I can update how my school is presented
2. As a school admin, I want to upload a cover image, so that I can customize the visual branding
3. As a school admin, I want to upload a school logo, so that students see the correct branding
4. As a school admin, I want to edit my school's "About" description in a textarea, so that I can write a compelling narrative
5. As a school admin, I want to add and remove specialties as chips, so that I can accurately list the dance styles we offer
6. As a school admin, I want to edit structured address fields (street, city, postal code, country), so that the location is accurate and well-formatted
7. As a school admin, I want to edit phone, email, and website, so that students can reach us
8. As a school admin, I want to upload and remove gallery images, so that I can showcase my school visually
9. As a school admin, I want to add and remove YouTube video URLs, so that I can link promotional content
10. As a school admin, I want to click "Save Changes" to persist all edits at once, so that I can review everything before committing
11. As a school admin, I want to click "Cancel" to discard changes and return to the view page, so that I can back out of accidental edits
12. As a school admin, I want to see validation feedback on fields I've filled in incorrectly, so that I don't accidentally save invalid data
13. As a school admin, I want to be warned about unsaved changes if I navigate away, so that I don't accidentally lose my edits
14. As a new user, I want to create my school via the edit form (creation mode), so that I can get started without a separate onboarding flow
15. As a new user who hasn't created a school yet, I want to see a helpful empty state on other pages prompting me to set up my school, so that I know what to do

## Implementation Decisions

### Data Model Changes

The `address` field is currently a single string. The Figma edit form shows structured address fields. Expand the address model:

| Change | Type | Details |
|---|---|---|
| Add `streetAddress` | String (nullable) | New column on `school` |
| Add `city` | String (nullable) | New column on `school` |
| Add `postalCode` | String (nullable) | New column on `school` |
| Add `country` | String (nullable) | New column on `school` |
| Remove `address` | — | Drop column, migrate existing data into `streetAddress` (best-effort) |

**Migration strategy:** Existing `address` values (from onboarding) will be moved to `streetAddress` as a single-line best-effort migration. Users can then restructure it via the edit page.

### Image Upload Infrastructure

Images (cover, logo, gallery) require real file upload — not URL input. This PRD includes the storage infrastructure.

**Storage abstraction:**

```java
public interface ImageStorageService {
    String store(byte[] data, String filename);  // returns public URL
    void delete(String key);
}
```

- **Local dev:** `FilesystemImageStorageService` — saves to a local directory, served via a static endpoint
- **Production:** `CloudflareR2ImageStorageService` — S3-compatible, uses Cloudflare R2

**Upload endpoint:** `POST /api/images`
- Accepts multipart file upload
- Returns the public URL of the stored image
- The edit form uploads images immediately on selection, stores the returned URL, then includes it in the PUT request on save

**Constraints:**
- Max file size: **5 MB**
- Allowed formats: **JPEG, PNG, WebP**
- Max gallery images per school: **12**
- No server-side resizing for v1 (store originals)

### Onboarding Removal

The existing `/onboarding` page and route are removed. Replaced by:

- **Creation mode:** When the authenticated user has no school, redirect to `/my-school/edit`. The edit form detects no existing school and enters creation mode:
  - Header shows "Set up your school" instead of "Editing your dance school information"
  - Cancel button is hidden (no previous state to return to)
  - "Save Changes" button reads "Create School"
  - First save calls `POST /api/schools` (with expanded fields), subsequent saves call `PUT /api/schools/me`
- **Empty states:** When no school exists, other pages (dashboard, students, my-school view) show a simple consistent message: "Set up your school to get started" with a CTA linking to `/my-school/edit`
- **Redirect logic:** The existing onboarding redirect guard is updated to point to `/my-school/edit` instead of `/onboarding`

### API

**Updated endpoint:** `GET /api/schools/me`
- Response updated with structured address fields (replaces single `address` string)

**New endpoint:** `PUT /api/schools/me`
- Updates the school where the authenticated user is an OWNER
- 401 if unauthenticated, 404 if user has no school
- 400 with field-level validation errors if request body is invalid

**Updated endpoint:** `POST /api/schools`
- Request body expanded to accept all profile fields (not just name/address/phone/email)

**New endpoint:** `POST /api/images`
- Multipart file upload
- Returns `{ "url": "https://..." }`
- 400 if file exceeds 5 MB or format not allowed
- 401 if unauthenticated

**PUT /api/schools/me request body:**

```json
{
  "name": "Ritmo Latino Dance Academy",
  "tagline": "Where Passion Meets Rhythm",
  "about": "Founded in 2018...",
  "streetAddress": "456 Dance Street",
  "city": "Miami",
  "postalCode": "33101",
  "country": "United States",
  "phone": "+1 (305) 555-DANCE",
  "email": "info@ritmolatinodc.com",
  "website": "www.ritmolatinodc.com",
  "coverImageUrl": "https://r2.example.com/cover.jpg",
  "logoUrl": "https://r2.example.com/logo.png",
  "specialties": ["Bachata", "Salsa", "Social Dancing", "Private Lessons"],
  "galleryImages": [
    { "url": "https://r2.example.com/photo1.jpg", "position": 0 },
    { "url": "https://r2.example.com/photo2.jpg", "position": 1 }
  ],
  "youtubeVideos": [
    { "url": "https://www.youtube.com/watch?v=abc123", "position": 0 }
  ]
}
```

**Response:** Returns the updated `SchoolDetailDto` (same shape as `GET /api/schools/me`).

**Validation rules:**
- `name` is required (not blank)
- `email` must be valid email format if provided
- `website` must be valid URL format if provided
- YouTube video URLs must be valid YouTube URLs (`youtube.com` or `youtu.be` domain) if provided
- Gallery image URLs must be valid URLs if provided
- Max 12 gallery images
- Max 2 YouTube videos
- Collections (`specialties`, `galleryImages`, `youtubeVideos`) are replaced wholesale — the request body represents the full desired state

**Error response shape (400):**

```json
{
  "errors": [
    { "field": "email", "message": "Must be a valid email address" },
    { "field": "name", "message": "Must not be blank" }
  ]
}
```

### Frontend

- **Route:** `/my-school/edit` (lazy-loaded standalone component)
- **Navigation:** Reached via the "Edit" button on the view page, or via redirect when no school exists
- **Form:** Reactive form (`FormGroup`) with all fields
- **Cancel:** Navigates back to `/my-school` (hidden in creation mode)
- **Save:** Calls `PUT /api/schools/me` (or `POST /api/schools` in creation mode), on success navigates to `/my-school`
- **Loading state:** Form is pre-populated from `GET /api/schools/me` data (empty in creation mode)
- **Unsaved changes:** `canDeactivate` route guard + `beforeunload` event — warns user if form is dirty

**Validation UX (established as a reusable pattern for the app):**
- **When to show errors:** On blur of a dirty field, plus on submit attempt (marks all fields as touched)
- **Error display:** `<mat-error>` inside `<mat-form-field>` — Angular Material's built-in pattern
- **Server errors:** 400 validation errors mapped back to form fields. Network/500 errors shown as a dismissable snackbar

**Section-by-section UI (matching Figma):**

1. **Header** — "My School" title (or "Set up your school" in creation mode), subtitle, Cancel (stroked, hidden in creation mode) and Save Changes / Create School (flat/primary) buttons
2. **Hero Card** — Cover image area with "Change Cover" file picker button, school logo (80px circle) with file picker, name input, tagline input
3. **About & Specialties row** — About textarea (2:1 flex ratio), Specialties card with removable chips + inline "+ Add" text input (1:1 flex ratio). Clicking "+ Add" transforms it into a text input; Enter or blur confirms.
4. **Location & Contact card** — Two-column layout: left column has Street Address, City + Postal Code (side by side), Country; right column has Phone, Email, Website
5. **Gallery card** — Image grid (3 columns, max 12) with delete (x) buttons on each image, "+ Add Image" file picker button
6. **YouTube Videos card** — URL input rows with delete buttons, "+ Add Video" button (max 2)

**Responsive:** Side-by-side sections stack vertically on mobile using the existing `ds.bp-down(md)` breakpoint mixins.

### Modules (implementation order)

1. **Image storage abstraction** — `ImageStorageService` interface with filesystem (dev) and Cloudflare R2 (prod) implementations
2. **Image upload endpoint** — `POST /api/images` with multipart handling, validation (size, format)
3. **Database migration** — Liquibase changeset: add `street_address`, `city`, `postal_code`, `country` columns; migrate `address` data to `street_address`; drop `address` column
4. **Backend model update** — Update `School` entity, `SchoolDetailDto`, `SchoolDto`, `SchoolMapper` for structured address
5. **PUT endpoint** — Add `PUT /api/schools/me` to `SchoolController` with `SchoolUpdateDto` and validation; update `POST /api/schools` to accept all fields
6. **Frontend validation pattern** — Establish reusable error display utilities (form helpers, error mapping from server responses, snackbar service)
7. **Frontend model update** — Update `SchoolDetail` interface and `SchoolService` for structured address + add `updateMySchool()` and `uploadImage()` methods
8. **My School view update** — Update the view page template to display structured address fields instead of single `address`
9. **My School edit component** — New standalone component with reactive form, all edit sections, file upload integration
10. **Route registration & guards** — Add `/my-school/edit` route, `canDeactivate` guard, update onboarding redirect to point to `/my-school/edit`
11. **Remove onboarding** — Delete `/onboarding` component and route
12. **Empty states** — Add "no school" empty state component, use on dashboard, students, my-school view

## Testing Decisions

- **Backend integration tests** for `PUT /api/schools/me`: verifies update persists correctly, validation errors return 400 with field-level messages, 401 when unauthenticated, 404 when user has no school
- **Backend integration tests** for `POST /api/images`: verifies upload stores file, returns URL, rejects oversized/wrong-format files
- **Backend integration test** for address migration: verify existing `address` data is correctly migrated to `streetAddress`
- **Visual verification** via Playwright screenshot for frontend layout — edit mode and creation mode

## Out of Scope

- **YouTube embed preview** — no inline video player in edit mode; just URL management
- **Autosave / draft** — changes are only persisted on explicit "Save Changes"
- **Undo/redo** — no history of edits
- **Concurrent editing protection** — no optimistic locking (single-admin assumption)
- **Rich text editor for About** — plain textarea only
- **Server-side image resizing** — store originals for v1; optimize later if needed
- **Image cropping UI** — upload as-is; no in-browser crop tool

## Further Notes

- The address model change (single string → structured fields) is a breaking change for the `GET /api/schools/me` response. The frontend view page must be updated in the same release.
- The edit page in creation mode is a temporary placeholder. A future public-facing onboarding funnel (landing page → guided setup → account creation) will replace it.
- The frontend validation pattern (dirty+blur, `<mat-error>`, server error mapping, snackbar) established here becomes the standard for all future forms in the app.
- The `ImageStorageService` abstraction is designed for reuse — the future student app will need image upload for profile photos, event images, etc.
