# FieldCRM web UI design system

Last verified: 26 August 2026

This is the visual source of truth for the FastAPI/Jinja2 web application. It applies to authenticated operational pages, Configuration Admin, authentication, public pages, protected document views, partial responses, and dialogs. Android is intentionally excluded.

## Cascade and source order

The normal authenticated shell loads styles in this order:

1. `frontend/static/css/dashboard.css` — legacy layout and component foundation.
2. `frontend/static/css/role-themes.css` — shared role variables and navigation accents.
3. Page-level styles from `extra_css`, including `dashboard_legacy.css` where required.
4. `frontend/static/css/web-ui-system.css` — final semantic action, accessibility, responsive, and approval overrides.

`web-ui-system.css` must stay last. All changed stylesheets use a cache-busting query string in their shells.

## Canonical palette

| Token/meaning | Value | Applied elements | Must not be used for |
|---|---:|---|---|
| Primary forward | `#2E0052` | The single move-up/forward action in a decision panel, primary brand surfaces, main headings | Tabs, utilities, return, cancel, filters, status badges |
| Primary hover | `#23003F` | Hover/pressed state of the primary forward action | Secondary actions |
| Secondary plum | `#6F2676` | Ordinary actions, sidebar active accents, links, tabs, filters, upload, preview, analysis, return/correction outlines | Destructive or completed states |
| Secondary hover | `#5B1F61` | Hover/pressed state for plum actions and links | Primary forward action |
| Secondary soft | `#F7EFF8` | Plum hover fills, pending/review chips, selected compact controls, callouts | Error or success fills |
| Secondary border | `#D8BFDB` | Plum-tinted borders and selected compact controls | Disabled borders |
| Danger | `#B42318` | Deny/archive, delete, deactivate, irreversible administration | Ordinary return/correction or validation help |
| Danger hover | `#912018` | Hover/pressed destructive action | Non-destructive actions |
| Success | `#147A55` | Verified, completed, approved state only | Action buttons that merely submit or navigate |
| Disabled | `#94A3B8` | Disabled/unavailable controls only | Pending, empty, or read-only states |
| Canvas | `#F3F5F7` | Application background | Cards and dialogs |
| Surface | `#FFFFFF` | Cards, forms, tables, dialogs | Page canvas |
| Soft neutral surface | `#F8FAFB` | Subtle grouped backgrounds | Semantic state indicators |
| Primary ink | `#172033` | Main text and field values | Muted metadata |
| Muted ink | `#526174` | Helper text, timestamps, secondary metadata | Disabled controls |
| Neutral border | `#CBD5E1` | Default inputs, tables, cards, separators | Semantic selected states |
| Focus ring | `rgba(111,38,118,.28)` | Keyboard focus around links, controls, fields | Persistent decoration |
| Dialog backdrop | `rgba(18,10,28,.42)` | Confirmation modal overlay | Page cards |

There are no brown or amber accent values in the web assets. Caution and correction states use plum unless they are destructive, in which case they use red.

### Special-surface palettes

These surfaces intentionally have their own neutral/brand presentation while preserving the semantic action hierarchy.

| Surface | Exact colors and element use |
|---|---|
| Public landing/terms | Brand/header/buttons/headings `#2E0052`; public accent/eyebrows/checks `#89268B`; canvas `#F2F2F2`; white cards `#FFFFFF`; ink `#1E293B`; muted `#64748B`; borders `#D8D4DA`; public success `#167047` |
| Login/recovery | Main gradient `#2E0052` → `#89268B`; page canvas `#EEF2F8`; card `rgba(255,255,255,.78)`; ink `#172033`; muted `#667085`; field border `#D8DEE8`; error `#B42318`/`#FFF4F3`; focus `#89268B` |
| Application Overview | Hero gradient `#2E0052` → `#59145F` → `#7D267E`; hero text white; tabs/cards white; active tab `#F3EAF5`/`#2E0052`; overview border `#E6DCE9`; activity/progress plum `#84278A`/`#76227B` |
| Configuration Admin | Ink `#171326`; muted `#6E687C`; purple `#5B2D8F`; dark purple `#29143F`; soft purple `#F1EAFB`; canvas `#F6F5F8`; card white; border `#E5E1EA`; verified `#18744C` on `#E9F6EF`; pending/action plum `#6F2676` on `#F7EFF8` |
| Borrower directory | Uses dashboard tokens; white record cards on soft neutral `#F7F9FB`, deep-purple selected border, and translucent plum selected/hover fill |
| Printable offer/report | White background, dark neutral text, configured institution logo/accent, neutral rules; screen-only navigation/actions are omitted |

