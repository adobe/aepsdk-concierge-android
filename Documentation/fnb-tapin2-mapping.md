# F&B widgets: tapin2 → BCOS elements → widget mapping

How the testapp F&B widgets (`code/testapp/.../conciergetestapp/fnb/`) consume BCOS's F&B
multimodal elements, and where each field comes from in tapin2.

- **tapin2 → BCOS:** BCOS calls tapin2 and emits elements in `response.multimodalElements.elements[]`.
- **BCOS → widget:** every meaningful field is in the element's `entity_info`. `entityId` is the
  business entity: a product for catalog cards, the arena venue for the cart bar, the tapin2 order
  for the cart.
- **Fixtures** (`code/testapp/src/debug/assets/fnb/`):
  - BCOS samples as shared: `bcos_catalog_sample.json` and `bcos_cart_sample.json`.
  - Stage-derived, in the same BCOS shape, built from the real tapin2 responses:
    `bcos_catalog_stage.json` (33 products) and `bcos_cart_stage.json` (order 695685).
  - Raw tapin2 references: `tapin2_stage_raw.json` and `tapin2_stage_cart_add.json`.

## Element types

| `type` | `cardType` | Renderer | Widget |
|---|---|---|---|
| `catalogItemCard` | `catalogItem` | `MenuFnbRenderer` | One menu tile (with Customize sheet) |
| `cartBar` | `stickyFooter` | `MenuFnbRenderer` | The menu's ADD TO CART footer |
| `cartView` | `cartSummary` | `CartFnbRenderer` | The cart summary card |

The menu is one widget built from many cards: tabs, grid, a shared local cart, and the footer.
The SDK registry therefore has to hand a renderer **all consecutive elements it claims** from one
message, not one element at a time. `FnbRenderers.group` shows that dispatch.

## Menu: `catalogItemCard` → `MenuItem`

| `entity_info` field | tapin2 source (stage-derived fixture) | Widget field | Shown as |
|---|---|---|---|
| (`entityId`) | `product.id` | `MenuItem.id` | Keys; submit `productId` |
| `productName` | `product.title` | `name` | Tile name, Customize title |
| `productDescription` | `product.description` (HTML stripped) | `description` | Customize body (HTML stripped again on the client) |
| `productImageURL` | `product.imageUrl` | `imageUrl` | Tile and Customize image; https only, else a placeholder |
| `subtitle` | BCOS-authored (e.g. "Vegetarian · Shareable") | `metadata` (split on `·`) | Tile "A · B" line, Customize metadata |
| `tags[].label` | BCOS-authored | `tag` (first label) | Tag chip |
| `unitPrice.amount` / `.currency` | `product.eventPrice` (else `price`) | `priceCents`, `MenuUiModel.currencyCode` | Price; `display` is ignored, since the widget formats and sums from `amount` |
| `available` | entry/product active flags | `available` | `false` → dimmed "Sold out" tile, no add |
| `categoryId`, `categoryLabel` | `category.id`, `category.title` | `MenuCategory` | Tabs and sections, **in card order** (BCOS sorts by `category.orderId`, then entry `orderId`) |
| `venueId`, `eventId` | arena and event | `MenuUiModel.venueId` / `eventId` | Echoed on submit |
| `locationId`, `locationLabel` | `location.id`, `location.title` | `locationId`, `locationName` | Header; submit `locationId` |
| `action.type` | `INCREMENT` (no options) / `OPEN_SHEET` (has options) | `opensSheet` | `+` quick-adds for `INCREMENT`, opens Customize for `OPEN_SHEET` |
| `hasOptions` | `modifierGroups` non-empty | (fallback for `opensSheet`) | — |
| `sheet.optionGroups[].id`, `.title` | `modifierGroups[].id`, `.title` | `OptionGroup` | Section label |
| `sheet.optionGroups[].minSelections` / `maxSelections` | `minQuantity` / `maxQuantity` | `minSelect` / `maxSelect` / `required` | Required when min ≥ 1; max 1 → radios; max ≤ 0 → any number |
| `sheet.optionGroups[].maxPerOption` | 1 when `maxOnePerSelection` | `maxPerOption` | Only 1 is supported by the UI |
| `sheet.optionGroups[].options[]` `.id` / `.label` / `.priceDelta.amount` / `.available` | `modifiers[].id` / `.title` / `.priceDiff` / active flags | `OptionChoice` | Option rows, "(+$1.50)", "(Unavailable)" |
| `sheet.specialInstructions` `.enabled` / `.label` / `.placeholder` / `.maxLength` | BCOS config | `instructions` | Text field in Customize; → cart line `note` |
| `sheet.quantity` `.min` / `.max` / `.default` / `.step` | BCOS config | `quantity` (`QuantityRule`) | Customize stepper bounds, per-line cap (max 99) |
| `sheet.addToCart.label` | BCOS config (`STAGE_LINE`) | `addToCartLabel` | Customize CTA ("ADD TO ORDER") |

