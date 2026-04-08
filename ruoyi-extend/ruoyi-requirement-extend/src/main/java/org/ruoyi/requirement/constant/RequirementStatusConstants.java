package org.ruoyi.requirement.constant;

import java.util.Set;

public final class RequirementStatusConstants {

    private RequirementStatusConstants() {
    }

    public static final String DRAFT = "draft";
    public static final String REVIEWING = "reviewing";
    public static final String IN_PROGRESS = "in_progress";
    public static final String CLOSED = "closed";

    public static final Set<String> ALL = Set.of(DRAFT, REVIEWING, IN_PROGRESS, CLOSED);
}
