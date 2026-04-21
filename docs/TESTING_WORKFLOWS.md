# Testing Workflows

Testable business workflows and their expected behaviors. Use Dev Tools to simulate student enrollment flows without manual data setup.

## Prerequisites

1. Start backend and frontend (see `CLAUDE.md` for commands)
2. Log in as **Owner 1** (`owner@test.com` / `password`) or **Owner 2** (`owner2@test.com` / `password`)
3. The database resets on every backend restart with fresh seed data

## Seed Data

Seed content (courses, students, enrollments, statuses) is defined in [`backend/src/main/java/ch/ruppen/danceschool/dev/DevDataSeeder.java`](../backend/src/main/java/ch/ruppen/danceschool/dev/DevDataSeeder.java). Read that file for the authoritative layout. It is designed to cover every enrollment state out of the box (courses in each lifecycle stage, students with varying dance levels, and seeded rows for CONFIRMED, PENDING_PAYMENT, and PENDING_APPROVAL). School 2 has no seeded enrollments — useful for empty-course and tenant-isolation tests.

---

## Enrolling a Student

**Business case:** A school owner registers a new student for a course.

**How to test:** In **Dev Tools**, pick a course and click "Add Student". For PARTNER courses, pick Leader or Follower with the role dropdown first.

**Expected:**
- On a BEGINNER/STARTER course with spare capacity → new enrollment lands in the **Enrolled** tab as `PENDING_PAYMENT` (shown with a "Mark Paid" button)
- On an INTERMEDIATE+ course → new enrollment lands in **Approve** tab (status `PENDING_APPROVAL`) because Dev Tools' generated students have no dance levels
- Role selector is harmless for SOLO courses (the value is ignored)
- The Course Overview tab counts update

---

## Filling a Course to Capacity

**Business case:** Simulate a fully booked course to test capacity limits and the enrollment experience at scale.

**How to test:** In **Dev Tools**, select an empty BEGINNER course and click "Fill Course".

**Expected:**
- Students are created and enrolled sequentially until capacity is reached (alternating LEAD/FOLLOW for PARTNER courses)
- All enrollments land in the **Enrolled** tab as `PENDING_PAYMENT` (each row has a "Mark Paid" button)
- Clicking "Fill Course" on an already-full course shows the "Course is already full" snack bar
- Trying "Add Student" on a full course sends the new enrollment to **Waitlist** with reason `CAPACITY` (see waitlist flow below)

**Limitation:** Fill Course reliably works **only on BEGINNER-level courses**. For INTERMEDIATE/ADVANCED courses, generated students have no dance level records and enrollments land in `PENDING_APPROVAL`, not `PENDING_PAYMENT`. The "Enrolled X students" snack bar message can be misleading in that case.

---

## Confirming Payments

**Business case:** After students sign up, the school owner confirms that payment has been received.

**How to test (bulk, via Dev Tools):** Select a course with pending payments and click "Simulate Payment" to confirm all at once.

**How to test (individual, via Course Overview):** Open a course → **Enrolled** tab → click "Mark Paid" on a `PENDING_PAYMENT` row.

**Expected:**
- Payment confirmation flips the row from `PENDING_PAYMENT` to `CONFIRMED` in place on the **Enrolled** tab
- The Course Overview tab counts update
- On the **Enrolled** tab, `CONFIRMED` rows show the Paid date; `PENDING_PAYMENT` rows show a "Mark Paid" button in the same column

---

## Approval Flow (level-gated)

**Business case:** INTERMEDIATE+ courses require the owner to approve students who don't have the required dance level on record.

**How it triggers:**
- A student enrolls in a course with `level >= INTERMEDIATE`
- The student's dance-level record for that style is missing, or lower than the course level
- The enrollment is created with status `PENDING_APPROVAL` (BEGINNER/STARTER courses always skip approval)
- Note: `PENDING_APPROVAL` rows do **not** count toward capacity

**How to test:**
1. Use the seeded Salsa Advanced course (already has 2 PENDING_APPROVAL rows), **or** go to Dev Tools and Add Student to any INTERMEDIATE+ course
2. Open the course → **Approve** tab → use the ✔ (approve) or ✖ (reject) icon buttons
3. On approve: the student's dance level is upserted (registered if missing, upgraded if lower), then the enrollment re-checks capacity:
   - Space available → `PENDING_PAYMENT` (moves to Enrolled tab, shown with "Mark Paid" button)
   - Course full → `WAITLISTED` with reason `CAPACITY` (moves to Waitlist tab)
4. On reject: enrollment becomes `REJECTED` and disappears from the Approve tab