## Menu footer: `cartBar` → `CartBarConfig`

| `entity_info` field | Widget | Shown as |
|---|---|---|
| `label` | `cartBar.label` | "ADD TO CART" |
| `showCount` | `cartBar.showCount` | "(n)" suffix |
| `hideWhenEmpty` | `cartBar.hideWhenEmpty` | Button hidden while the cart is empty |
| `enabled` | `cartBar.enabled`, `MenuUiModel.orderingAvailable` | `false` disables adding and submit; the header shows "Ordering unavailable" |
| `venueId`, `eventId` | `MenuUiModel.venueId` / `eventId` | Echoed on submit |
| `action` | `SUBMIT_CART` | Tapping sends the submit turn below |

## Submit (`SUBMIT_CART`) → `add_to_cart`

ADD TO CART sends one user turn with `Concierge.sendMessage`: a readable summary plus an
`[ORDER_DETAILS]` JSON block shaped like the `add_to_cart` tool's items. Each item maps to a
tapin2 `cart/add` `products[]` entry.

```
[ORDER_DETAILS]
{"action":"SUBMIT_CART","submitId":"<uuid>","venueId":1000010528,"eventId":36747,"items":[
  {"locationId":19289,"productId":1364015,"quantity":1,
   "modifierGroups":[{"id":157322,"isMultiSelect":false,"modifiers":[{"id":581257,"isSelected":true}]}],
   "note":"light ice"}]}
[/ORDER_DETAILS]
```

- `submitId` is a client UUID per tap, not a tapin2 id. BC skips a repeated `submitId`, because
  re-adding under the same tapin2 order merges lines and would double the quantities.
- `venueId` and `eventId` come from the cards, so BC doesn't have to resolve them. The tapin2
  order id is BC state.
- Only selected modifiers are sent. `note` is the fan's text: JSON-escaped, single line,
  `[`/`]` stripped, at most 140 characters. Confirm that tapin2 `cart/add` accepts a per-product
  `note`; the response has `items[].note`.
- Ids are JSON numbers, and vendor names never appear in the JSON.

## Cart: `cartView` → `CartSummaryUiModel`

