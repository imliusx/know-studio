# ACL Completion Audit Plan

- [x] Add secure document-content service/controller contract and tests.
- [x] Complete citation SSE and persistence payload; normalize frontend restore.
- [x] Add citation download action and error feedback.
- [x] Make admin navigation role-aware for system admin, Team Admin and MEMBER.
- [x] Run backend/frontend gates and API authorization E2E.
- [x] Preserve Snowflake IDs as strings across REST, SSE, persisted citations and frontend state; verify a real chat round trip.
- [x] Restore the Chat sidebar brand header and keep KnowledgeBase switching out of the user Chat surface.
- [x] Use one static sidebar brand across Chat/Admin and move evaluation KnowledgeBase selection into the evaluation page.
- [x] Stabilize short factual QA with deterministic single-intent planning, bounded evidence clusters, focused grounding, extractive explicit-rule answers and Markdown heading normalization.
- [x] Route known short greetings to Chat before LLM classification and use heuristic fallback for low-confidence model output.
- [x] Load the document overview across all manageable KnowledgeBases and bind detail queries to the route KnowledgeBase ID.
- [x] Require lexical question-term coverage when reranking is unavailable and reject unrelated hybrid retrieval hits.
- [x] Route unscoped knowledge questions to a high-confidence readable KnowledgeBase subset, with secure low-confidence fallback.
- [x] Add bounded retrieval trace attributes and refusal-aware offline evaluation across backend, migration and admin UI.
- [ ] Run desktop/mobile browser screenshots and interaction checks. Blocked on 2026-07-12 after the approved browser runtime reported no available browser and discovery returned `[]`; backend and frontend health checks passed.
- [ ] Commit, archive task and update the completion audit record.
