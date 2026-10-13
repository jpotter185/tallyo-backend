# Auto-fix setup (one time, on the Mac)

The jack-server watchdog files issues labeled `from-server`. The label starts `.github/workflows/claude-fix.yml`.
Claude fixes the bug on GitHub's runners and opens a PR that says "Fixes #N". You merge it, the merge deploys,
and the issue closes. Three setup steps are needed before the first run.

## 1. Install the Claude GitHub App

Open https://github.com/apps/claude, click **Install** (or **Configure**), choose **Only select repositories**,
and pick **tallyo-backend**.

The app is what lets the bot's PRs trigger CI. `/install-github-app` in Claude Code works too, but it also offers
to add its own workflow files; decline those, because `claude-fix.yml` is the only one we need.

## 2. Add the Claude token secret

```bash
claude setup-token
```

Copy the token it prints, then:

```bash
gh secret set CLAUDE_CODE_OAUTH_TOKEN -R jpotter185/tallyo-backend
```

Paste the token when prompted. Or use the web UI: Settings → Secrets and variables → Actions → New repository
secret, with the name `CLAUDE_CODE_OAUTH_TOKEN`.

Runs use your Claude plan's usage. If the token ever leaks, revoke it and run `claude setup-token` again.

## 3. Protect main (the bot must not push to it)

Settings → Rules → Rulesets → **New ruleset** → **New branch ruleset**:

- Name: `protect-main`. Enforcement status: **Active**.
- Bypass list: **Add bypass** → **Repository admin** → Always allow. This lets you keep pushing and merging.
- Target branches: **Add target** → **Include default branch**.
- Rules: check **Restrict updates**, **Restrict deletions** and **Block force pushes**.
- Save.

## 4. First run: issue #4 (varchar overflow)

Re-adding the label starts the workflow:

```bash
gh issue edit 4 -R jpotter185/tallyo-backend --remove-label from-server
gh issue edit 4 -R jpotter185/tallyo-backend --add-label from-server
gh run list -R jpotter185/tallyo-backend --workflow claude-fix.yml
gh run watch -R jpotter185/tallyo-backend    # pick the run
```

Expect a branch `claude/issue-4-<run>` and a PR containing:

- "Fixes #4"
- the root cause, the change and a risk level
- the `./mvnw test` results
- verify output: the app booted against Postgres 15, plus the curl results
- for schema changes, a new Liquibase file in `db/migrations/` (Hibernate's `ddl-auto: update` never widens an
  existing column). Deploys apply it; see `db/README.md`.

CI (Backend OpenAPI Check) should run on the PR, and nothing should land on main until you merge.

If no PR appears, open the run's log in the Actions tab. The step `verify-app.sh` had not run before this,
so check it first. If Claude was unsure of the cause, it comments on the issue instead of opening a PR.

## 5. After you merge #4's PR

Merging deploys and closes #4. The column widening is `db/migrations/001-widen-games-text-columns.sql`, which
`deploy.sh` applies before restarting the app, so there is no manual SQL. To check it ran, on jack-server:

```bash
sudo docker exec myapp-postgres sh -c 'psql -U "$POSTGRES_USER" -d "${POSTGRES_DB:-$POSTGRES_USER}" -c "SELECT id, dateexecuted FROM databasechangelog"'
```

If the error comes back on the next CFB/NFL night, the watchdog files a new issue that links to #4.

## Retrying or stopping

- Retry a run: remove and re-add the `from-server` label.
- Stop a run: Actions tab → the run → Cancel workflow.
- Turn it all off: Actions tab → "Claude fix for server issues" → ⋯ → Disable workflow.
