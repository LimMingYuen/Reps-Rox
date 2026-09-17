# CLAUDE.md

## Git workflow

- After a pull request is created and merged into `main`, always delete the feature branch — both remote and local:
  - `gh pr merge <number> --merge --delete-branch` (or `git push origin --delete <branch>` if already merged)
  - `git checkout main && git pull && git branch -d <branch>`
