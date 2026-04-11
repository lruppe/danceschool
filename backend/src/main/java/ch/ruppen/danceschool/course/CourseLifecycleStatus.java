package ch.ruppen.danceschool.course;

/**
 * Derived lifecycle status for a course, computed from date fields.
 * Never persisted — always derived at read time.
 */
public enum CourseLifecycleStatus {
    DRAFT,
    OPEN,
    RUNNING,
    FINISHED
}
