load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "interp",
    srcs = [
        "CompatabilityUtility.java",
        "FilePosition.java",
        "FileRange.java",
        "InterpException.java",
        "Label.java",
        "LabelName.java",
        "LabelPart.java",
        "LabelPkg.java",
        "LabelSyntaxException.java",
        "LabelWorkspace.java",
    ],
    deps = [
        "//server/src/main/java/server/bazel/tree",
        "//server/src/main/java/server/utils",
        "//server/src/main/java/server/workspace",
        "//third_party/java:guava",
        "//third_party/java:log4j",
    ],
    visibility = [
        "//server/src/main/java/server/diagnostics:__subpackages__",
        "//server/src/test/java/server/bazel/interp:__subpackages__",
    ]
)
