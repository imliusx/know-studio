import { createFileRoute } from "@tanstack/react-router"
import { KnowledgeBaseDocumentsPage } from "@/features/ddrag/documents"

export const Route = createFileRoute(
  "/_authenticated/admin/documents/$groupId"
)({
  component: RouteComponent,
})

function RouteComponent() {
  const { groupId } = Route.useParams()

  return <KnowledgeBaseDocumentsPage groupId={Number(groupId)} />
}
