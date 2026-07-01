import { useState } from "react"
import { Fragment } from "react/jsx-runtime"
import { format } from "date-fns"
import {
  ArrowLeft,
  MoreVertical,
  Edit,
  Paperclip,
  Phone,
  ImagePlus,
  Plus,
  Search as SearchIcon,
  Send,
  Video,
  MessagesSquare,
} from "lucide-react"
import { cn, getDisplayNameInitials } from "@/lib/utils"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card"
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from "@/components/ui/input-group"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"
import { ConfigDrawer } from "@/components/config-drawer"
import { LanguageSwitch } from "@/components/language-switch"
import { Header } from "@/components/layout/header"
import { Main } from "@/components/layout/main"
import { ProfileDropdown } from "@/components/profile-dropdown"
import { Search } from "@/components/search"
import { ThemeSwitch } from "@/components/theme-switch"
import { NewChat } from "./components/new-chat"
import { type ChatUser, type Convo } from "./data/chat-types"
// Fake Data
import { conversations } from "./data/convo.json"

export function Chats() {
  const [search, setSearch] = useState("")
  const [selectedUser, setSelectedUser] = useState<ChatUser | null>(null)
  const [mobileSelectedUser, setMobileSelectedUser] = useState<ChatUser | null>(
    null
  )
  const [createConversationDialogOpened, setCreateConversationDialog] =
    useState(false)

  // Filtered data based on the search query
  const filteredChatList = conversations.filter(({ fullName }) =>
    fullName.toLowerCase().includes(search.trim().toLowerCase())
  )

  const currentMessage = selectedUser?.messages.reduce(
    (acc: Record<string, Convo[]>, obj) => {
      const key = format(obj.timestamp, "d MMM, yyyy")

      // Create an array for the category if it doesn't exist
      if (!acc[key]) {
        acc[key] = []
      }

      // Push the current object to the array
      acc[key].push(obj)

      return acc
    },
    {}
  )

  const users = conversations.map(
    ({ id, profile, username, fullName, title }) => ({
      id,
      profile,
      username,
      fullName,
      title,
    })
  )

  return (
    <>
      {/* ===== Top Heading ===== */}
      <Header>
        <Search className="me-auto" />
        <LanguageSwitch />
        <ThemeSwitch />
        <ConfigDrawer />
        <ProfileDropdown />
      </Header>

      <Main fixed>
        <section className="flex h-full gap-6">
          {/* Left Side */}
          <div className="flex w-full flex-col gap-2 sm:w-56 lg:w-72 2xl:w-80">
            <div className="sticky top-0 z-10 -mx-4 border-b bg-background px-4 pb-3 sm:static sm:z-auto sm:mx-0 sm:border-b-0 sm:p-0">
              <div className="flex items-center justify-between py-2">
                <div className="flex gap-2">
                  <h1 className="text-2xl font-bold">Inbox</h1>
                  <MessagesSquare className="size-5" />
                </div>

                <Button
                  size="icon-sm"
                  variant="ghost"
                  onClick={() => setCreateConversationDialog(true)}
                >
                  <Edit />
                  <span className="sr-only">New message</span>
                </Button>
              </div>

              <InputGroup>
                <InputGroupAddon>
                  <SearchIcon />
                </InputGroupAddon>
                <InputGroupInput
                  type="text"
                  placeholder="Search chat..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              </InputGroup>
            </div>

            <ScrollArea className="-mx-3 h-full p-3">
              {filteredChatList.map((chatUsr) => {
                const { id, profile, username, messages, fullName } = chatUsr
                const lastConvo = messages[0]
                const lastMsg =
                  lastConvo.sender === "You"
                    ? `You: ${lastConvo.message}`
                    : lastConvo.message
                return (
                  <Fragment key={id}>
                    <button
                      type="button"
                      className={cn(
                        "group hover:bg-accent hover:text-accent-foreground",
                        "flex w-full rounded-md px-2 py-2 text-start text-sm",
                        selectedUser?.id === id && "sm:bg-muted"
                      )}
                      onClick={() => {
                        setSelectedUser(chatUsr)
                        setMobileSelectedUser(chatUsr)
                      }}
                    >
                      <div className="flex gap-2">
                        <Avatar>
                          <AvatarImage src={profile} alt={username} />
                          <AvatarFallback>
                            {getDisplayNameInitials(fullName)}
                          </AvatarFallback>
                        </Avatar>
                        <div>
                          <span className="col-start-2 row-span-2 font-medium">
                            {fullName}
                          </span>
                          <span className="col-start-2 row-span-2 row-start-2 line-clamp-2 text-muted-foreground group-hover:text-accent-foreground/90">
                            {lastMsg}
                          </span>
                        </div>
                      </div>
                    </button>
                    <Separator className="my-1" />
                  </Fragment>
                )
              })}
            </ScrollArea>
          </div>

          {/* Right Side */}
          {selectedUser ? (
            <Card
              className={cn(
                "absolute inset-0 start-full z-50 hidden w-full flex-1 flex-col gap-0 p-0 sm:static sm:z-auto sm:flex",
                mobileSelectedUser && "inset-s-0 flex"
              )}
            >
              {/* Top Part */}
              <CardHeader className="flex flex-none flex-row items-center justify-between gap-4 border-b p-4">
                {/* Left */}
                <div className="flex gap-3">
                  <Button
                    size="icon-sm"
                    variant="ghost"
                    className="-ms-2 sm:hidden"
                    onClick={() => setMobileSelectedUser(null)}
                  >
                    <ArrowLeft className="rtl:rotate-180" />
                    <span className="sr-only">Back</span>
                  </Button>
                  <div className="flex items-center gap-2 lg:gap-4">
                    <Avatar size="lg">
                      <AvatarImage
                        src={selectedUser.profile}
                        alt={selectedUser.username}
                      />
                      <AvatarFallback>
                        {getDisplayNameInitials(selectedUser.fullName)}
                      </AvatarFallback>
                    </Avatar>
                    <div>
                      <span className="col-start-2 row-span-2 text-sm font-medium lg:text-base">
                        {selectedUser.fullName}
                      </span>
                      <span className="col-start-2 row-span-2 row-start-2 block max-w-32 truncate text-xs text-muted-foreground lg:max-w-none lg:text-sm">
                        {selectedUser.title}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Right */}
                <div className="-me-1 flex items-center gap-1 lg:gap-2">
                  <Button
                    size="icon-sm"
                    variant="ghost"
                    className="hidden sm:inline-flex"
                  >
                    <Video />
                    <span className="sr-only">Start video call</span>
                  </Button>
                  <Button
                    size="icon-sm"
                    variant="ghost"
                    className="hidden sm:inline-flex"
                  >
                    <Phone />
                    <span className="sr-only">Start call</span>
                  </Button>
                  <Button size="icon-sm" variant="ghost">
                    <MoreVertical />
                    <span className="sr-only">More actions</span>
                  </Button>
                </div>
              </CardHeader>

              {/* Conversation */}
              <CardContent className="flex min-h-0 flex-1 flex-col px-4">
                <div className="flex min-h-0 flex-1">
                  <div className="chat-text-container relative -me-4 flex flex-1 flex-col overflow-y-hidden">
                    <div className="chat-flex flex h-40 w-full grow flex-col-reverse justify-start gap-4 overflow-y-auto py-2 pe-4 pb-4">
                      {currentMessage &&
                        Object.keys(currentMessage).map((key) => (
                          <Fragment key={key}>
                            {currentMessage[key].map((msg, index) => (
                              <div
                                key={`${msg.sender}-${msg.timestamp}-${index}`}
                                className={cn(
                                  "chat-box max-w-72 rounded-lg px-3 py-2 wrap-break-word",
                                  msg.sender === "You"
                                    ? "self-end bg-primary text-primary-foreground"
                                    : "self-start bg-muted"
                                )}
                              >
                                {msg.message}{" "}
                                <span
                                  className={cn(
                                    "mt-1 block text-xs font-light text-foreground/75 italic",
                                    msg.sender === "You" &&
                                      "text-end text-primary-foreground/85"
                                  )}
                                >
                                  {format(msg.timestamp, "h:mm a")}
                                </span>
                              </div>
                            ))}
                            <div className="text-center text-xs">{key}</div>
                          </Fragment>
                        ))}
                    </div>
                  </div>
                </div>
              </CardContent>
              <CardFooter className="gap-2">
                <form className="flex w-full gap-2">
                  <InputGroup className="flex-1">
                    <InputGroupAddon>
                      <InputGroupButton size="icon-xs" type="button">
                        <Plus />
                        <span className="sr-only">Add</span>
                      </InputGroupButton>
                      <InputGroupButton
                        size="icon-xs"
                        type="button"
                        className="hidden lg:inline-flex"
                      >
                        <ImagePlus />
                        <span className="sr-only">Attach image</span>
                      </InputGroupButton>
                      <InputGroupButton
                        size="icon-xs"
                        type="button"
                        className="hidden lg:inline-flex"
                      >
                        <Paperclip />
                        <span className="sr-only">Attach file</span>
                      </InputGroupButton>
                    </InputGroupAddon>
                    <InputGroupInput
                      type="text"
                      placeholder="Type your messages..."
                    />
                    <InputGroupAddon align="inline-end">
                      <InputGroupButton
                        size="icon-xs"
                        type="submit"
                        className="hidden sm:inline-flex"
                      >
                        <Send />
                        <span className="sr-only">Send</span>
                      </InputGroupButton>
                    </InputGroupAddon>
                  </InputGroup>
                  <Button className="sm:hidden">
                    <Send data-icon="inline-start" /> Send
                  </Button>
                </form>
              </CardFooter>
            </Card>
          ) : (
            <Card
              className={cn(
                "absolute inset-0 start-full z-50 hidden w-full flex-1 flex-col gap-0 p-0 sm:static sm:z-auto sm:flex"
              )}
            >
              <CardContent className="flex flex-1 p-0">
                <Empty className="border-0">
                  <EmptyHeader>
                    <EmptyMedia variant="icon">
                      <MessagesSquare />
                    </EmptyMedia>
                    <EmptyTitle>Your messages</EmptyTitle>
                    <EmptyDescription>
                      Send a message to start a chat.
                    </EmptyDescription>
                  </EmptyHeader>
                  <EmptyContent>
                    <Button onClick={() => setCreateConversationDialog(true)}>
                      Send message
                    </Button>
                  </EmptyContent>
                </Empty>
              </CardContent>
            </Card>
          )}
        </section>
        <NewChat
          users={users}
          onOpenChange={setCreateConversationDialog}
          open={createConversationDialogOpened}
        />
      </Main>
    </>
  )
}
