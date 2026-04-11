# Ubiquitous Language — Danceschool

DDD-style glossary. Canonical naming for code, APIs, and conversation.

| Term           | Definition                                                                                          |
|----------------|-----------------------------------------------------------------------------------------------------|
| **School**     | A dance school. The tenant boundary. All data (classes, members) belongs to a school.               |
| **Owner**      | The person who created/claimed a school. Has full administrative control. A role on `SchoolMember`.  |
| **Member**     | A user associated with a school in a specific role (Owner, Teacher, User). Modeled as `SchoolMember`. |
| **Teacher**    | A member role (future). Can manage classes but not school settings.                                  |
| **Class**      | A dance class offered by a school (e.g., "Salsa Beginners, Monday 19:00"). Created by Owner/Teacher.|
| **Student**    | An end user of the student-facing apps (future). Browses schools, views classes, enrolls.           |
| **PricingPlan** | A subscription product template defined by a school. Has a lifecycle: DRAFT → ACTIVE → ARCHIVED. Students purchase plans to get access to classes. |
| **Enrollment** | A student signing up for a class (future).                                                          |

## Bounded Contexts

| Context              | Audience          | Description                                      |
|----------------------|-------------------|--------------------------------------------------|
| **School Management** | Owners, Teachers | Admin portal — manage school, classes, members    |
| **Discovery**         | Students         | Student apps — browse schools, view classes, enroll |
| **Identity**          | All              | Authentication, user accounts, OAuth              |