| `entity_info` field | tapin2 source (`cart/add`) | Widget field | Shown as |
|---|---|---|---|
| `cartId` (= `entityId`) | order `id` (**tapin2's plain order id**, despite the name) | `cartId` | Carried in Remove, Show more, and Checkout |
| `guid` | `guid` | `guid` | Not shown; the checkout URL needs it |
| `venue.name` | `venue.title` | `venueName` | "● Golden 1 Center" header |
| `lines[].lineId` | `items[].id` | `line.lineId` | The Remove key |
| `lines[].entityId` | `items[].product.id` | `line.productId` | — |
| `lines[].name`, `.quantity` | `items[].product.title`, `.quantity` | `title`, `quantity` | "Veggie Nachos × 1" |
| `lines[].modifiersSummary` | `items[].modifier` (or modifier names) | `modifiersSummary` | "Coke · …" / "No modifiers · …" |
| `lines[].location.id` / `.label` | `items[].locationId` → `distinctLocations[]` | `locationId`, `locationName` | "… · Local Eats 118". **One order spans stands** (confirmed by the sample) |
| `lines[].unitPrice`, `.lineTotal` | `items[].pricePer`, `.subtotal` | `pricePerCents`, `subtotalCents` | Line price |
| `lines[].actions.remove` | — | `removable` | Remove link (hidden when absent) |
| `lines[].actions.edit` (`EDIT_LINE`) | — | (ignored) | Not in the UX; quantity changes happen in chat |
| `totals.subtotal` / `.tax` / `.total` | `subtotalNet` / `taxAddedNet` / `totalNet` | `*Cents` | Subtotal, Tax, **Total** (`fees`/`discount` are shown if BCOS adds them) |
| `checkout.label` | — | `checkoutLabel` | "Proceed to checkout" |
| `checkout.url` → else `checkout.receiptUrl` | `receiptUrl` (see gaps) | `checkoutUrl` | Button shown only for https on `*.tapin2.co` |

### Cart actions

| Button | Action | Sent as |
|---|---|---|
| Remove | `RemoveCartItem(cartId, lineId, title)` | `sendMessage`: "Please remove Veggie Nachos from my order." + `[CART_ACTION]{"action":"REMOVE_LINE","cartId":679154,"lineId":1988604}[/CART_ACTION]`. The line shows "Removing…" until BC returns a new `cartView` |
| Show more restaurants | `ShowMoreRestaurants(cartId)` | `sendMessage`: "Show me more restaurants near my section. Keep my current order." + `[CART_ACTION]{"action":"SHOW_MORE_LOCATIONS","cartId":679154}` |
| Proceed to checkout (`PROCEED_TO_CHECKOUT`) | `Checkout(cartId, url)` | No turn. The host opens it through `ConciergeFnbActionHandler(openUrl = …)` after the allowlist check (a Custom Tab is recommended) |
| *(chat only)* | — | Quantity changes ("make that 2 sodas"): BC matches the line and calls `update_cart_item`. There's no stepper in the card by design |

## Gaps to raise with BCOS

1. **Checkout URL:** the sample uses tapin2's `receiptUrl`. tapin2 confirmed that is **not** the
   checkout link. Send the Review page instead,
   `https://mobile(-stg).tapin2.co/Review/Index/{venueId}?eventId={eventId}&orderId={guid}`, as
   `checkout.url`. The widget prefers `url` over `receiptUrl`.
2. **Missing catalog fields:**
   - `isAlcohol` (21+ badge and age messaging);
   - a was-price (`wasPrice` money object);
   - option `isDefault` (pre-selection and quick-add);
   - location ordering state (paused or closed; today only `cartBar.enabled` signals it).

   The widget already reads `isAlcohol`, `wasPrice` and `isDefault` if present.
3. **Missing cart fields:**
   - `orderCode` (tapin2 `idLast3`, the pickup code);
   - `isPaid`;
   - `containsAlcohol`;
   - a `secondaryAction` for "Show more restaurants" (the widget shows it by default);
   - per-line `note` echo.
4. **Ordering is implicit:** cards carry no order field, so BCOS must emit them sorted by
   `category.orderId`, then entry `orderId`.
5. **Payload size:** the stage menu as catalog cards is about 20 KB for 33 products, versus
   about 10 KB for the trimmed tapin2 shape. Every card repeats `venueId`, `eventId` and the
   location, and `OPEN_SHEET` cards repeat name, description, image and price inside `sheet`.
   Consider hoisting shared fields onto the `cartBar` or a group header, or dropping the
   duplicated `sheet` fields.
6. **`maxPerOption > 1`** (e.g. "extra shot × 2") is not supported by the widget yet.

## tapin2 observations (stage)

1. **No default options and no "required" flag.** tapin2 expresses "required" only as
   `minQuantity >= 1`, and modifiers have no default. For a required group, `+` on the tile opens
   Customize instead of quick-adding (e.g. Fountain Soda → "Fountain Soda Flavor"). The mapper
   still honors an `isDefault` flag if one is ever added.
2. **One location, several menus.** The sample combines menus 22938 (Beverages), 22939 (Food), and
   22940 (Beer) into a single menu. A product listed on more than one menu is shown once.
3. **No tile metadata field.** Metadata is derived from short comma-separated descriptions (the
   churro). Long ingredient lists and prose produce none. Confirm the intended source.
4. **No currency.** USD is assumed; ask BC to add a `currency` code.
5. **Duplicate `orderId`s** (both desserts are 1): ties keep payload order.
6. **Data quality:** some descriptions don't match their items (e.g. "Koolaid" has a tater-tot
   description). The widget displays whatever tapin2 sends.
7. **Nested modifiers / `maxOnePerSelection: false`** did not appear. The widget supports one of
   each option per group only.
