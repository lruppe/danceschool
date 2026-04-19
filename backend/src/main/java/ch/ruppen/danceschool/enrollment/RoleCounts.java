package ch.ruppen.danceschool.enrollment;

public record RoleCounts(int leads, int follows) {

    public static final RoleCounts EMPTY = new RoleCounts(0, 0);

    RoleCounts plus(DanceRole role, int count) {
        if (role == DanceRole.LEAD) return new RoleCounts(leads + count, follows);
        if (role == DanceRole.FOLLOW) return new RoleCounts(leads, follows + count);
        return this;
    }
}
