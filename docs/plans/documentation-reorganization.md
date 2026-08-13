# Documentation Reorganization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将项目文档整理为一个入口和四个职责明确的子目录，并保证所有 Markdown 链接有效。

**Architecture:** `docs/README.md` 作为阅读入口；需求、设计、计划和测试材料分别放入 `requirements/`、`design/`、`plans/`、`testing/`。只移动和修复文档引用，不修改应用代码。

**Tech Stack:** Markdown、Git、shell。

---

### Task 1: Move documentation

**Files:**
- Move: `docs/AI_DELIVERY.md` → `docs/README.md`
- Move: `docs/REQUIREMENTS.md` → `docs/requirements/REQUIREMENTS.md`
- Move: `docs/superpowers/specs/2026-08-13-scientific-calculator-design.md` → `docs/design/scientific-calculator-design.md`
- Move: `docs/superpowers/plans/2026-08-13-scientific-calculator-implementation.md` → `docs/plans/scientific-calculator-implementation.md`
- Move: `docs/CURL_TEST_CASES.md` → `docs/testing/CURL_TEST_CASES.md`
- Move: `docs/TEST_EVIDENCE.md` → `docs/testing/TEST_EVIDENCE.md`

- [x] **Step 1:** 创建四个目标目录并移动文件。
- [x] **Step 2:** 将本计划移动到 `docs/plans/documentation-reorganization.md`。

### Task 2: Repair navigation

**Files:**
- Modify: `docs/README.md`
- Modify: `docs/requirements/REQUIREMENTS.md`
- Modify: `docs/design/scientific-calculator-design.md`
- Modify: `docs/plans/scientific-calculator-implementation.md`
- Modify: `README.md`

- [x] **Step 1:** 在 `docs/README.md` 添加可点击的文档导航。
- [x] **Step 2:** 更新全部旧路径引用和目录树示例。
- [x] **Step 3:** 检查 Markdown 引用不存在旧路径或断链。

### Task 3: Verify

- [x] **Step 1:** 执行 `git diff --check`，预期无输出、退出码为 0。
- [x] **Step 2:** 枚举 `docs/` 文件，确认结构与设计一致。
- [x] **Step 3:** 检查所有相对 Markdown 链接指向现存文件。
