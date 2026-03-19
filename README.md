# zenos-infra
Wrangler config, CI/CD, IaC scripts

## Local Environment Bootstrap

Use the interactive script below to configure local credentials and start services.

```bash
./scripts/setup-local-dev.sh
```

What it does:
- Prompts for backend credentials (`GOOGLE_CLIENT_ID`, `JWT_SECRET`)
- Writes backend local config: `../zenos-backend/.env`
- Writes frontend local config: `../zenos-frontend/.env.local`
- Optionally installs dependencies for backend/frontend/db repos
- Optionally starts backend (`wrangler dev --env dev`) and frontend (`pnpm dev --host`)

Generated runtime files/logs are stored under `.local-dev/` and are ignored by git.
