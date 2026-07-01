import { cn } from "@/lib/utils"
import { StickToBottom, useStickToBottomContext } from "use-stick-to-bottom"

export type ChatContainerRootProps = {
  children: React.ReactNode
  className?: string
} & React.HTMLAttributes<HTMLDivElement>

export type ChatContainerContentProps = {
  children: React.ReactNode
  className?: string
  scrollClassName?: string
  onScroll?: React.UIEventHandler<HTMLDivElement>
} & React.HTMLAttributes<HTMLDivElement>

export type ChatContainerScrollAnchorProps = {
  className?: string
  ref?: React.RefObject<HTMLDivElement>
} & React.HTMLAttributes<HTMLDivElement>

function ChatContainerRoot({
  children,
  className,
  ...props
}: ChatContainerRootProps) {
  return (
    <StickToBottom
      className={cn("flex overflow-y-auto", className)}
      resize="smooth"
      initial="instant"
      role="log"
      {...props}
    >
      {children}
    </StickToBottom>
  )
}

function ChatContainerContent({
  children,
  className,
  scrollClassName,
  onScroll,
  ...props
}: ChatContainerContentProps) {
  const context = useStickToBottomContext()

  return (
    <div
      // eslint-disable-next-line react-hooks/refs -- use-stick-to-bottom exposes DOM refs through context for custom scroll containers.
      ref={context.scrollRef}
      onScroll={onScroll}
      style={{
        height: "100%",
        width: "100%",
      }}
      className={scrollClassName}
    >
      <div
        // eslint-disable-next-line react-hooks/refs -- mirrors StickToBottom.Content while allowing scroll event binding.
        ref={context.contentRef}
        className={cn("flex w-full flex-col", className)}
        {...props}
      >
        {children}
      </div>
    </div>
  )
}

function ChatContainerScrollAnchor({
  className,
  ...props
}: ChatContainerScrollAnchorProps) {
  return (
    <div
      className={cn("h-px w-full shrink-0 scroll-mt-4", className)}
      aria-hidden="true"
      {...props}
    />
  )
}

export { ChatContainerRoot, ChatContainerContent, ChatContainerScrollAnchor }
