package server.bazel.interp;

import server.utils.Context;

public class SyncContext extends Context {
    private SyncContext() {
        super();
    }

    public static SyncContext empty() {
        return new SyncContext();
    }
}
