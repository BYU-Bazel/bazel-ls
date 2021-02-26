# Work In Progress

This folder will eventually be for bazel interpretting. A few of the items in this
folder are redundant of what's defined in the server.bazel.tree package, that is 
intentional to minimize conflicting changes with existing code. Pls no touch unless 
you're working on the bazel interpretting issue.

## Goal

Lazily build a dependency graph resembling a Bazel project's anatomy.

## Test Program

There is a test program to speed up development iteration. Use the following
command to run the test program.

```
./server/src/main/java/server/bazel/interp/test.sh
```

## Notes

- If you change a file in a source graph, the only thing that could change would
be the files that depend on it (e.g. inverse deps). The forward dependencies will
not be effected.

## TODO

- ~~Create empty graph~~
- ~~Add file~~
    - ~~Don't to any sort of syncing, simply add the node~~
    - ~~Create a specific node type based on which file type the path extends~~
- Create main file for testing the graph stuff
- Sync with files
    - If workspace file, go through and parse
        - Create nodes based on what was parse from the workspace file
        - Create edges associated between those nodes
- Move the source file declaraion of a label to reside in target name
- ~~Check if node is in graph~~
- Get node in graph by id
- Get node in graph by id as specific type
- Remove file node
- Test out an example of this
