# Contextual follow-up question design

## Architecture

`AgentOrchestrationService` owns a deterministic, bounded naming-follow-up
resolver before intent classification. It uses recent user messages already
loaded from `ConversationContext`; no additional provider call is introduced.

The original message remains persisted. A derived `ChatRequest` carries the
resolved message through classification, retrieval, evidence selection, and
generation. `ConversationContext.currentQuestion` identifies the original last
user message so model history removes it even when the derived message differs.

## Resolution Contract

1. Normalize spaces and terminal punctuation in the current message.
2. Match only `那/那么 + recognized naming subject + 呢` style follow-ups.
3. Find the previous user message, excluding the just-appended current message.
4. Require the previous message to contain `命名` and a recognized naming subject.
5. Replace that previous subject with the follow-up subject.
6. Otherwise return the original current message unchanged.

## Compatibility

- Ordinary full questions are unchanged.
- Ambiguous `这个` and unrelated short messages still use existing CLARIFY logic.
- The displayed and persisted user text remains `那常量呢`.
- Retrieval and model telemetry observe the resolved Knowledge question.

## Rollback

Remove the resolver and return to classifying `request.message()` directly. No
data or API migration is required.
