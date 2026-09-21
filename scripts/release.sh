#!/usr/bin/env bash
# Creates the GitHub release, and optionally uploads to Google Play first.
# Play upload (opt-in via --to-play) runs the `deploy_phone` fastlane lane: it runs
# `clean :app:bundleRelease` (baking the GPP AUTO-resolved versionCode into the AAB) and
# uploads to the alpha track with `release_status: "completed"`. Play runs BEFORE the
# GitHub release, and any lane failure aborts the whole script (set -euo pipefail), so a
# failed Play upload never leaves behind a GitHub release without a Play rollout.
# Confirm the baked code first with `./gradlew printReleaseVersionCode -PpublishToPlay`
# while credentials are configured.
set -euo pipefail

DRY_RUN=false
TO_PLAY=false
for arg in "$@"; do
	case "$arg" in
	--dry-run | -n) DRY_RUN=true ;;
	--to-play) TO_PLAY=true ;;
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
	echo "Error: no 'What's New in v${VERSION}' section found in $CHANGELOG." >&2
	exit 1
fi

NOTES="$(awk -v ver="$VERSION" '
    $0 ~ ("^## What.s New in v" ver "$") { capture = 1; next }
    capture && /^ / { exit }
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
# fastlane's `clean :app:bundleRelease` wipes the build directory, which would
# delete the APK before the GitHub release step. Stage a copy outside build/.
STAGED_APK_DIR="${TMPDIR:-$HOME}/pdxbus-release-${VERSION}"
mkdir -p "$STAGED_APK_DIR"
STAGED_APK="$STAGED_APK_DIR/PdxBusTracker-release-${VERSION}.apk"
cp -f "$APK" "$STAGED_APK"

TAG="v${VERSION}"
if gh release view "$TAG" >/dev/null 2>&1; then
	echo "Error: a release (or tag) $TAG already exists on GitHub." >&2
	exit 1
fi

echo "Version:  $VERSION"
echo "Tag:      $TAG"
echo "APK:      $STAGED_APK"
echo "To Play:  $TO_PLAY"
echo "--- release notes ---"
echo "$NOTES"
echo "---------------------"

if [[ "$DRY_RUN" == true ]]; then
	if [[ "$TO_PLAY" == true ]]; then
		echo "[dry-run] Would run: bundle exec fastlane android deploy_phone"
	fi
	echo "[dry-run] Would run: gh release create \"$TAG\" \"$STAGED_APK\" --title \"$TAG\" --notes-file <extracted notes>"
	exit 0
fi

# Play upload runs BEFORE the GitHub release: any lane failure aborts this script
# (set -euo pipefail) before any tag or GitHub release is created.
if [[ "$TO_PLAY" == true ]]; then
	if [[ -z "${ANDROID_PUBLISHER_CREDENTIALS:-}" ]] && ! grep -q "^playServiceAccountJsonPath=" ~/.gradle/gradle.properties 2>/dev/null; then
		echo "Error: --to-play requested but no Play credentials are configured." >&2
		echo "Set ANDROID_PUBLISHER_CREDENTIALS or the playServiceAccountJsonPath property" >&2
		echo "(in ~/.gradle/gradle.properties) so the lane can authenticate." >&2
		exit 1
	fi
	if ! command -v bundle >/dev/null 2>&1; then
		echo "Error: --to-play requested but 'bundle' is not on PATH (fastlane unavailable)." >&2
		exit 1
	fi
	echo "Uploading to Google Play (alpha) via fastlane..."
	bundle exec fastlane android deploy_phone
	echo "Play upload finished."
fi

# Explicit template under a writable dir: bare `mktemp` targets /tmp, which is
# not writable in this Termux environment.
NOTES_FILE="$(mktemp "${TMPDIR:-$HOME}/release-notes-XXXXXX")"
trap 'rm -f "$NOTES_FILE"' EXIT
printf '%s\n' "$NOTES" >"$NOTES_FILE"

gh release create "$TAG" "$STAGED_APK" --title "$TAG" --notes-file "$NOTES_FILE"

URL="$(gh release view "$TAG" --json url -q .url)"
echo "Published: $URL"
