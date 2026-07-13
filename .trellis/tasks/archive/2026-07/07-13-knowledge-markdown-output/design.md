# Knowledge Markdown output design

## Architecture

The backend prompt remains the primary output contract. `knowledge-system.st`
will describe concrete CommonMark/GFM rules based on the archived legacy prompt,
and `AgentPromptCatalog` will expose the behavior as `knowledge-v3`.

The frontend renderer remains a defensive compatibility boundary. Before lexing
Markdown into memoized blocks, it will normalize only malformed line-start
hyphen list markers outside fenced code. This repairs already persisted answers
and protects the UI from occasional model formatting drift without changing SSE.

## Data Flow

1. The Knowledge prompt asks the model for valid Markdown.
2. Token events continue through the existing SSE path unchanged.
3. The accumulated message is passed to the Markdown component.
4. The renderer repairs `-text` to `- text` only at line start and outside fences.
5. `marked` and `react-markdown` parse the normalized CommonMark/GFM source.

## Compatibility

- Existing valid Markdown is unchanged.
- Fenced code contents and `---` horizontal rules are never rewritten.
- Persisted malformed answers become correctly rendered without data migration.
- Prompt-version telemetry changes from `knowledge-v2` to `knowledge-v3`.

## Trade-Offs

Prompt-only enforcement is nondeterministic, while buffering and rewriting the
complete backend response would weaken streaming. A strict prompt plus narrow
render-time compatibility normalization preserves streaming and gives stable UI
behavior.

## Rollback

Revert the prompt/version and the isolated Markdown normalization function. No
database or API rollback is required.
