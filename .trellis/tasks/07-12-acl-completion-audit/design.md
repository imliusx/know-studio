# ACL Completion Audit Design

## Frontend Access Projection

`AppSidebar` reuses cached `teams` and `knowledge-bases` queries. System ADMIN
sees all administration entries. Team Admin sees Team administration. A user
with an effective MANAGE KnowledgeBase sees documents and evaluation entries.
Ordinary members do not see administration entries.

## Citation Contract

Agent citation events and persisted metadata use one wire shape:

```text
knowledgeBaseId, documentId, chunkId, chunkIndex, fileName, score, snippet
```

The frontend API boundary normalizes this shape and tolerates the earlier
`text` field when restoring already persisted messages.

## Protected Document Content

`KnowledgeQueryService` authorizes the KnowledgeBase, loads the document by
both IDs and opens the MinIO object. The controller streams it as an attachment.
The frontend downloads through Axios with the Bearer token and creates a
temporary object URL; citations never link directly to MinIO.

## Focused Answer Grounding

Single-intent questions keep the normalized original query instead of invoking
LLM query expansion. Retrieval reranks individual seeds before neighbor
expansion, and clustering is bounded to an anchor plus immediate neighbors so
one document cannot collapse into a chapter-sized evidence item. Generation
receives only focused, question-relevant windows. Explicit naming-rule
questions use an extractive answer when the evidence contains a matching
mandatory/recommended rule, preserving the source wording and citations.
