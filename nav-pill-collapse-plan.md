# Collapse bottom pill to one tab on non-main screens

## Context

On the Arrivals screen (and every other non-main screen — Settings, Nearby Stops), the floating pill still shows all four top-level tabs plus a trailing circle button that is Settings everywhere except Settings itself where it becomes Back. The ask: outside the main pager (`route == "home"`), collapse the pill to its one active tab, show a back-arrow circle on the LEFT side of the bar, and hide the trailing circle there — the Settings gear appears only on the four main tabs.

## Approach

All changes in `app/src/main/java/com/trimettransit/tracker/activities/MainActivity.kt` (only file with nav-bar code; single caller of `MainBottomBar`).

1. **`MainBottomBar` (line ~204) — add `collapsed: Boolean = false` parameter** after `compact`.

2. **Select items by mode** at the top of `MainBottomBar`, replacing `val items = bottomNavItems`:
   ```kotlin
   val items = if (collapsed)
       listOf(bottomNavItems[topPage.coerceIn(0, bottomNavItems.lastIndex)])
   else bottomNavItems
   ```
   Reuses the existing per-item rendering untouched: the lone item renders selected (`topPage == item.pageIndex`) → filled surfaceContainer background, icon + label expansion via existing `labelWidth` animation (`shouldHideLabel` won't suppress it since `items.size > 3` is false when collapsed).

3. **Tap behavior**: change the `IconButton` onClick guard from
   `if (item.pageIndex != topPage)` to `if (collapsed || item.pageIndex != topPage)`.
   In collapsed mode tapping the tab always calls `onNavigate(item.pageIndex)`, which routes through the existing `navigateToTopPage` (pops back stack to `"home"`, animates pager).

4. **Animate expand/collapse**: wrap the inner `Row` inside the pill `Surface` (the one iterating `items.forEachIndexed`, line ~244-320) in `AnimatedContent(targetState = collapsed, transitionSpec = { (fadeIn(spring()) + scaleIn(initialScale = 0.85f, animationSpec = spring())) togetherWith (fadeOut(spring()) + scaleOut(targetScale = 0.85f, animationSpec = spring())) }, label = "nav_collapse") { collapsedState -> Row(...) { ...use collapsedState... } }`. `AnimatedContent`, `spring`, `fadeIn/fadeOut`, `scaleIn/scaleOut` are already imported/used in this file. Keep the outer pill `Surface` static so only contents morph.

5. **Move the back circle to the LEFT of the bar** (`MainBottomBar`, outer `Row` at line ~234): reorder to `[back circle] [pill Surface] [settings circle]`.
   - Insert before the pill `Surface` (line ~238): `if (showBack) { Surface(...) { ... } }` — an exact copy of the current trailing-circle block (line ~322-342: `RoundedCornerShape(28.dp)`, `tertiaryContainer`, `shadowElevation = 8.dp`, `tonalElevation = 4.dp`, 48.dp `IconButton` with `pressScale` interaction source, `Icons.AutoMirrored.Filled.ArrowBack`) with `onClick = onBackClick`, placed first in the `Row` so the existing `spacedBy(12.dp)` separates it from the pill.
   - Gate the existing trailing `Surface` on `!showBack` and simplify it to Settings-only: wrap in `if (!showBack)`, drop the `onClick = if (showBack) onBackClick else onSettingsClick` ternary → `onClick = onSettingsClick`, and drop the ArrowBack branch so its Icon is always `Icons.Default.Settings` / `"Settings"`.

6. **Call site (line ~596-606)** inside `bottomBar = {}`:
   - `showBack = currentRoute == "settings"` → `showBack = !isTopLevel`
   - add `collapsed = !isTopLevel`
   Everything else unchanged (`topPage`, `onNavigate = ::navigateToTopPage`, `onSettingsClick`, existing `onBackClick = { navController.popBackStack() }`, `compact = currentRoute.startsWith("arrivals/")`). Result: main tabs → 4-tab pill + right Settings circle; arrivals/nearby_stops/settings → left back-arrow circle + single-tab pill, no right circle. No new imports needed.

7. **CHANGELOG.md**: add bullet under the top Unreleased section describing the collapsed single-tab pill with a left back button on sub-screens and Settings now only on main screens (repo convention for user-visible changes).

Behavior notes: Settings remains reachable from all four main tabs only. PiP already hides the whole bar (`!inPip`). Deep stacks behave correctly: arrivals opened from Nearby Stops → back circle returns to Nearby Stops (`popBackStack`), not home; tapping the collapsed tab jumps straight to the remembered main tab.


## Critical files & anchors
- `app/src/main/java/com/trimettransit/tracker/activities/MainActivity.kt` — `MainBottomBar` composable (~204-345): param list, `items` selection (~215), item onClick guard (~270), inner `Row` wrap (~244); call site in `MainAppContent` scaffold `bottomBar` (~592-602). Reread before editing; line numbers drift.
- `CHANGELOG.md` — top Unreleased section.

## Verification

From repo root (Termux shell with `ANDROID_HOME` set):

1. `./gradlew :app:lint :app:assembleDebug` — compiles clean, no new lint findings (every module gates builds on `abortOnError = true`). No install, no emulator, no device interaction.
2. Static behavior audit in place of device testing (explicitly requested off): after editing, re-read the final `MainBottomBar` and its call site and verify each render branch against expected states:
   - `currentRoute == "home"` → `items == bottomNavItems` (4 tabs), `showBack == false`, trailing Surface rendered with Settings icon.
   - `arrivals/*` / `nearby_stops` / `settings` → `items` is the single active-page entry, leading back-circle present (`ArrowBack`, `onClick = onBackClick` → `popBackStack()`), trailing Surface absent.
   - Collapsed-mode tab tap calls `onNavigate(item.pageIndex)` unconditionally; expanded mode keeps the `!= topPage` guard.

## Assumptions & contingencies

- "One tab" = the currently-active top-level tab (icon + label of `topPagerState.currentPage`), since that's the context the user left; tapping it returns there. If reality shows the user wanted a fixed "Home" tab instead, swap the `items` selection in step 2 to always pick page 0 — one-line change, no other steps affected.
- Back-arrow circle sits LEFT of the pill and the trailing circle is hidden on ALL non-main screens (arrivals, nearby_stops, settings) — that is the literal ask; Settings stays reachable from every main tab.
