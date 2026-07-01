import { useState } from "react"
import { X } from "lucide-react"
import { getDisplayNameInitials } from "@/lib/utils"
import { showSubmittedData } from "@/lib/show-submitted-data"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { type ChatUser } from "../data/chat-types"

type User = Omit<ChatUser, "messages">

type NewChatProps = {
  users: User[]
  open: boolean
  onOpenChange: (open: boolean) => void
}
export function NewChat({ users, onOpenChange, open }: NewChatProps) {
  const [selectedUsers, setSelectedUsers] = useState<User[]>([])

  const handleSelectUser = (user: User) => {
    if (!selectedUsers.find((u) => u.id === user.id)) {
      setSelectedUsers([...selectedUsers, user])
    } else {
      handleRemoveUser(user.id)
    }
  }

  const handleRemoveUser = (userId: string) => {
    setSelectedUsers(selectedUsers.filter((user) => user.id !== userId))
  }

  const handleOpenChange = (newOpen: boolean) => {
    onOpenChange(newOpen)
    // Reset selected users when dialog closes
    if (!newOpen) {
      setSelectedUsers([])
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>New message</DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-sm text-muted-foreground">To:</span>
            {selectedUsers.map((user) => (
              <Badge key={user.id} asChild>
                <button
                  type="button"
                  aria-label={`Remove ${user.fullName}`}
                  onClick={() => handleRemoveUser(user.id)}
                >
                  {user.fullName}
                  <X data-icon="inline-end" />
                </button>
              </Badge>
            ))}
          </div>
          <Card className="p-0">
            <CardContent className="p-0">
              <Command>
                <CommandInput placeholder="Search people..." />
                <CommandList>
                  <CommandEmpty>No people found.</CommandEmpty>
                  <CommandGroup>
                    {users.map((user) => (
                      <CommandItem
                        key={user.id}
                        data-checked={selectedUsers.some(
                          (u) => u.id === user.id
                        )}
                        onSelect={() => handleSelectUser(user)}
                      >
                        <div className="flex items-center gap-2">
                          <Avatar>
                            <AvatarImage
                              src={user.profile}
                              alt={user.fullName}
                            />
                            <AvatarFallback>
                              {getDisplayNameInitials(user.fullName)}
                            </AvatarFallback>
                          </Avatar>
                          <div className="flex flex-col">
                            <span className="text-sm font-medium">
                              {user.fullName}
                            </span>
                            <span className="text-xs text-muted-foreground">
                              {user.username}
                            </span>
                          </div>
                        </div>
                      </CommandItem>
                    ))}
                  </CommandGroup>
                </CommandList>
              </Command>
            </CardContent>
          </Card>
          <Button
            onClick={() => showSubmittedData(selectedUsers)}
            disabled={selectedUsers.length === 0}
          >
            Chat
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
