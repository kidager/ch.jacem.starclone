#!/usr/bin/env bash
# Turns a release-please changelog (stdin) into Play Store release notes (stdout).
# Keeps user-facing sections only, one gitmoji (https://gitmoji.dev) per line, no markdown.
set -euo pipefail

awk '
  { sub(/\r$/, "") } # GitHub returns release and PR bodies with CRLF
  /^### / {
    emoji = ""
    if ($0 == "### Features") emoji = "✨"
    else if ($0 == "### Bug Fixes") emoji = "🐛"
    else if ($0 == "### Performance Improvements") emoji = "⚡️"
    next
  }
  /^#/ { emoji = ""; next }
  emoji != "" && /^\* / { print emoji " " substr($0, 3) }
' |
  sed -E \
    -e 's/ \(\[[^]]*\]\([^)]*\)\)//g' \
    -e 's/\*\*[^*]+:\*\* //' \
    -e 's/&gt;/>/g; s/&lt;/</g; s/&amp;/\&/g' |
  # Capitalize the first letter after the emoji
  perl -CSD -pe 's/^(\S+ )(\p{Ll})/$1\u$2/' |
  awk '{ print; n++ } END { if (n == 0) print "🐛 Bug fixes and improvements." }'
