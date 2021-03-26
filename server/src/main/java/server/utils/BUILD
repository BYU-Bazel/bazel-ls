load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "utils",
    srcs = [
        "Callbacks.java",
        "DocumentTracker.java",
        "FileRepository.java",
        "Logging.java",
        "Nullability.java",
        "Observatory.java",
        "Positions.java",
        "StarlarkWizard.java",
    ],
    deps = [
        "//third_party/java:guava",
        "//third_party/java:log4j",
        "//third_party/java:lsp4j",
        "//third_party/java:starlark",
    ],
)
