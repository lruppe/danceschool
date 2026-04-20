package ch.ruppen.danceschool.shared.error;

import java.util.List;

public class CourseEditPolicyViolationException extends RuntimeException {

    private final String tier;
    private final List<String> rejectedFields;

    public CourseEditPolicyViolationException(String tier, List<String> rejectedFields) {
        super("Course edit not allowed for fields: " + rejectedFields);
        this.tier = tier;
        this.rejectedFields = List.copyOf(rejectedFields);
    }

    public String getTier() {
        return tier;
    }

    public List<String> getRejectedFields() {
        return rejectedFields;
    }
}
