import { createFileRoute } from '@tanstack/react-router'
import { ChatHome } from '@/features/chat/chat-home'

export const Route = createFileRoute('/_authenticated/')({
  component: ChatHome,
})
