# Comments, docs and API changes

**Comments describe the code as it stands.**

- Written only when the code can't say it; public APIs get a one-line KDoc where the name isn't enough.
- Short and stable: a comment changes only when the behavior it describes changes.
- No history and no process: nothing about earlier behavior, a fix, a request or a session. A reader should not be able to tell what prompt produced the code. The source check flags history phrasing in comments.

**Markdown is small and stable.**

- One topic per file, at most 150 lines. Design docs describe current behavior only, like comments.
- No changelogs, progress logs, dates or session notes in Markdown; git history is the history.
- A change edits the one file for its topic, if any. A pull request that rewrites large parts of Markdown needs a reason.
- Reference docs (schemas, the plugin API, the MCP tools) are generated from code and never edited by hand.

**Remove, never deprecate.** When an API changes, every caller in the repo changes in the same pull request and the old API is deleted. No `@Deprecated`, no compatibility shims. Versioned surfaces outside the repo (save files, the plugin API, the network protocol) evolve through explicit version numbers and migrations instead.
