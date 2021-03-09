load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "codelens",
    srcs = [
        "CodeLensProvider.java",
        "CodeLensResolver.java",
        "CommandConstants.java",
    ],
    deps = [
        "//server/src/main/java/server/utils",
        "//third_party/java:guava",
        "//third_party/java:log4j",
        "//third_party/java:lsp4j",
    ],
)