import assert from "node:assert/strict"
import test from "node:test"
import { normalizeMarkdownLists } from "../src/components/ui/markdown-normalization.ts"

test("repairs malformed hyphen list markers", () => {
  assert.equal(
    normalizeMarkdownLists("规则：\n\n-主键索引\n- 唯一索引"),
    "规则：\n\n- 主键索引\n- 唯一索引"
  )
})

test("preserves horizontal rules, negative numbers, and fenced code", () => {
  const markdown = "---\n\n-1\n\n```text\n-not a list\n```"

  assert.equal(normalizeMarkdownLists(markdown), markdown)
})
