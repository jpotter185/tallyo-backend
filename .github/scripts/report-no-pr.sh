#!/usr/bin/env bash
# Used by claude-fix.yml: when a run ends without a PR for $BRANCH, comment on issue $ISSUE with the run link, the
# outcome, any tool calls the allowlist blocked, and Claude's last message (from the action's execution file).
set -uo pipefail
if [ -n "$(gh pr list --head "$BRANCH" --state all --json number --jq '.[].number')" ]; then
  echo "PR exists for $BRANCH; nothing to report"; exit 0
fi
msgs=$(mktemp)
# The execution file is a JSON array of messages; accept newline-delimited JSON too.
if [ -s "${EXEC_FILE:-}" ] && jq -s 'if length == 1 and (.[0] | type) == "array" then .[0] else . end' "$EXEC_FILE" >"$msgs" 2>/dev/null; then
  result=$(jq -c '[.[] | select(.type == "result")] | last // {}' "$msgs")
else
  result='{}'
fi
{
  echo "Auto-fix run finished without opening a PR ([run log]($RUN_URL))."
  echo
  if [ "$result" = '{}' ]; then
    echo "No Claude result was recorded; it may not have started. Check the run log."
  else
    jq -r '"Outcome: \(.subtype // "?"), turns: \(.num_turns // "?"), blocked tool calls: \(if .permission_denials then (.permission_denials | length) else (.permission_denials_count // "?") end)"' <<<"$result"
    denials=$(jq -r '(.permission_denials // [])[] | "- \(.tool_name): " + ((.tool_input.command // .tool_input.file_path // (.tool_input | tostring)) | gsub("[`\n]"; " ") | .[0:200] | "`\(.)`")' <<<"$result")
    if [ -n "$denials" ]; then
      echo; echo "**Blocked by the tool allowlist:**"; echo "$denials"
    fi
    echo; echo "**Claude's last message:**"; echo
    jq -r '.result // "(none)" | .[0:3000]' <<<"$result" | sed 's/^/> /'
  fi
  echo; echo "Re-add the \`from-server\` label to retry."
} >"$msgs.md"
gh issue comment "$ISSUE" --body-file "$msgs.md"
