import { useParams } from '@tanstack/react-router'
import { KnowledgeBaseDocumentsPage } from './documents'

export function KnowledgeBaseRoute() {
  const { groupId } = useParams({
    from: '/_authenticated/admin/documents/$groupId',
  })

  return <KnowledgeBaseDocumentsPage groupId={groupId} />
}