## Role colors

Every operational role uses one visual role system. Role changes affect labels, navigation destinations, permissions, and data—not color.

| Role | Accent | Hover | Tint | Sidebar active border | Sidebar active text |
|---|---:|---:|---:|---:|---:|
| Relationship Officer | `#6F2676` | `#5B1F61` | `#F7EFF8` | `#B98FBD` | `#F7EFF8` |
| Team Lead | `#6F2676` | `#5B1F61` | `#F7EFF8` | `#B98FBD` | `#F7EFF8` |
| Supervisor | `#6F2676` | `#5B1F61` | `#F7EFF8` | `#B98FBD` | `#F7EFF8` |
| Credit Analyst/Officer | `#6F2676` | `#5B1F61` | `#F7EFF8` | `#B98FBD` | `#F7EFF8` |
| CRM/Head CRM | `#6F2676` | `#5B1F61` | `#F7EFF8` | `#B98FBD` | `#F7EFF8` |
| ED/MD/Executive | `#6F2676` | `#5B1F61` | `#F7EFF8` | `#B98FBD` | `#F7EFF8` |
| Legal | `#6F2676` | `#5B1F61` | `#F7EFF8` | `#B98FBD` | `#F7EFF8` |
| Auditor | `#6F2676` | `#5B1F61` | `#F7EFF8` | `#B98FBD` | `#F7EFF8` |
| System Admin | `#6F2676` | `#5B1F61` | `#F7EFF8` | `#B98FBD` | `#F7EFF8` |
| Configuration Admin | `#6F2676` | `#5B1F61` | `#F7EFF8` | Plum/neutral shell | Plum/white |

## Element-by-element color and treatment

### Shell and navigation

| Element | Background | Text/icon | Border/state | Arrangement |
|---|---|---|---|---|
| Desktop sidebar | Deep brand purple from `dashboard.css` | White at full opacity for active, reduced opacity for idle | Active left border `#B98FBD`; active tint uses translucent plum | Fixed left rail; logo top, grouped navigation centre, logout bottom |
| Sidebar link idle | Transparent | Translucent white | Transparent left border | One row, icon/label then count badge |
| Sidebar link hover | Subtle translucent white/plum | White | Existing border retained | No movement; full-row hit target |
| Sidebar link active | `rgba(111,38,118,.18)` | `#F7EFF8` | `#B98FBD` left edge | Same size as idle to prevent layout shift |
| Sidebar count badge | `#6F2676` for review/warning counts | White | None | Trailing compact pill |
| Mobile drawer | Same as desktop sidebar | Same as desktop | Blurred/dim backdrop | Off-canvas; opened by menu button |
| Top bar | White/neutral surface | `#172033` | Neutral bottom separation | Page title left; search, notifications, profile right |
| Global search | White | `#172033`; placeholder `#526174` | `#CBD5E1`, plum focus | Inline desktop; constrained at smaller widths |

### Actions

| Semantic class | Default | Hover/focus | Typical labels |
|---|---|---|---|
| `.action-primary` | Filled `#2E0052`, white text | `#23003F`, slight lift, plum focus ring | Concur & Forward, move up, approve/send, verify |
| `.action-secondary-filled` | Filled `#6F2676`, white text | `#5B1F61`, slight lift | Upload, Open Analysis, Request MD Input, Request Opinion |
| `.action-secondary-outlined` | Transparent, plum text/border | `#F7EFF8` fill, `#5B1F61` text/border | Return, request correction, preview, reference views |
| `.action-secondary-ghost` | Transparent, plum text, transparent border | `#F7EFF8` fill | Cancel, Back, Close |
| `.action-secondary-compact` | Transparent, plum text, 36px minimum height | `#F7EFF8` and `#D8BFDB` border | Tabs, zoom, fit, previous/next, row utilities |
| `.action-warning` | Plum outline, not brown | Plum-soft hover | Compatibility alias for cautionary reversible actions |
| `.action-danger` | Filled `#B42318`, white text | `#912018` | Deny & Archive, Delete, Deactivate |
| `.action-link` | Plain `#6F2676` text | `#5B1F61`, stronger underline | Low-competition navigation |
| Disabled | Filled/bordered `#94A3B8`, white text | No lift; not-allowed cursor | Unavailable submit/upload/continue |

