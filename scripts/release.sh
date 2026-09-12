#!/usr/bin/env bash
set -euo pipefail

DRY_RUN=false
for arg in "$@"; do
	case "$arg" in
	--dry-run | -n) DRY_RUN=true ;;
	*)
		echo "Unknown argument: $arg" >&2
		exit 2
		;;
	esac
done

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

VERSION="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -1)"
if [[ -z "$VERSION" ]]; then
	echo "Error: could not detect versionName in app/build.gradle.kts" >&2
	exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
	echo "Error: working tree is dirty; commit (and push) your changes before releasing." >&2
	exit 1
fi

CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$CURRENT_BRANCH" != "master" ]]; then
	echo "Warning: not on master (on '$CURRENT_BRANCH')." >&2
fi

LOCAL_HEAD="$(git rev-parse HEAD 2>/dev/null || true)"
REMOTE_HEAD="$(git rev-parse origin/master 2>/dev/null || true)"
if [[ -n "$REMOTE_HEAD" && "$LOCAL_HEAD" != "$REMOTE_HEAD" ]]; then
	echo "Warning: local master is not up to date with origin/master; push the release commit first?" >&2
fi

CHANGELOG="CHANGELOG.md"
if ! grep -q "^## What's New in v${VERSION}$" "$CHANGELOG"; then
	echo "Error: no '## What's New in v${VERSION}' section found in $CHANGELOG." >&2
	exit 1
fi

NOTES="$(awk -v ver="$VERSION" '
    $0 ~ ("^## What.s New in v" ver "$") { capture = 1; next }
    capture && /^## / { exit }
    capture { print }
' "$CHANGELOG")"
if [[ -z "$NOTES" ]]; then
	echo "Error: changelog section for v${VERSION} is empty." >&2
	exit 1
fi

APK="app/build/outputs/renamed_apks/release/PdxBusTracker-release-${VERSION}.apk"
if [[ ! -f "$APK" ]]; then
	echo "Error: signed APK not found at $APK" >&2
	echo "Build it first:  ./gradlew assembleRelease" >&2
	exit 1
fi

TAG="v${VERSION}"
if gh release view "$TAG" >/dev/null 2>&1; then
	echo "Error: a release (or tag) $TAG already exists on GitHub." >&2
	exit 1
fi

echo "Version:  $VERSION"
echo "Tag:      $TAG"
echo "APK:      $APK"
echo "--- release notes ---"
echo "$NOTES"
echo "---------------------"

if [[ "$DRY_RUN" == true ]]; then
	echo "[dry-run] Would run: gh release create \"$TAG\" \"$APK\" --title \"$TAG\" --notes-file <extracted notes>"
	exit 0
fi

NOTES_FILE="$(mktemp)"
trap 'rm -f "$NOTES_FILE"' EXIT
printf '%s\n' "$NOTES" >"$NOTES_FILE"

gh release create "$TAG" "$APK" --title "$TAG" --notes-file "$NOTES_FILE"

URL="$(gh release view "$TAG" --json url -q .url)"
echo "Published: $URL"
