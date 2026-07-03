import { cn } from "@/lib/utils"
import { CheckIcon, CopyIcon } from "lucide-react"
import React, { useEffect, useMemo, useState } from "react"
import { codeToHtml } from "shiki"
import ayuDark from "shiki/themes/ayu-dark.mjs"
import ayuLight from "shiki/themes/ayu-light.mjs"
import { useTheme } from "@/context/theme-provider"
import { Button } from "@/components/ui/button"

const ayuLightTokenBackground = {
  ...ayuLight,
  colorReplacements: {
    "#f8f9fa": "transparent",
  },
}

const ayuDarkTokenBackground = {
  ...ayuDark,
  colorReplacements: {
    "#0d1017": "transparent",
  },
}

function normalizeCodeIndent(code: string) {
  let lines = code.replace(/\r\n?/g, "\n").split("\n")

  while (lines[0]?.trim() === "") lines = lines.slice(1)
  while (lines.at(-1)?.trim() === "") lines = lines.slice(0, -1)

  const indents = lines
    .filter((line) => line.trim() !== "")
    .map((line) => line.match(/^[ \t]*/)?.[0].length ?? 0)

  if (indents.length === 0) return ""

  const minIndent = Math.min(...indents)
  if (minIndent === 0) return lines.join("\n")

  return lines
    .map((line) => (line.trim() === "" ? "" : line.slice(minIndent)))
    .join("\n")
}

export type CodeBlockProps = {
  children?: React.ReactNode
  className?: string
} & React.HTMLProps<HTMLDivElement>

function CodeBlock({ children, className, ...props }: CodeBlockProps) {
  return (
    <div
      className={cn(
        "not-prose flex w-full flex-col overflow-clip border",
        "border-border bg-card text-card-foreground rounded-xl",
        className
      )}
      {...props}
    >
      {children}
    </div>
  )
}

export type CodeBlockCodeProps = {
  code: string
  language?: string
  theme?: string
  className?: string
} & React.HTMLProps<HTMLDivElement>

function CodeBlockCode({
  code,
  language = "tsx",
  theme,
  className,
  ...props
}: CodeBlockCodeProps) {
  const [highlightedHtml, setHighlightedHtml] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)
  const { resolvedTheme } = useTheme()
  const normalizedCode = useMemo(() => normalizeCodeIndent(code), [code])
  const shikiTheme =
    theme ??
    (resolvedTheme === "dark"
      ? ayuDarkTokenBackground
      : ayuLightTokenBackground)

  useEffect(() => {
    async function highlight() {
      if (!normalizedCode) {
        setHighlightedHtml("<pre><code></code></pre>")
        return
      }

      const html = await codeToHtml(normalizedCode, {
        lang: language,
        theme: shikiTheme,
      })
      setHighlightedHtml(html)
    }
    highlight()
  }, [normalizedCode, language, shikiTheme])

  useEffect(() => {
    setCopied(false)
  }, [normalizedCode])

  async function handleCopyCode(event: React.MouseEvent<HTMLButtonElement>) {
    event.stopPropagation()

    if (!normalizedCode) return

    await navigator.clipboard.writeText(normalizedCode)
    setCopied(true)
    window.setTimeout(() => setCopied(false), 1200)
  }

  const classNames = cn(
    "relative w-full overflow-x-auto text-[13px] [&_pre]:pt-10 [&_pre]:pr-14 [&_pre]:pb-4 [&_pre]:pl-6",
    className
  )

  // SSR fallback: render plain code if not hydrated yet
  return highlightedHtml ? (
    <div className={classNames} {...props}>
      <CodeLanguageBadge language={language} />
      <CopyCodeButton copied={copied} onClick={handleCopyCode} />
      <div dangerouslySetInnerHTML={{ __html: highlightedHtml }} />
    </div>
  ) : (
    <div className={classNames} {...props}>
      <CodeLanguageBadge language={language} />
      <CopyCodeButton copied={copied} onClick={handleCopyCode} />
      <pre>
        <code>{normalizedCode}</code>
      </pre>
    </div>
  )
}

function CodeLanguageBadge({ language }: { language: string }) {
  return (
    <span
      className="absolute top-2 left-4 z-10 font-mono text-[11px] text-muted-foreground uppercase"
    >
      {language || "text"}
    </span>
  )
}

function CopyCodeButton({
  copied,
  onClick,
}: {
  copied: boolean
  onClick: React.MouseEventHandler<HTMLButtonElement>
}) {
  return (
    <Button
      type="button"
      variant="ghost"
      size="icon-sm"
      className="absolute top-2 right-2 z-10 text-muted-foreground"
      onClick={onClick}
      aria-label={copied ? "已复制代码" : "复制代码"}
      title={copied ? "已复制代码" : "复制代码"}
    >
      {copied ? <CheckIcon /> : <CopyIcon />}
    </Button>
  )
}

export type CodeBlockGroupProps = React.HTMLAttributes<HTMLDivElement>

function CodeBlockGroup({
  children,
  className,
  ...props
}: CodeBlockGroupProps) {
  return (
    <div
      className={cn("flex items-center justify-between", className)}
      {...props}
    >
      {children}
    </div>
  )
}

export { CodeBlockGroup, CodeBlockCode, CodeBlock }
