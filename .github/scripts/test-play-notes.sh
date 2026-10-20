#!/usr/bin/env bash
# Tests for the offline parts of the Play Store release notes scripts.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
failures=0

check() { # <name> <expected> <actual>
  if [ "$2" == "$3" ]; then
    echo "ok   $1"
  else
    echo "FAIL $1"
    diff <(printf '%s\n' "$2") <(printf '%s\n' "$3") | sed 's/^/     /'
    failures=$((failures + 1))
  fi
}

changelog='## [1.2.0](https://github.com/kidager/ch.jacem.starclone/compare/v1.1.0...v1.2.0) (2026-09-29)


### Features

* **app:** add Firebase Crashlytics with opt-out setting ([#44](https://github.com/kidager/ch.jacem.starclone/issues/44)) ([f7138d8](https://github.com/kidager/ch.jacem.starclone/commit/f7138d8))


### Bug Fixes

* **data:** keep picked cover art URI instead of decoding it as Base64 ([#43](https://github.com/kidager/ch.jacem.starclone/issues/43)) ([57fc4b6](https://github.com/kidager/ch.jacem.starclone/commit/57fc4b6))
* handle A &amp; B ([#50](https://github.com/kidager/ch.jacem.starclone/issues/50))


### Performance Improvements

* **data:** faster folder scan ([#51](https://github.com/kidager/ch.jacem.starclone/issues/51))


### Dependencies

* bump coroutines from 1.10.2 to 1.11.0 ([#28](https://github.com/kidager/ch.jacem.starclone/issues/28))


### Continuous Integration

* **release:** ship pre-releases to internal ([#42](https://github.com/kidager/ch.jacem.starclone/issues/42))

---
This PR was generated with [Release Please](https://github.com/googleapis/release-please).'

check "maps sections to gitmoji and keeps user-facing ones only" \
"✨ Add Firebase Crashlytics with opt-out setting
🐛 Keep picked cover art URI instead of decoding it as Base64
🐛 Handle A & B
⚡️ Faster folder scan" \
"$(printf '%s\n' "$changelog" | ./play-release-notes.sh)"

check "handles CRLF bodies as returned by the GitHub API" \
"✨ Add Firebase Crashlytics with opt-out setting
🐛 Keep picked cover art URI instead of decoding it as Base64
🐛 Handle A & B
⚡️ Faster folder scan" \
"$(printf '%s\n' "$changelog" | sed 's/$/\r/' | ./play-release-notes.sh)"

check "falls back to a generic line without user-facing changes" \
"🐛 Bug fixes and improvements." \
"$(printf '### Dependencies\n\n* bump x\n' | ./play-release-notes.sh)"

comment='<!-- play-release-notes -->
### 📱 Play Store release notes

<!-- notes:start -->
```text
✨ New thing
🐛 Fixed thing
```
<!-- notes:end -->

<!-- generated:abc -->'

check "extracts the notes from the comment" \
"✨ New thing
🐛 Fixed thing" \
"$(printf '%s\n' "$comment" | ./play-notes.sh extract)"

check "strips CRLF from comments edited in the browser" \
"✨ New thing
🐛 Fixed thing" \
"$(printf '%s\n' "$comment" | sed 's/$/\r/' | ./play-notes.sh extract)"

notes=$'✨ New thing\n🐛 Fixed thing'
check "rendered comment reads back to the same notes, even with a warning" \
"$notes" \
"$(./play-notes.sh render "$notes" abc "Edited by hand" | ./play-notes.sh extract)"

# 10 lines of 49 chars + newline = 499, an 11th line goes over 500
line="🐛 $(printf 'x%.0s' {1..47})"
eleven=$(for _ in {1..11}; do echo "$line"; done)
ten=$(for _ in {1..10}; do echo "$line"; done)
check "cuts at a line boundary under 500 characters, counting emoji as one" \
"$ten" \
"$(printf '%s\n' "$eleven" | ./play-notes.sh fit)"

check "leaves short notes untouched" \
"✨ Short" \
"$(printf '✨ Short\n' | ./play-notes.sh fit)"

# Gemini: a fake curl first in PATH answers $FAKE_REPLY (the text) with HTTP $FAKE_CODE, or exits $FAKE_EXIT.
# Both can list one value per call ("503 200": fails, then answers); the last one repeats.
fake=$(mktemp -d)
trap 'rm -rf "$fake"' EXIT
cat > "$fake/curl" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$@" > "$FAKE_DIR/args"
calls=$(( $(cat "$FAKE_DIR/calls" 2>/dev/null || echo 0) + 1 ))
echo "$calls" > "$FAKE_DIR/calls"
pick() { local values=($1); local i=$(( calls <= ${#values[@]} ? calls : ${#values[@]} )); echo "${values[$((i - 1))]}"; }
status=$(pick "${FAKE_EXIT:-0}")
[ "$status" != 0 ] && exit "$status"
code=$(pick "${FAKE_CODE:-200}")
while [ $# -gt 0 ]; do [ "$1" = -o ] && out=$2; shift; done
if [ "$code" = 200 ]; then
  jq -n --arg t "$FAKE_REPLY" '{candidates: [{content: {parts: [{text: $t}]}}]}' > "$out"
else
  jq -n --arg m "${FAKE_REPLY:-}" '{error: {message: $m}}' > "$out"
fi
printf '%s' "$code"
EOF
chmod +x "$fake/curl"
gemini() { # <env assignments...>: Gemini's notes, its failure reason in $fake/reason, call count in $fake/calls
  rm -f "$fake/calls"
  printf '%s\n' "$changelog" | env PATH="$fake:$PATH" FAKE_DIR="$fake" GEMINI_API_KEY=k3y GEMINI_RETRY_DELAY=0 "$@" ./ai-release-notes.sh 2>"$fake/err"
  local status=$?
  sed -n 's/^reason: //p' "$fake/err" > "$fake/reason"
  return $status
}
reason() { cat "$fake/reason"; }
calls() { cat "$fake/calls"; }

check "returns Gemini's notes when they are plain emoji lines" \
"🎵 Faster folder scanning
🛠️ General bug fixes and improvements" \
"$(gemini FAKE_REPLY=$'🎵 Faster folder scanning\n\n🛠️ General bug fixes and improvements\n')"

check "sends the key in a header, never in the URL, with the changelog in the request" \
"header ok, url ok, changelog ok" \
"$(gemini FAKE_REPLY='🛠️ Fixes' > /dev/null
   grep -qx 'x-goog-api-key: k3y' "$fake/args" && h=ok || h=missing
   grep '^https://' "$fake/args" | grep -q k3y && u=leaks || u=ok
   grep -q 'faster folder scan' "$fake/args" && c=ok || c=missing
   echo "header $h, url $u, changelog $c")"

# The workflow passes an empty GEMINI_MODEL when the repo variable isn't set
model_used() { gemini FAKE_REPLY='🛠️ Fixes' "$@" > /dev/null; sed -n 's|.*/models/\(.*\):generateContent|\1|p' "$fake/args"; }
check "uses gemini-3.5-flash-lite when GEMINI_MODEL is empty" \
"gemini-3.5-flash-lite" \
"$(model_used GEMINI_MODEL=)"

check "uses GEMINI_MODEL when it is set" \
"gemini-9-pro" \
"$(model_used GEMINI_MODEL=gemini-9-pro)"

fails() { # <expected reason> <env assignments...>
  local expected=$1 out
  shift
  out=$(gemini "$@"; echo "exit $?")
  check "fails with a reason: $expected" "exit 1, $expected" "$out, $(reason)"
}
fails "timed out after 2 minutes, tried 3 times" FAKE_EXIT=28
fails "couldn't reach Gemini (curl error 6)" FAKE_EXIT=6
fails "free quota used up (HTTP 429), tried 3 times" FAKE_CODE=429
fails "Gemini API error (HTTP 503): This model is currently experiencing high demand, tried 3 times" \
  FAKE_CODE=503 FAKE_REPLY="This model is currently experiencing high demand."
fails "Gemini API error (HTTP 400): API key not valid" FAKE_CODE=400 FAKE_REPLY="API key not valid."

check "a temporary overload is tried again, and the next answer is used" \
"🛠️ Fixes, 2 calls" \
"$(gemini FAKE_CODE="503 200" FAKE_REPLY='🛠️ Fixes'), $(calls) calls"

check "a timeout is tried again too" \
"🛠️ Fixes, 2 calls" \
"$(gemini FAKE_EXIT="28 0" FAKE_REPLY='🛠️ Fixes'), $(calls) calls"

check "a permanent error isn't tried again" \
"1 calls" \
"$(gemini FAKE_CODE=400 FAKE_REPLY=nope > /dev/null; calls) calls"
fails "the answer was empty" FAKE_REPLY=$'\n  \n'
fails "the answer had more than 6 lines" FAKE_REPLY="$eleven"
fails "the answer was over Play's 500 characters" FAKE_REPLY="🐛 $(printf 'x%.0s' {1..520})"
fails "the answer wasn't plain emoji lines" FAKE_REPLY='* **Fixes**: [link](https://x)'

rm -f "$fake/args"
check "without a key, says so and never calls Gemini" \
"exit 1, no GEMINI_API_KEY secret, not called" \
"$(printf '%s\n' "$changelog" | env PATH="$fake:$PATH" FAKE_DIR="$fake" GEMINI_API_KEY= ./ai-release-notes.sh 2>"$fake/err"; echo "exit $?"), $(sed -n 's/^reason: //p' "$fake/err"), $([ -e "$fake/args" ] && echo called || echo not called)"

# The notes comment: changelog notes first with a waiting line, then Gemini's result applied
hash() { if command -v sha256sum >/dev/null; then sha256sum; else shasum -a 256; fi | cut -d' ' -f1; }
plain=$'✨ Add Firebase Crashlytics with opt-out setting\n🐛 Handle A & B'
ai_notes=$'🛡️ Crash reports you can turn off\n🛠️ General bug fixes and improvements'
waiting=$(./play-notes.sh render "$plain" "$(printf '%s' "$plain" | hash)" "" "⏳ Writing customer-facing notes with Gemini…")
edited=${waiting/Handle A & B/Fixed A and B by hand}

check "the first comment holds the changelog notes and a waiting line" \
"$plain, waiting" \
"$(printf '%s\n' "$waiting" | ./play-notes.sh extract), $(grep -q '⏳ Writing customer-facing notes with Gemini' <<< "$waiting" && echo waiting || echo missing)"

applied=$(./play-notes.sh apply "$waiting" "$plain" "$ai_notes" "")
check "Gemini's notes replace untouched ones, the changelog notes stay collapsed, the waiting line goes" \
"$ai_notes, collapsed, no waiting" \
"$(printf '%s\n' "$applied" | ./play-notes.sh extract), $(grep -q '<summary>Generated from the changelog</summary>' <<< "$applied" && grep -q 'Handle A & B' <<< "$applied" && echo collapsed || echo missing), $(grep -q '⏳' <<< "$applied" && echo waiting || echo no waiting)"

check "a later sync sees Gemini's notes as untouched and can refresh them" \
"untouched" \
"$(stored=$(sed -n 's/^<!-- generated:\([0-9a-f]*\) -->$/\1/p' <<< "$applied")
   current=$(printf '%s\n' "$applied" | ./play-notes.sh extract) # hashed like sync does
   [ "$(printf '%s' "$current" | hash)" = "$stored" ] && echo untouched || echo edited)"

failed=$(./play-notes.sh apply "$waiting" "$plain" "" "timed out after 2 minutes")
check "a failure keeps the changelog notes and says why" \
"$plain, ⚠️ Gemini notes failed: timed out after 2 minutes. The notes above come from the changelog., no waiting" \
"$(printf '%s\n' "$failed" | ./play-notes.sh extract), $(grep '^⚠️' <<< "$failed"), $(grep -q '⏳' <<< "$failed" && echo waiting || echo no waiting)"

check "a reason ending with a period doesn't get a second one" \
"⚠️ Gemini notes failed: try again later. The notes above come from the changelog." \
"$(./play-notes.sh apply "$waiting" "$plain" "" "try again later." | grep '^⚠️')"

kept=$(./play-notes.sh apply "$edited" "$plain" "$ai_notes" "")
check "notes edited while Gemini worked are kept, Gemini's version only suggested" \
"Fixed A and B by hand, suggested" \
"$(printf '%s\n' "$kept" | ./play-notes.sh extract | grep -o 'Fixed A and B by hand'), $(grep -q 'Gemini suggests:<br><br>🛡️ Crash reports you can turn off' <<< "$kept" && echo suggested || echo missing)"

kept_failed=$(./play-notes.sh apply "$edited" "$plain" "" "free quota used up (HTTP 429)")
check "edited notes are kept on a failure too, with the reason" \
"Fixed A and B by hand, ⚠️ Gemini notes failed: free quota used up (HTTP 429)." \
"$(printf '%s\n' "$kept_failed" | ./play-notes.sh extract | grep -o 'Fixed A and B by hand'), $(grep '^⚠️' <<< "$kept_failed")"

[ "$failures" -eq 0 ] || { echo "$failures test(s) failed"; exit 1; }
