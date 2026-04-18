# Testing Workflows

Testable business workflows and their expected behaviors. Use Dev Tools to simulate student enrollment flows without manual data setup.

## Prerequisites

1. Start backend and frontend (see `CLAUDE.md` for commands)
2. Log in as **Owner 1** (`owner@test.com` / `password`) or **Owner 2** (`owner2@test.com` / `password`)
3. The database resets on every backend restart with fresh seed data

## Seed Data Overview

School 1 comes pre-loaded with 7 courses (mix of solo/partner, beginner through advanced, in various lifecycle stages), 7 students with different dance levels, and enrollments across two running courses. School 2 has 2 courses and 3 students with no enrollments. This gives you both populated and empty courses to test against.

---

## Enrolling a Student

**Business case:** A school owner registers a new student for a course.

**How to test:** Use Dev Tools to pick a course and add a student. For partner dance courses (e.g., Bachata), choose whether the student dances as Leader or Follower.

**Expected:**
- The student appears in the enrollment list with status "Pending Payment"
- Partner courses show the dance role; solo courses do not
- A full course rejects further enrollments
- Students without the required dance level are rejected from intermediate/advanced courses

---

## Filling a Course to Capacity

**Business case:** Simulate a fully booked course to test capacity limits and the enrollment experience at scale.

**How to test:** Use Dev Tools "Fill Course" on an empty beginner-level course.

**Expected:**
- Students are created and enrolled until the course reaches its max capacity
- Partner courses get an alternating mix of leaders and followers
- All enrollments start in "Pending Payment" status
- Attempting to fill an already-full course is rejected

**Limitation:** Fill Course only works reliably on beginner-level courses. Higher-level courses reject the generated students because they lack dance experience records.

---

## Confirming Payments

**Business case:** After students sign up, the school owner confirms that payment has been received.

**How to test (bulk via Dev Tools):** Select a course with pending payments and use "Simulate Payment" to confirm all at once.

**How to test (individual via Course Overview):** Open a course, go to the "Open Payment" tab, and mark individual students as paid.

**Expected:**
- Payment confirmation moves students from "Pending Payment" to "Confirmed"
- The enrollment tab counts update to reflect the change
- Confirmed students show a payment date

---

## Reviewing Enrollment Status by Course

**Business case:** A school owner wants to see who is enrolled, who is on the waitlist, who needs approval, and who still owes payment for a specific course.

**How to test:** Open any course from the Courses page and browse the enrollment tabs.

**Expected:**
- **Enrolled** — confirmed, paid students
- **Open Payment** — students awaiting payment, each with a "Mark Paid" action
- **Waitlist** — students waiting for a spot
- **Approve** — students awaiting manual approval
- Each tab shows a count badge
- Partner courses display each student's dance role; solo courses do not

---

## Full Enrollment Lifecycle (End-to-End)

**Business case:** Walk through the complete journey from sign-up to confirmed enrollment.

**How to test:**
1. Enroll a couple of students into an empty partner course (one leader, one follower)
2. Confirm their payments via Dev Tools or the Course Overview
3. Verify they appear as confirmed in the course's Enrolled tab
4. Enroll one more student without paying
5. Verify they appear under Open Payment and can be individually marked as paid

**Expected:** Each student progresses through: Enrollment (Pending Payment) → Payment Confirmed. The course overview reflects the current state at every step.

---

## Multi-Tenant Isolation

**Business case:** Each school's data is completely separate. An owner should never see another school's students, courses, or enrollments.

**How to test:** Log in as Owner 1, note the courses and students. Log out, log in as Owner 2. Verify completely different data.

**Expected:** Each owner sees only their own school's courses, students, and enrollments. No cross-school data leakage.

---

## Dev Tools Restrictions

- **Fill Course / Add Student only works for beginner-level courses.** Dev Tools generates students without dance level records, so the enrollment API correctly rejects them from intermediate, advanced, and masterclass courses.
- **Role selector is always visible.** The Leader/Follower dropdown appears even for solo courses. It is harmless (the value is ignored), but may be confusing.
