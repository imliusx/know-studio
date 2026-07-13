import { createFileRoute } from '@tanstack/react-router'
import { DocumentsPage } from '@/features/ddrag/documents'

export const Route = createFileRoute('/_authenticated/admin/documents/')({
  component: DocumentsPage,
})
