# BYU Bazel plugin for VSCode

A Bazel language server prototype.

## Features

- Diagnostics
    - Informs you of invalid label syntax.
    - Informs you of duplicate dependencies or targets. 
- Codelens
    - Build or test targets.
    - Can be turned off via the settings.
- Document links
    - Quickly navigate from BUILD files to their dependencies.
- Autocomplete
    - Autocomplete labels.
- Formatting
    - Format BUILD files.
- Commands
    - bazel.build - Builds a target
    - bazel.test - Tests a target
    - bazel.syncServer - Syncs the current state of a Bazel project. Must be called when the project state changes for all features to work correctly.
- Settings
    - Use the settings to configure the extension.
