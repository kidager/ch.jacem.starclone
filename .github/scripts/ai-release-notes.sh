#!/usr/bin/env bash
# Turns a release-please changelog (stdin) into customer-facing Play Store notes (stdout), written by Gemini.
# Needs GEMINI_API_KEY (GEMINI_MODEL optional). On any problem it prints nothing, exits 1 and writes
# a short "reason: ..." line on stderr, shown on the release PR.
set -euo pipefail

MAX_CHARS=500
MAX_LINES=6

fail() {
  echo "reason: $1" >&2
  exit 1
}

[ -n "${GEMINI_API_KEY:-}" ] || fail "no GEMINI_API_KEY secret"
model=${GEMINI_MODEL:-gemini-3.5-flash-lite}

prompt='You write the Google Play "What'"'"'s new" text for StarClone, an Android app to browse, sync and back up cloud storage with rclone (a fork of Round-Sync).
From the changelog below, write release notes for users, not for developers:
- One change per line. Each line starts with one emoji that matches the change, then a space, then a short plain sentence.
- Plain text only: no markdown, no bullets, no headings, no links, no commit hashes, no issue numbers.
- Only what users notice. Leave out CI, tests, build, dependencies, refactoring and release tooling.
- Cover small or technical fixes with one line such as "🛠️ General bug fixes and improvements".
- At most 6 lines and 400 characters in total, in English.
- If nothing is user-facing, answer only: 🛠️ General bug fixes and improvements'

changelog=$(cat)
request=$(jq -n --arg text "$prompt"$'\n\nChangelog:\n'"$changelog" \
  '{contents: [{parts: [{text: $text}]}], generationConfig: {temperature: 0.3}}')

response=$(mktemp)
trap 'rm -f "$response"' EXIT

# Timeouts, overload (5xx) and rate limits are often temporary on the free tier: try again a couple of times
ATTEMPTS=3
RETRY_DELAY=${GEMINI_RETRY_DELAY:-30}
for attempt in $(seq 1 "$ATTEMPTS"); do
  status=0
  # The key goes in a header: a URL can end up in logs. Free-tier answers can take over 30 s
  code=$(curl -sS --max-time 120 -o "$response" -w '%{http_code}' \
    -H "x-goog-api-key: $GEMINI_API_KEY" \
    -H "content-type: application/json" \
    -d "$request" \
    "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent") || status=$?
  [ "$status" -eq 0 ] && [ "$code" = 200 ] && break

  if [ "$status" -eq 28 ] || { [ "$status" -eq 0 ] && [[ "$code" =~ ^(429|500|502|503|504)$ ]]; }; then
    if [ "$attempt" -lt "$ATTEMPTS" ]; then
      echo "Gemini attempt $attempt failed (${code:-curl error $status}), trying again in ${RETRY_DELAY}s" >&2
      sleep "$RETRY_DELAY"
      continue
    fi
    tries=", tried $ATTEMPTS times"
  fi
  [ "$status" -eq 28 ] && fail "timed out after 2 minutes${tries:-}"
  [ "$status" -ne 0 ] && fail "couldn't reach Gemini (curl error $status)"
  [ "$code" = 429 ] && fail "free quota used up (HTTP 429)${tries:-}"
  message=$(jq -r '.error.message // empty' "$response" 2>/dev/null | head -c 150)
  fail "Gemini API error (HTTP $code): ${message%.}${tries:-}"
done

notes=$(jq -er '.candidates[0].content.parts[0].text' "$response" 2>/dev/null) || fail "Gemini sent no text"
notes=$(printf '%s\n' "$notes" | sed 's/\r$//; /^[[:space:]]*$/d')

# Only plain lines that start with an emoji, within Play's limit
[ -n "$notes" ] || fail "the answer was empty"
[ "$(printf '%s\n' "$notes" | wc -l)" -le "$MAX_LINES" ] || fail "the answer had more than $MAX_LINES lines"
[ "$(printf '%s' "$notes" | perl -CSD -0777 -ne 'print length')" -le "$MAX_CHARS" ] ||
  fail "the answer was over Play's $MAX_CHARS characters"
printf '%s\n' "$notes" | perl -CSD -ne 'exit 1 unless /^[^\x00-\x7F]\S* \S/ && !/[*#`]|\]\(|https?:/' ||
  fail "the answer wasn't plain emoji lines"

printf '%s\n' "$notes"
