# Collapse bottom pill to one tab on non-main screens — final verified plan

Source: `nav-pill-collapse-plan.md` (verified against code 2026-08-24). All anchors confirmed in
`app/src/main/java/com/trimettransit/tracker/activities/MainActivity.kt`.

## Corrections vs original plan (3)

1. **Missing imports** — plan wrongly claimed `scaleIn`/`scaleOut` already imported. Add:
   - `import androidx.compose.animation.scaleIn`
   - `import androidx.compose.animation.scaleOut`
2. **AnimatedContent scoping** — `items` and `shouldHideLabel` must be computed INSIDE the
   AnimatedContent lambda from `collapsedState`, not as outer vals from the current `collapsed`
   param. Otherwise both transition frames render identical content (crossfade degenerates to a
   pulse) and `shouldHideLabel` always sees a 4-item list.
3. **Changelog** — no "Unreleased" section exists. Per user: create new top section
   `## What's New in v4.9.5` that persists (matches existing per-version heading style).
   Do NOT bump `versionName` in build.gradle (not requested).

## Edits

### A. Imports (MainActivity.kt ~89)
Insert `scaleIn`/`scaleOut` between the existing `AnimatedContent` and `togetherWith` imports.

### B. MainBottomBar (~204-345)

1. Signature: add `collapsed: Boolean = false` after `compact`.
2. Delete outer `val items = bottomNavItems` (line 215) and outer `shouldHideLabel` (216-217);
   keep `windowInfo`, `density`, `fontScale` (used inside lambda).
3. Outer Row (~234) reorder to `[back circle] [pill] [settings circle]`:
   - Leading block: `if (showBack) { Surface(...) }` — exact copy of current trailing circle
     block (lines 322-342): `RoundedCornerShape(28.dp)`, `tertiaryContainer`,
     `shadowElevation = 8.dp`, `tonalElevation = 4.dp`, 48.dp IconButton with
     `pressScale(source, 0.92f)`; Icon = `Icons.AutoMirrored.Filled.ArrowBack`,
     contentDescription `"Back"`, `onClick = onBackClick`. Own interaction source.
   - Pill Surface (238-321) unchanged except inner `Row` wrapped in:
     ```kotlin
     AnimatedContent(
         targetState = collapsed,
         transitionSpec = {
             (fadeIn(spring()) + scaleIn(initialScale = 0.85f, animationSpec = spring())) togetherWith
                 (fadeOut(spring()) + scaleOut(targetScale = 0.85f, animationSpec = spring()))
         },
         label = "nav_collapse"
     ) { collapsedState ->
         val items = if (collapsedState)
             listOf(bottomNavItems[topPage.coerceIn(0, bottomNavItems.lastIndex)])
         else bottomNavItems
         val shouldHideLabel = fontScale > 1.25f ||
                 (windowInfo.containerSize.width < with(density) { 400.dp.roundToPx() } && items.size > 3)
         Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
             items.forEachIndexed { index, item -> /* body unchanged */ }
         }
     }
     ```
   - Item onClick guard (~271): `if (item.pageIndex != topPage)` → `if (collapsed || item.pageIndex != topPage)`.
   - Trailing circle (~322-342): wrap in `if (!showBack)`, `onClick = onSettingsClick` (drop ternary),
     icon always `Icons.Default.Settings` / `"Settings"`.

### C. Call site (~592-602, inside scaffold bottomBar)
- `showBack = currentRoute == "settings"` → `showBack = !isTopLevel`
- add `collapsed = !isTopLevel` after `compact = ...`
- everything else unchanged (`isTopLevel` defined line 401, in scope).

### D. CHANGELOG.md
New top section right after `# Changelog`:
```markdown
## What's New in v4.9.5

- **Collapsed navigation pill on sub-screens:** on Arrivals, Nearby Stops and Settings the
  floating bottom bar now collapses to a single tab for the area you're in, with a dedicated
  back-arrow button on its left; the Settings gear now appears only on the four main tabs.
  (`app/src/main/java/com/trimettransit/tracker/activities/MainActivity.kt`)
```

## Verification
1. `./gradlew :app:lint :app:assembleDebug` — clean compile, no new lint findings.
2. Static branch audit of final `MainBottomBar` + call site:
   - `currentRoute == "home"` → 4-tab pill, no back circle, trailing Settings circle present.
   - `arrivals/*` / `nearby_stops` / `settings` → single active-page tab, leading ArrowBack
     circle (`onBackClick` = popBackStack), no trailing circle.
   - Collapsed tap → unconditional `onNavigate(item.pageIndex)`; expanded keeps `!= topPage` guard.
   - PiP still hides whole bar via existing `!inPip` AnimatedVisibility.