**Expected:**
- Approve tab shows each pending row with a student-level chip and the approve/reject icon buttons
- After approve, the student keeps their new dance level (important side effect: approving also certifies the student's level for that style)
- Reject is terminal — the row is gone from all enrollment tabs

---

## Waitlist Flow

**Business case:** When a course is full or role-imbalanced, further enrollments are held on the waitlist in FIFO order (per role for PARTNER courses).

### Waitlist by capacity

**How it triggers:** Committed enrollments (`PENDING_PAYMENT + CONFIRMED`) have reached `maxParticipants`. A further enrollment attempt produces `WAITLISTED` with reason `CAPACITY`.

**How to test:**
1. Dev Tools → empty BEGINNER course → Fill Course (reaches capacity)
2. Add Student → new row lands in **Waitlist** tab with reason chip "Capacity" and position `#1`
3. Add Student again → second row on waitlist with position `#2` (or `#2` within the same role for PARTNER courses)

### Waitlist by role imbalance

**How it triggers:** Only for PARTNER courses with a non-null `roleBalanceThreshold`. If adding another `LEAD` would push `leads - follows > threshold`, the new LEAD enrollment is waitlisted with reason `ROLE_IMBALANCE`. Default threshold when enabling balancing on a new course is 3. A `null` threshold means balancing is off (no imbalance check).

**How to test:**
1. Dev Tools → pick a PARTNER BEGINNER course with balancing enabled (e.g., "Bachata Beginners" — threshold 3)
2. Pick a role (e.g., Leader) → click "Create Imbalance" (button appears only for PARTNER courses with a non-null threshold)
3. The UI enrolls enough LEADs to exceed `threshold + 1`
4. Overflow LEADs appear in **Waitlist** tab with reason chip "Role imbalance" and a position number

**Expected:**
- Waitlist tab shows each row with position chip (`#1`, `#2`, …) + reason chip
- Positions are FIFO per role; solo courses use a single queue
- **No auto-promotion:** when a seat opens, the waitlist does not auto-advance — it's manual for now

### Promotion via approval

**Side effect of the approval flow:** Approving a `PENDING_APPROVAL` row when the course is full routes the student to `WAITLISTED (CAPACITY)` instead of `PENDING_PAYMENT`. This is the only way rows enter the waitlist through the UI without Dev Tools.

---

## Reviewing Enrollment Status by Course

**Business case:** An owner wants to see who is enrolled, on the waitlist, awaiting approval, or still owes payment for a specific course.

**How to test:** Open any non-DRAFT course from the Courses page and click through the tabs.

**Expected tabs (in this order):**

| # | Tab | Shows | Last-column content |
|---|---|---|---|
| 0 | **Enrolled** | `CONFIRMED` + `PENDING_PAYMENT` rows | Paid date for `CONFIRMED`, "Mark Paid" button for `PENDING_PAYMENT` |
| 1 | **Waitlist** | `WAITLISTED` rows | Position chip + reason chip (Capacity / Role imbalance) |
| 2 | **Approve** | `PENDING_APPROVAL` rows | Approval-reason chip (the student's dance level, or "No level" if unset) + approve/reject icon buttons |

- Every tab shows a count badge next to the label
- PARTNER course rows show a role chip in the Role column; SOLO course rows show "—"
- DRAFT courses do **not** show tabs — only the Course Summary and a "Publish" button

---

## Courses List — Role Distribution Column

**Business case:** A quick glance at the Courses page should show how balanced enrollment is for PARTNER courses.

**How to test:** Visit the Courses page and view the **Enrollment** column.

**Expected:**
- SOLO courses show `X / maxParticipants`
- PARTNER courses additionally show `XL / YF` beneath, where X = leads enrolled, Y = follows enrolled (both count `PENDING_PAYMENT + CONFIRMED` — waitlisted and pending-approval rows excluded)

---

## Full Enrollment Lifecycle (End-to-End)

**Business case:** Walk through the complete journey from sign-up to confirmed enrollment on a beginner partner course.

**How to test:**
1. Dev Tools → pick "Bachata Beginners" (empty, PARTNER, BEGINNER) → add one LEAD and one FOLLOW
2. Simulate Payment → both move to **Enrolled**
3. Add one more student (any role) → new `PENDING_PAYMENT` row appears on the **Enrolled** tab with a "Mark Paid" button
4. Click "Mark Paid" on that row → it flips to `CONFIRMED` in place (the Paid date replaces the button)
5. Fill Course → remaining seats fill up with `PENDING_PAYMENT` rows
6. Add one more student → lands on **Waitlist** (Capacity)

**Expected:** each tab reflects the current state at every step; tab counts are consistent with row counts; the courses-list role distribution updates after step 1 and again after step 2.

---

## Multi-Tenant Isolation

**Business case:** Each school's data is completely separate. An owner should never see another school's students, courses, or enrollments.

**How to test:** Log in as Owner 1, note the courses and students. Log out, log in as Owner 2. Verify completely different data.

**Expected:** Each owner sees only their own school's courses, students, and enrollments. No cross-school data leakage.

---

## Dev Tools Restrictions and Quirks

- **Fill Course / Add Student on non-BEGINNER courses** — generated students have no dance-level records, so the backend routes their enrollments to `PENDING_APPROVAL` instead of `PENDING_PAYMENT`. The "Enrolled X students" snack bar message still says "Enrolled" — the rows actually land in the Approve tab.
- **Role selector is always visible** — the Leader/Follower dropdown also appears for SOLO courses. The value is ignored; the dropdown is harmless but can confuse.
- **Create Imbalance** only appears for PARTNER courses. It enrolls enough students of the selected role to exceed the threshold and produce at least one `WAITLISTED (ROLE_IMBALANCE)` row.
- **No waitlist auto-promotion** — there is no UI or backend job that moves a waitlisted student into `PENDING_PAYMENT` when a seat frees up. This is known / intentional for now.
