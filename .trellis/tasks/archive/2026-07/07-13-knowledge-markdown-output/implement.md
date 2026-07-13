# Knowledge Markdown output implementation

1. Add explicit CommonMark/GFM output rules to the Knowledge system prompt.
2. Bump the Knowledge prompt version and update prompt catalog tests.
3. Add narrow malformed-hyphen-list normalization to the frontend Markdown
   preprocessing pipeline, excluding fenced code and horizontal rules.
4. Extend Playwright acceptance to assert semantic list rendering for the exact
   reported Knowledge question on desktop and mobile.
5. Update backend prompt-engineering and frontend integration specs.
6. Run backend tests, frontend lint/typecheck/build, Playwright, Docker config,
   and `git diff --check`.
