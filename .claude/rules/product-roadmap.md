# Product Roadmap

**Phase 1 — DanceStudio Manager Platform (current)**
- Admin portal (Angular frontend) for school Owners and Teachers
- Office-oriented: manage sign-ups, payments, organize helpers when students cancel (partner matching for couple dance)
- Multi-tenant B2B — each school is a tenant

**Phase 2 — Student App (future, Android/iOS)**
- Students browse dance schools, view classes, enroll
- Social login (Apple, Google, possibly Instagram) — frictionless onboarding is a priority
- Dance events feed — community-oriented: school owners post events, plus external sources (scraped data, festival organizations)
- Community building is a core goal

**Phase 3 — Online payments (future)**
- Processor: **Stripe**. Mobile clients use the **Stripe Mobile SDK (PaymentSheet)** — native bottom sheet, Apple Pay / Google Pay out of the box. Web admin uses Stripe Checkout (hosted redirect).
- **Direct-pay happy path:** student applies → booking logic resolves to direct path → backend creates a PaymentIntent → phone presents PaymentSheet inline → Stripe webhook flips enrollment to CONFIRMED. Payment happens on-the-fly as part of enrollment.
- **Deferred-payment path (`PENDING_PAYMENT`) reserved for unhappy / non-inline cases:** approval-required enrollments, waitlist promotions, and admin walk-ins paid offline. These wait in `PENDING_PAYMENT` until resolved (payment link sent, or admin marks paid).
- **Marketplace model (TBD):** each school is a tenant and receives its own payouts → need a Stripe Connect-style solution. **Stripe Connect** is the leading candidate; research still needed on Connect flavor (Standard / Express / Custom), fee structure, onboarding UX, tax handling, and payout cadence.
- **v1 ships without real payment integration** — all enrollments handled offline (admin marks paid). Architectural stubs in place (a `PENDING_PAYMENT` state and a single `confirmPayment` seam on the backend) so Stripe can be wired in without reshaping the enrollment flow or the API contract.

**Business model:** Solve admin pain for teachers → get students onto the platform → build community via events → monetize via ads, school subscriptions, and other revenue streams TBD

## Authentication & Authorization — Phase 1 assumptions that will change

- **Single school per user.** A user has at most one `SchoolMember` row — enforced via DB unique constraint on `school_member.user_id`. Lets admin authz collapse to "is caller a member of any school?" and keeps `/me` URLs unambiguous. **Phase 2 drops the cap** (a user may be OWNER at School A and Student at Schools B, C…).
- **OWNER == TEACHER.** Both roles treated identically in Phase 1. **Split when a concrete capability difference appears** (e.g., only OWNER can manage members or delete the school).
- **Implicit school in `/me` URLs.** Works only because of the single-school cap. **Phase 2 refactors admin endpoints to `/api/schools/{schoolId}/...`** with parameterized authz.
- **No student login.** Students are admin-created data rows. **Phase 2 activates student sign-in** and layers student-specific authz onto enrollment endpoints.
- **Auth providers: email/password + Google only.** **Phase 2 adds Sign in with Apple** (App Store Guideline 4.8 — mandatory once iOS ships). Skip Instagram/Facebook for auth.
