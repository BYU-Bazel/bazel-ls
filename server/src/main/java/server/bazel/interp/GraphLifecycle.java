package server.bazel.interp;

interface GraphLifecycle {
    void onStart();

    void onSync();

    void onFinish();
}