All decision-panel action classes (`.action-primary`, `.action-secondary-filled`, `.action-secondary-outlined`, `.action-secondary-ghost`, and `.action-danger`) share identical sizing and responsive behavior: minimum touch target, full-width breakpoints, stacking order, and mobile spacing (see Responsive and accessibility contract). Only fill, border, and text color differ. `.action-secondary-compact` is the deliberate exception for tabs, zoom, and row utilities; it stays compact and inline and never becomes a full-width decision action.

Decision-panel order is primary, workflow secondary/correction, danger, then cancel/back. On mobile the same semantic order is enforced while controls become full width.

### Cards, tables, forms, and states

| Element | Color specification | Arrangement |
|---|---|---|
| Page/card surface | White, neutral border/shadow, 14px radius | 20–32px responsive padding |
| Metric card | White; plum icon tint; dark value; muted label | Responsive grid; collapses without reordering |
| Input/select | `#FBFCFE`, `#172033`, `#CBD5E1` border | Label above control; minimum 46px height |
| Input focus | White, plum border and focus ring | No layout shift |
| Textarea | Same as input | Minimum 116px; vertical resize |
| Table header | Neutral/soft surface, dark text | Sticky only where template requests it |
| Table row hover | Neutral or plum-soft tint | Row action remains visible and keyboard reachable |
| Verified/approved/completed badge | Pale green background, `#147A55` text | Compact pill, informational only |
| Needs review/returned/pending badge | `#F7EFF8`, `#6F2676` text | Compact pill; never brown |
| Missing/unavailable | Neutral or plum-soft depending context | Text remains explicit; color is not sole signal |
| Danger/error | Pale red context or filled red action | Error copy plus icon/text label |
| Readiness progress | Plum track fill on neutral track | Percentage and critical-item text adjacent |
| Approval warning/callout | `#F7EFF8`, `#D8BFDB`, `#5B1F61` | Full-width callout within readiness card |

### Dialogs, feedback, and document preview

| Element | Color | Arrangement/behavior |
|---|---|---|
| Confirmation backdrop | `rgba(18,10,28,.42)` with 8px blur | Fixed full viewport |
| Confirmation panel | White, neutral border, 18px radius | Centred; message then semantic buttons |
| Confirm action | Inherits source action meaning | Danger stays red; secondary stays plum; primary stays deep purple |
| Cancel in dialog | Plum ghost | Before confirm visually on desktop; stacked on mobile |
| Success toast | Dark success green, white text | Fixed bottom-right; full-width inset on mobile |
| Error toast | Dark red, white text | Same placement as success |
| Document preview | Darkened backdrop, white viewer surface | Centred modal; scrollable pages; Close and Escape |
| Upload progress | Plum progress with textual percentage | Beneath selected filename/action |

## Typography, spacing, and geometry

- Primary font: Montserrat, with system fallbacks where a standalone page requires them.
- Control radius: 10px.
- Card radius: 14px.
- Dialog radius: 18px.
- Minimum control/touch target: 44px; 46px on narrow phones.
- Spacing scale: 4, 8, 12, 16, 20, 24, and 32px.
- Main text: `#172033`; helper text: `#526174`.
- Labels precede fields and do not rely on placeholders.
- Currency is shown as naira with grouping where available.

## Responsive and accessibility contract

- Desktop: persistent sidebar and grouped horizontal actions.
- Tablet: reduced/collapsible rail; cards and action rows wrap in source/semantic order.
- Mobile: drawer navigation; one-column page cards; every action class in a decision panel or form footer becomes full width. Primary and secondary actions in the same panel match in width and height and differ only by color/fill.
- Tables use horizontal scrolling with an explicit “Swipe to view more” cue where appropriate.
- Focus is visible on every interactive element.
- Dialog focus stays inside the confirmation panel; Escape closes it and returns focus.
- Reduced-motion preference collapses animations and transitions.
- Text, icon, status wording, and shape accompany color; color is never the only state signal.
- Jinja output remains autoescaped. Shared JavaScript builds feedback text with `textContent`, not raw HTML.

## Maintenance rules

1. Add a semantic action class alongside legacy classes; do not remove route, form, name, value, or confirmation attributes for styling.
2. Keep one `.action-primary` per decision panel.
3. Secondary and danger actions in the same panel must match the primary action's touch target and responsive width; sizing never implies hierarchy, only color/fill does.
4. Put shared behavior in external JavaScript and shared presentation in external CSS.
5. Load `web-ui-system.css` after legacy/page styles.
6. Version changed static assets in every shell that loads them.
7. Update this document, `layout-sketches.md`, and `source-catalog.md` whenever a new web surface or asset is added.
