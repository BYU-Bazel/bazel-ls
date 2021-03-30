package(default_visibility = ["//visibility:public"])

py_binary(
    name = "main",
    srcs = [
        "main.py",
        "foo/path/to/folder/a.py",
        ":foo/path/to/folder/b.py",
    ],
    deps = [
        "//foo/path/to/package:package",
    ],
)

py_test(
    name = "test_case",
    srcs = [
        "//test:number_test.py",
    ],
    main = "//test:number_test.py",
)

