# Errors

Command failures and integration errors.

---

## [ERR-20260813-001] github-cli-search

**Logged**: 2026-08-13T00:00:00+08:00
**Priority**: low
**Status**: pending
**Area**: infra

### Summary
GitHub CLI is unavailable and unauthenticated GitHub Search API hit its rate limit.

### Error
`zsh: command not found: gh`; later API response: `API rate limit exceeded`.

### Context
- Attempted repository discovery for open-source Java Spring Boot calculator implementations.
- Raw GitHub file URLs and repository tree APIs remained usable for candidates already found.

### Suggested Fix
Use authenticated GitHub CLI/API for exhaustive searches in future; report this limitation when presenting results.

---

## [ERR-20260813-001] shell_search_backtick

**Logged**: 2026-08-13T00:00:00+08:00
**Priority**: low
**Status**: resolved
**Area**: docs

### Summary
A ripgrep pattern containing Markdown backticks was passed through a double-quoted shell string, so the shell attempted command substitution.

### Error
```
zsh:1: command not found: AtomicLong
```

### Context
- Audited the implementation plan for placeholder language and frozen product decisions.
- Backticks in the search pattern were interpreted by zsh before ripgrep ran.

### Suggested Fix
Use single-quoted shell patterns or avoid embedding Markdown backticks in shell search expressions.

### Metadata
- Reproducible: yes
- Related Files: docs/superpowers/plans/2026-08-13-scientific-calculator-implementation.md

---
