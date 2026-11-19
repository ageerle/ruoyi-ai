package org.ruoyi.chat.service.coding.harness.runtime;

/** Fail-fast admission rejection used before a new run has any durable side effects. */
public final class HarnessSchedulerCapacityException extends RuntimeException {

    private final Scope scope;
    private final int limit;

    public HarnessSchedulerCapacityException(Scope scope, int limit) {
        super("Harness pending-run capacity reached for " + scope.label()
            + " (limit " + limit + ")");
        this.scope = scope;
        this.limit = limit;
    }

    public Scope scope() {
        return scope;
    }

    public int limit() {
        return limit;
    }

    public enum Scope {
        OWNER("owner"),
        TENANT("tenant"),
        GLOBAL("global scheduler");

        private final String label;

        Scope(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }
}
