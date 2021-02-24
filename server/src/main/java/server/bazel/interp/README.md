# Work In Progress

This folder will eventually be for bazel interpretting. A few of the items in this
folder are redundant of what's defined in the server.bazel.tree package, that is 
intentional to minimize conflicting changes with existing code. Pls no touch unless 
you're working on the bazel interpretting issue.

## TODO

- ~~Create empty graph~~
- Add file
    - Don't to any sort of syncing, simply add the node
    - Create a specific node type based on which file type the path extends
- Remove file node
- Test out an example of this
- Sync with files
    - If workspace file, go through and parse
        - Find all repository* imports and treat them accordingly
        - Construct nodes based on these repositories
        - Locate the import in the bazel tree in the external folder
        - Branch off of the nodes, declaring available rules for each import
        - These can be hooked into by other nodes
- Check if node is in graph
- Get node in graph by id
- Get node in graph by id as specific type
