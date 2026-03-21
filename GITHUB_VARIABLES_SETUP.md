# GitHub Environments & Variables Setup

## Overview
This document outlines the GitHub environment-specific variables required for the Zenos CI/CD pipeline. Different environments (staging, production) have their own API endpoints.

## Setup Instructions

### Step 1: Create GitHub Environments

Go to your repository: `https://github.com/zenos-inc/zenos/settings/environments`

1. Click **New environment**
2. Enter name: `staging`
3. Click **Configure environment**
4. Click **Add variable**
   - Name: `VITE_API_BASE_URL`
   - Value: `https://staging.api.zenos.work`
5. Click **Add variable** to save

Repeat for a `production` environment:
1. Click **New environment**
2. Enter name: `production`
3. Click **Configure environment**
4. Click **Add variable**
   - Name: `VITE_API_BASE_URL`
   - Value: `https://api.zenos.work`
5. Click **Add variable** to save

### Step 2: Verify Workflow Configuration

The `frontend-ci-deploy.yml` workflow now:
1. Reads `inputs.environment` (e.g., "staging" or "production")
2. Sets the ci job's `environment: ${{ inputs.environment }}`
3. Accesses the environment-specific `VITE_API_BASE_URL` variable during build:
   ```yaml
   env:
     VITE_API_BASE_URL: ${{ vars.VITE_API_BASE_URL }}
   ```

When the ci job runs with `environment: staging`, GitHub injects that environment's `VITE_API_BASE_URL` variable.

### Step 3: Caller Workflow Integration

The caller workflows (e.g., `frontend-ci-cd.yml` in `zenos-frontend`) pass `inputs.environment`:

```yaml
- name: Run frontend CI/CD
  uses: zenos-work/zenos-infra/.github/workflows/frontend-ci-deploy.yml@development
  with:
    environment: staging  # or 'production'
    run_deploy: true
```

## How It Works

1. **Caller** passes `environment: staging` or `environment: production` to the reusable workflow
2. **Reusable workflow** ci job sets `environment: ${{ inputs.environment }}`
3. **GitHub Actions** provides that environment's variables to job steps
4. **Build step** reads `${{ vars.VITE_API_BASE_URL }}` from that environment
5. **Result**: Frontend builds with the correct API endpoint for that environment

## Verification

After setting environments and variables:

1. Push a commit and trigger a workflow with `inputs.environment: staging`
2. Check the GitHub Actions workflow run
3. View the "Build frontend" step output
4. Confirm `VITE_API_BASE_URL` is correctly set to `https://staging.api.zenos.work`
5. Verify the staging frontend loads without `undefined` API URLs

## Security Notes

- Environment variables are visible to anyone with repository read access
- Use environment secrets (not variables) for API keys and credentials
- Different environments can have different protection rules (require approvals, etc.)
