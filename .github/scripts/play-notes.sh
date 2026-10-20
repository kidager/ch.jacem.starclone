#!/usr/bin/env bash
# Play Store release notes kept in a comment on the release-please PR, so they can be edited
# before merging. release-please rewrites the PR body on every update, a comment survives.
#
#   play-notes.sh sync <pr-number>   create or refresh the notes comment (keeps manual edits)
#   play-notes.sh ai <pr-number>     replace them with Gemini's customer-facing notes, or say why not
#   play-notes.sh release <tag>      print the notes for a release (from its PR comment)
#   play-notes.sh extract            stdin comment body -> notes
#   play-notes.sh fit                stdin notes -> notes cut to Play's limit, at a line boundary
set -euo pipefail

MARKER='<!-- play-release-notes -->'
MAX_CHARS=500
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

char_count() { perl -CSD -0777 -ne 'print length'; }

notes_hash() {
  if command -v sha256sum >/dev/null; then sha256sum; else shasum -a 256; fi | cut -d' ' -f1
}

extract() {
  awk '
    { sub(/\r$/, "") } # comments edited in the browser use CRLF
    /<!-- notes:start -->/ { inside = 1; next }
    /<!-- notes:end -->/ { inside = 0 }
    inside && !/^```/ { print }
  '
}

fit() {
  perl -CSD -ne '
    chomp; my $sep = length($out) ? "\n" : "";
    last if length($out) + length($sep) + length($_) > '"$MAX_CHARS"';
    $out .= $sep . $_;
    END { print "$out\n" }
  '
}

# render <notes> <generated-hash> [warning] [status] [changelog-notes]
# status: a line under the box (Gemini progress or failure); changelog-notes: shown collapsed, never published
render() {
  local notes=$1 hash=$2 warning=${3:-} status=${4:-} previous=${5:-} count
  count=$(printf '%s' "$notes" | char_count)
  cat <<EOF
$MARKER
### 📱 Play Store release notes

Edit the text in the box below, it is published as the Play Store "What's new" when this PR is merged. Delete this comment to regenerate it.

<!-- notes:start -->
\`\`\`text
$notes
\`\`\`
<!-- notes:end -->

<sub>$count/$MAX_CHARS characters$([ "$count" -gt "$MAX_CHARS" ] && echo ", ⚠️ over the limit, extra lines will be dropped")</sub>
${status:+
$status
}${warning:+
> [!WARNING]
> $warning
}${previous:+
<details><summary>Generated from the changelog</summary>

\`\`\`text
$previous
\`\`\`
</details>
}
<!-- generated:$hash -->
EOF
}

WAITING="⏳ Writing customer-facing notes with Gemini…"

# Hand-edited notes are never replaced, the warning shows what was suggested instead
suggestion() { # <who> <notes>
  printf 'Edited by hand, so it was not regenerated. %s:<br><br>%s' "$1" "$(printf '%s' "$2" | sed 's/$/<br>/' | tr -d '\n')"
}

# apply <comment-body> <changelog-notes> <gemini-notes> <failure-reason>: the comment once Gemini is done.
# Gemini's notes replace untouched ones (the changelog version kept collapsed); a failure keeps them with the reason.
apply() {
  local body=$1 plain=$2 ai=$3 reason=${4%.} current stored_hash edited=false
  current=$(printf '%s\n' "$body" | extract)
  stored_hash=$(printf '%s\n' "$body" | sed -n 's/^<!-- generated:\([0-9a-f]*\) -->$/\1/p')
  [ "$(printf '%s' "$current" | notes_hash)" = "$stored_hash" ] || edited=true

  if [ -n "$ai" ] && [ "$edited" = false ]; then
    render "$ai" "$(printf '%s' "$ai" | notes_hash)" "" "" "$plain"
  elif [ -n "$ai" ]; then
    render "$current" "$stored_hash" "$(suggestion "Gemini suggests" "$ai")"
  elif [ "$edited" = true ] && [ "$current" != "$plain" ]; then
    render "$current" "$stored_hash" "$(suggestion "The changelog now suggests" "$plain")" "⚠️ Gemini notes failed: $reason."
  else
    render "$current" "$stored_hash" "" "⚠️ Gemini notes failed: $reason. The notes above come from the changelog."
  fi
}

find_comment() { # <pr-number> -> id of our notes comment, empty if none
  gh api "repos/$GITHUB_REPOSITORY/issues/$1/comments" --paginate \
    --jq ".[] | select(.user.login == \"kidager[bot]\" and (.body | startswith(\"$MARKER\"))) | .id" |
    head -n1
}

# Fast pass: notes from the changelog sections right away, Gemini's follow later (ai)
sync() {
  local pr=$1 generated gen_hash id body current cur_hash stored_hash new_body
  generated=$(gh pr view "$pr" --repo "$GITHUB_REPOSITORY" --json body --jq .body | "$SCRIPT_DIR/play-release-notes.sh")
  gen_hash=$(printf '%s' "$generated" | notes_hash)
  id=$(find_comment "$pr")

  if [ -z "$id" ]; then
    render "$generated" "$gen_hash" "" "$WAITING" | gh api "repos/$GITHUB_REPOSITORY/issues/$pr/comments" -F body=@- --silent
    echo "Created release notes comment on #$pr"
    return
  fi

  body=$(gh api "repos/$GITHUB_REPOSITORY/issues/comments/$id" --jq .body)
  current=$(printf '%s\n' "$body" | extract)
  cur_hash=$(printf '%s' "$current" | notes_hash)
  stored_hash=$(printf '%s\n' "$body" | sed -n 's/^<!-- generated:\([0-9a-f]*\) -->$/\1/p')

  if [ "$cur_hash" = "$stored_hash" ]; then
    new_body=$(render "$generated" "$gen_hash" "" "$WAITING")
    echo "Refreshed release notes comment on #$pr"
  elif [ "$generated" = "$current" ]; then
    new_body=$(render "$current" "$stored_hash" "" "$WAITING")
    echo "Release notes on #$pr were edited and match the changelog"
  else
    # Edited by hand: keep the text, flag that the changelog has moved on
    new_body=$(render "$current" "$stored_hash" "$(suggestion "The changelog now suggests" "$generated")" "$WAITING")
    echo "Kept hand-edited release notes on #$pr"
  fi
  printf '%s\n' "$new_body" | gh api -X PATCH "repos/$GITHUB_REPOSITORY/issues/comments/$id" -F body=@- --silent
}

# Slow pass, its own job: ask Gemini, then replace the notes or report why it couldn't
ai() {
  local pr=$1 id changelog plain notes="" reason="" errors body
  id=$(find_comment "$pr")
  [ -n "$id" ] || { echo "No release notes comment on #$pr, nothing to do"; return; }
  changelog=$(gh pr view "$pr" --repo "$GITHUB_REPOSITORY" --json body --jq .body)
  plain=$(printf '%s\n' "$changelog" | "$SCRIPT_DIR/play-release-notes.sh")
  errors=$(mktemp)
  if notes=$(printf '%s\n' "$changelog" | "$SCRIPT_DIR/ai-release-notes.sh" 2>"$errors"); then
    echo "Release notes written by Gemini"
  else
    notes=""
    reason=$(sed -n 's/^reason: //p' "$errors" | tail -n1)
    reason=${reason:-unknown error}
    echo "::warning::Gemini release notes failed: $reason"
  fi
  rm -f "$errors"
  # Read the comment again only now: it may have been edited while Gemini was working
  body=$(gh api "repos/$GITHUB_REPOSITORY/issues/comments/$id" --jq .body)
  apply "$body" "$plain" "$notes" "$reason" | gh api -X PATCH "repos/$GITHUB_REPOSITORY/issues/comments/$id" -F body=@- --silent
}

release() {
  local tag=$1 sha pr id notes=""
  sha=$(gh api "repos/$GITHUB_REPOSITORY/commits/$tag" --jq .sha)
  pr=$(gh api "repos/$GITHUB_REPOSITORY/commits/$sha/pulls" --jq '.[0].number // empty')
  if [ -n "$pr" ]; then
    id=$(find_comment "$pr")
    [ -n "$id" ] && notes=$(gh api "repos/$GITHUB_REPOSITORY/issues/comments/$id" --jq .body | extract)
  fi
  if [ -z "${notes//[[:space:]]/}" ]; then
    echo "::warning::No release notes comment found for $tag, generating them from the release body" >&2
    notes=$(gh release view "$tag" --repo "$GITHUB_REPOSITORY" --json body --jq .body | "$SCRIPT_DIR/play-release-notes.sh")
  fi
  if [ "$(printf '%s' "$notes" | char_count)" -gt "$MAX_CHARS" ]; then
    echo "::warning::Release notes are over $MAX_CHARS characters, extra lines were dropped" >&2
  fi
  printf '%s\n' "$notes" | fit
}

cmd=${1:-}
shift || true
case "$cmd" in
  sync | ai | release) "$cmd" "$@" ;;
  apply | extract | fit | render) "$cmd" "$@" ;;
  *) sed -n '2,10p' "$0" >&2; exit 64 ;;
esac
