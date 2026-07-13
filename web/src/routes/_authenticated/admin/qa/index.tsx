import { createFileRoute } from '@tanstack/react-router'
import { QaPage } from '@/features/ddrag/qa'

export const Route = createFileRoute('/_authenticated/admin/qa/')({
  component: QaPage,
})
