---
name: wrap-session
description: Reconcile architecture docs (AGENTS.md, CLAUDE.md) against what actually changed this session, before closing. Complements the SessionEnd hook, which only regenerates status.md/patterns.md mechanically from shell-command logs and has no understanding of code semantics. Use when the user signals they're ending the session — "wrap up", "closing the session", "done for today", "let's stop here" — or invoke directly as /wrap-session.
---

## What this is for

`.claude/hooks/session-end.ps1` already runs automatically at session end, but it only does mechanical log parsing: it rewrites `patterns.md` from `shell-error → ok` command pairs and updates `status.md`'s "Last changed" section from `cmd.log` + `git log`. It never reads source code and has no idea what anything *means* — it cannot know a new class was added, a module boundary shifted, or a constant changed.

This skill fills that gap using the one thing the hook doesn't have: your actual understanding of what happened this session.

## What NOT to touch

- **`.claude/context/status.md`** and **`.claude/context/patterns.md`** — exclusively owned by `session-end.ps1`. Editing them here fights the hook; your changes get overwritten at session end anyway.
- **Source code** — this skill is documentation-only. If code needs fixing, that's a separate task.

## Steps

1. **Recall what changed this session** — from conversation context first (new files/classes, renamed types, changed constants, new scripts/hooks/skills, workflow changes), then verify against reality:
   ```powershell
   git status --short
   git diff --stat HEAD
   git log --oneline -15
   ```
   Conversation memory can miss things after a context compaction — the git commands are the backstop, not the primary source.

2. **Check each candidate doc for drift**, only where the session's changes actually touch it:
   - `AGENTS.md` — directory tree, module/file one-liners, the `Key types` / `Key constants` tables, the simulation/render loop diagrams. Triggers: a new class, a new file, a moved file, a changed constant, a changed signature that the doc describes.
   - `CLAUDE.md` — workflow rules. Triggers: a new script, a new hook, a new skill, a changed build/test command.
   - Any other checked-in doc the session's change set clearly affects.

   If nothing changed that any doc describes, say so and skip to step 4 — don't edit for the sake of editing.

3. **Make the edits**, matching each file's existing style and level of detail (see how `Chronicler` was documented in `AGENTS.md` as the reference example: directory entry, loop-diagram note, `Key types` row — not a full prose section).

4. **Log the work.** Append one line to `.claude/logs/cmd.log`, in the same shape the hooks already use, so this skill's activity is part of the same audit trail instead of a separate, easy-to-forget place:
   ```powershell
   $entry = [ordered]@{
       ts      = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss")
       kind    = "doc-update"
       files   = @("AGENTS.md")          # actual files touched, or @() if none
       summary = "Documented Chronicler class"   # short, or "no drift found"
   }
   $entry | ConvertTo-Json -Compress | Add-Content -Path ".claude\logs\cmd.log" -Encoding UTF8
   ```
   This entry uses a `kind` the `SessionEnd` hook's filters ignore (`ok`/`shell-error`/`build-failure`/`session-start`), so it's inert to existing parsing — purely an audit record.

5. **Compile-check is not needed** (docs only), but if you touched anything under `scripts/` while investigating, run `.\scripts\compile-check.ps1` anyway as a sanity check.

6. **Commit** the doc changes in their own small, focused commit(s) — per this project's working style, docs-only changes don't need to wait for a separate user request to commit (the user's global instructions already ask for small, frequent commits; this is squarely that). Never push.

7. **Report back** in one or two sentences: what was updated, or confirm nothing needed updating.
