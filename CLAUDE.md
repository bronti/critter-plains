This file is Claude Code's entry point for this repo, ported from an earlier Continue-based setup (`.continue/`, now retired).

@AGENTS.md

@.claude/context/status.md

@.claude/context/patterns.md

## Workflow rules

- Use `.\scripts\compile-check.ps1` after edits — fast (~3-4s), errors only.
- Use `.\scripts\build-quiet.ps1 [tasks]` for a full build, or `.\scripts\build-quiet.ps1 test` to run tests.
- Never run `gradlew.bat` directly — its output is too verbose.
- Bash commands are logged automatically by a `PostToolUse` hook (`.claude/hooks/log-bash.ps1`) — there's no manual logging step to remember.
- Session boundaries and log analysis are automatic too: `SessionStart`/`SessionEnd` hooks mark each session, and at the end of a session, shell-error corrections get folded into `.claude/context/patterns.md` and a summary into `.claude/context/status.md` for the next session to read (imported above).
- Kotlin 2.0, JVM 17. Match the style of surrounding code — no unnecessary blank lines, no trailing comments on closing braces.
