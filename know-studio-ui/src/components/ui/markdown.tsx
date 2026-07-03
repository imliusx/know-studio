import { cn } from "@/lib/utils"
import { marked } from "marked"
import { memo, useId, useMemo } from "react"
import ReactMarkdown, { type Components } from "react-markdown"
import remarkBreaks from "remark-breaks"
import remarkGfm from "remark-gfm"
import { CodeBlock, CodeBlockCode } from "./code-block"

export type MarkdownProps = {
  children: string
  id?: string
  className?: string
  components?: Partial<Components>
}

function parseMarkdownIntoBlocks(markdown: string): string[] {
  const tokens = marked.lexer(markdown)
  return tokens.map((token) => token.raw)
}

function normalizeMarkdownCodeFences(markdown: string): string {
  return markdown
    .replace(/([^\n])```([A-Za-z][\w-]*)(?=\s|$)/g, "$1\n\n```$2")
    .replace(/(^|\n)(```[A-Za-z][\w-]*)[ \t]+(\S)/g, "$1$2\n$3")
    .replace(/([^\n`])```(?=\s*(?:\n|$))/g, "$1\n```")
}

function extractLanguage(className?: string): string {
  if (!className) return "plaintext"
  const match = className.match(/language-([\w-]+)/)
  return match ? match[1] : "plaintext"
}

function getTextContent(children: React.ReactNode): string | null {
  if (typeof children === "string" || typeof children === "number") {
    return String(children)
  }

  if (Array.isArray(children)) {
    const parts = children.map(getTextContent)
    return parts.every((part) => part !== null) ? parts.join("") : null
  }

  return null
}

function normalizeInlineCode(children: React.ReactNode) {
  const text = getTextContent(children)
  if (!text) return children

  const match = text.trim().match(/^(`+)([\s\S]*?)\1$/)
  return match?.[2] ? match[2] : children
}

const INITIAL_COMPONENTS: Partial<Components> = {
  code: function CodeComponent({ className, children, ...props }) {
    const isInline =
      !props.node?.position?.start.line ||
      props.node?.position?.start.line === props.node?.position?.end.line

    if (isInline) {
      return (
        <code
          className={cn(
            "rounded-md bg-muted px-1.5 py-0.5 font-mono text-sm font-normal text-foreground before:content-none after:content-none",
            className
          )}
          {...props}
        >
          {normalizeInlineCode(children)}
        </code>
      )
    }

    const language = extractLanguage(className)

    return (
      <CodeBlock className={className}>
        <CodeBlockCode code={children as string} language={language} />
      </CodeBlock>
    )
  },
  pre: function PreComponent({ children }) {
    return <>{children}</>
  },
}

const MemoizedMarkdownBlock = memo(
  function MarkdownBlock({
    content,
    components = INITIAL_COMPONENTS,
  }: {
    content: string
    components?: Partial<Components>
  }) {
    return (
      <ReactMarkdown
        remarkPlugins={[remarkGfm, remarkBreaks]}
        components={components}
      >
        {content}
      </ReactMarkdown>
    )
  },
  function propsAreEqual(prevProps, nextProps) {
    return prevProps.content === nextProps.content
  }
)

MemoizedMarkdownBlock.displayName = "MemoizedMarkdownBlock"

function MarkdownComponent({
  children,
  id,
  className,
  components = INITIAL_COMPONENTS,
}: MarkdownProps) {
  const generatedId = useId()
  const blockId = id ?? generatedId
  const normalizedChildren = useMemo(
    () => normalizeMarkdownCodeFences(children),
    [children]
  )
  const blocks = useMemo(
    () => parseMarkdownIntoBlocks(normalizedChildren),
    [normalizedChildren]
  )

  return (
    <div className={className}>
      {blocks.map((block, index) => (
        <MemoizedMarkdownBlock
          key={`${blockId}-block-${index}`}
          content={block}
          components={components}
        />
      ))}
    </div>
  )
}

const Markdown = memo(MarkdownComponent)
Markdown.displayName = "Markdown"

export { Markdown }
