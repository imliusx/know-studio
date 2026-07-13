export function normalizeMarkdownLists(markdown: string): string {
  let inFence = false

  return markdown
    .split("\n")
    .map((line) => {
      if (line.trimStart().startsWith("```")) {
        inFence = !inFence
        return line
      }

      if (inFence || /^\s{0,3}-{3,}\s*$/.test(line)) {
        return line
      }

      return line.replace(/^(\s{0,3})-([^\s\d-])/, "$1- $2")
    })
    .join("\n")
}
