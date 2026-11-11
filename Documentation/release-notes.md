# Release notes

## Unreleased

### Message typography

Added optional message body size/weight and inline link weight/underline theme
overrides. Defaults preserve existing behavior. Invalid CSS sizes and weights
are ignored with a warning, including programmatic typography supplied to
`ConciergeTheme`. Link overrides also apply in lists and blockquotes;
Markdown-bold links retain at least bold weight. Explicit body line height is
preserved through response paragraph rendering; omitted line height retains
existing response spacing. Pixel line heights (for example `"24px"`) are absolute
scaled sizes rather than multipliers. Body paragraph line height no longer
overrides headings or fenced code, including nested blocks.
Response paragraphs and list items also supply their resolved body font size to
Compose so explicit line heights scale consistently at accessibility font sizes.

**Binary compatibility:** Adding parameters to the public `ConciergeTypography`
Kotlin data class, including `lineHeightPx` for absolute line height, changes its
constructor and generated `copy` signatures.
Default parameter values preserve ordinary source calls that omit the new
parameters, but do not preserve binary compatibility. Applications and libraries
compiled against the previous class must be recompiled when upgrading.
