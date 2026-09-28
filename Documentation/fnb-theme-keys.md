# F&B widget theme keys

The F&B ordering widgets in the testapp (`code/testapp/.../conciergetestapp/fnb/`) read these keys
from the `theme` block of theme.json. The SDK passes unrecognized `--` keys through as
`ConciergeTheme.tokens?.cssVariables` (see [Style Guide](style-guide.md#json-structure)), and
`FnbTheme` resolves them into typed values.

> **Status:** every default below is a **placeholder** derived from the host Concierge palette.
> Figma tokens are still to come. Adding them only changes values: set the keys in theme.json,
> or change the defaults in `FnbTheme.resolve`. Widget code doesn't need to change.

A missing or malformed value falls back to the default. Colors accept `#RGB`, `#RRGGBB`,
`#RRGGBBAA`, or `transparent`. Dimensions accept `12px`, `12dp`, or `12` (dp), from 0 to 1000.

## Colors

| Key | Controls | Placeholder default |
| --- | --- | --- |
| `--fnb-card-background-color` | Menu card background | `surface` |
| `--fnb-card-border-color` | Menu card border, Customize image, option rows, Required/Optional pills, quantity pill | `outline` |
| `--fnb-text-primary-color` | Titles, item names, prices | `onSurface` |
| `--fnb-text-secondary-color` | Metadata, hints, was-price | `onSurfaceVariant` |
| `--fnb-accent-color` | Selected tab, primary CTA, tile stepper buttons, radio, checkbox, selected option row border | `--button-primary-background`, else `primary` |
| `--fnb-on-accent-color` | Text and glyphs drawn on the accent color | `--button-primary-text`, else `onPrimary` |
| `--fnb-disabled-color` | Disabled or muted CTA and stepper background | `--button-disabled-background`, else `outline` |
| `--fnb-tab-background-color` | Unselected category tab | `container` |
| `--fnb-tab-text-color` | Unselected category tab text | `onSurface` |
| `--fnb-tile-background-color` | Item tile background | `surface` |
| `--fnb-tile-image-placeholder-color` | Image placeholder or loading fill | `container` |
| `--fnb-tag-background-color` | Tag chip (e.g. "Popular", "21+") | `onSurface` |
| `--fnb-tag-text-color` | Tag chip text | `surface` |
| `--fnb-error-color` | Validation error: option row borders, group label, hint, pill, and message; submit failure text | `error` |
| `--fnb-status-online-color` | Venue status dot when ordering is available | `#2E7D32` |
| `--fnb-status-offline-color` | Venue status dot when paused or disabled | `onSurfaceVariant` |
| `--fnb-sheet-background-color` | Customize dialog background | `surface` |

## Dimensions

| Key | Controls | Placeholder default |
| --- | --- | --- |
| `--fnb-card-corner-radius` | Menu card and Customize dialog corners | `16px` |
| `--fnb-content-padding` | Inner padding of card, sections, and dialog | `12px` |
| `--fnb-grid-spacing` | Gap between tiles and rows | `12px` |
| `--fnb-tile-corner-radius` | Tile, Customize image, option row, and quantity pill corners | `12px` |
| `--fnb-tile-image-height` | Tile image height (Customize image is 1.8×) | `112px` |
| `--fnb-tab-corner-radius` | Category tab pill corners | `20px` |
| `--fnb-button-corner-radius` | Primary CTA corners | `24px` |
| `--fnb-button-height` | Primary CTA height | `48px` |
| `--fnb-stepper-size` | Stepper button size (touch target is always at least 48dp) | `32px` |

## Numbers

| Key | Controls | Placeholder default |
| --- | --- | --- |
| `--fnb-card-height-fraction` | Menu card height as a fraction of screen height, clamped to 0.3–0.9 | `0.65` |
