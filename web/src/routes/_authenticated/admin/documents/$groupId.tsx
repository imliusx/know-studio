import { createFileRoute } from "@tanstack/react-router"
import { KnowledgeBaseRoute } from "@/features/ddrag/knowledge-base-route"

export const Route = createFileRoute(
  "/_authenticated/admin/documents/$groupId"
)({
  component: KnowledgeBaseRoute,
})
