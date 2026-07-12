# Workspace Target Ownership Matrix

| Current scope | Target ownership | Notes |
|---|---|---|
| workspaces | teams + default knowledge_bases | Transitional one-to-one data mapping only |
| workspace_members | team_members | OWNER/ADMIN -> TEAM_ADMIN |
| documents | knowledge_base_id | Managed by KB MANAGE grant |
| document_chunks | knowledge_base_id | Read path must verify KB access |
| chunk_embeddings | knowledge_base_id | Vector query always filters allowed KB IDs |
| ingestion_jobs | knowledge_base_id | Async job carries explicit KB ID |
| upload_sessions | knowledge_base_id | Upload requires KB MANAGE |
| sessions | user_id | No Team/KB ownership |
| messages | session -> user | Store citations/used KBs in metadata |
| session_memory | session -> user | No Team/KB ownership |
| eval_datasets | knowledge_base_id | KB MANAGE required |
| eval_samples | dataset -> knowledge_base | No independent scope widening |
| eval_runs | dataset -> knowledge_base | KB MANAGE required |
| frontend workspace store | remove | Admin pages select KB locally |
| frontend TeamSwitcher | remove from Chat context | Team management is an admin feature |

## Local V5 Baseline (2026-07-12)

```text
users=8
workspaces=6
workspace_members=10
documents=2
chunks=2
sessions=4
messages=9
eval_datasets=3
eval_samples=2
eval_runs=9
```
