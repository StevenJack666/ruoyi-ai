package org.ruoyi.requirement.constant;

import java.util.Set;

public final class ProjectBugStatusConstants {

    private ProjectBugStatusConstants() {
    }

    public static final String OPEN = "open";
    public static final String IN_PROGRESS = "in_progress";
    public static final String RESOLVED = "resolved";
    public static final String CLOSED = "closed";

    public static final Set<String> ALL = Set.of(OPEN, IN_PROGRESS, RESOLVED, CLOSED);
}
