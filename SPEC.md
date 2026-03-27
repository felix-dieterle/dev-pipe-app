# Dev-Pipe App Specification

## What is Dev-Pipe?

**Dev-Pipe** is a self-hosted AI-powered coding pipeline. It allows you to:

1. **Create a task** (e.g., "Create a hello world Python script")
2. **AI automatically implements it** - Creates repo, writes code, commits, pushes, and creates a PR
3. **You review and merge** - AI creates a Pull Request for you to review

Think of it as a personal AI developer that creates GitHub Pull Requests for you.

### Example Flow

```
User: "Create a login form in React"
    │
    ▼
Dev-Pipe: Creates GitHub repo
    │
    ▼
Dev-Pipe: AI writes code (using opencode with big-pickle model)
    │
    ▼
Dev-Pipe: Creates Pull Request
    │
    ▼
User: Reviews PR → Merges ✅
```

## System Overview

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   GitHub     │◄───►│  Dev-Pipe   │◄───►│   Docker    │
│   API        │     │   Server    │     │  (runner)   │
└──────────────┘     └──────────────┘     └──────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │    Redis     │
                     │   (state)    │
                     └──────────────┘
```

### Components

1. **Dev-Pipe Server** (Go)
   - REST API for sessions/jobs
   - Webhook receiver for GitHub events
   - Job executor that runs pipeline steps

2. **Docker Runner**
   - Runs each pipeline step in isolated containers
   - Uses debian-based image with opencode AI CLI
   - Security: memory limit, no network for untrusted steps

3. **Redis**
   - Stores session and job state
   - Persists pipeline history

4. **GitHub**
   - Creates repositories via API
   - Creates Pull Requests
   - Uses Personal Access Token (PAT)

## Pipeline Steps

When a session is approved, this happens automatically:

```
1. create-repo    → Creates GitHub repository
2. checkout      → Clones repo to workspace
3. opencode      → AI writes code (big-pickle model)
4. git-commit    → Commits changes to new branch
5. git-push      → Pushes branch to remote
6. create-pr     → Creates Pull Request
```

## Session States

```
planning ──(approve)──► approved ──(start)──► implementing ──(PR created)──►
                                              awaiting_approval ──(merge)──► done
        ▲                         │
        └─────────────────────────┘ (reject)
```

- **planning**: Just created, waiting for approval
- **approved**: Approved, pipeline starts automatically
- **implementing**: Pipeline is running
- **awaiting_approval**: PR created, waiting for user to merge
- **done**: Merged successfully

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Android    │────►│  PHP API    │────►│  Dev-Pipe   │
│  App        │     │  (Token)    │     │  Server     │
└─────────────┘     └─────────────┘     └─────────────┘
```

### Backend URL Discovery
- PHP endpoint at `public/apps/devpipe/api.php` returns current dev-pipe URL
- Stored URL is cached locally on first launch
- User can manually update URL in settings

### Authentication
- Single API token configured in app (first launch or settings)
- Token stored securely using Android Keystore
- Token sent as `Authorization: Bearer <token>` header

## Features

### 1. Session Management
- **Create Session**: Enter task description, repo owner/name
- **List Sessions**: Show all sessions with status
- **View Session**: Details + job status
- **Approve Session**: One-tap approval
- **Reject Session**: Reject with optional reason
- **Merge PR**: Merge approved PR (optional)

### 2. Dashboard
- Component status (server, docker, github, store)
- Session counts by status
- Job counts by status

### 3. Settings
- Backend URL (auto-discovered or manual)
- API Token configuration
- Theme (light/dark/system)

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/health | Component status |
| GET | /api/status | Dashboard summary |
| GET | /api/sessions | List sessions |
| POST | /api/sessions | Create session |
| GET | /api/sessions/{id} | Get session |
| POST | /api/sessions/{id} | Approve/reject/merge |
| GET | /api/jobs | List jobs |
| GET | /api/jobs/{id} | Get job |

### Authentication
All endpoints require `Authorization: Bearer <token>` header.

## PHP Backend (public/apps/devpipe/api.php)

### Discovery Endpoint
```
GET /api.php?token=<fixed_token>&action=get_url
```
Returns: `{ "url": "https://..." }`

### Actions
- `get_url` - Returns cached dev-pipe URL
- `set_url?url=<url>` - Updates cached URL (admin)
- `status` - Checks if dev-pipe is reachable

### Token Verification
- Fixed token in PHP config (DEV_PIPE_TOKEN)
- Must match for URL discovery

## Data Models

### Session
```json
{
  "session_id": "1234567890",
  "name": "my-task",
  "description": "Create hello.txt",
  "status": "planning|approved|implementing|awaiting_approval|done",
  "repo": { "owner": "user", "name": "repo" },
  "pr_url": "https://github.com/...",
  "created_at": "2026-01-01T00:00:00Z",
  "approved_at": "2026-01-01T00:00:00Z"
}
```

### Job
```json
{
  "id": "1234567890",
  "name": "session-job-my-task",
  "status": "pending|running|success|failed",
  "steps": [
    { "name": "Create Repository", "status": "success" },
    { "name": "Checkout", "status": "success" },
    { "name": "OpenCode Task", "status": "success" },
    { "name": "Commit", "status": "success" },
    { "name": "Push", "status": "success" },
    { "name": "Create PR", "status": "success" }
  ]
}
```

### Component Status
```json
{
  "components": [
    { "name": "server", "status": "online" },
    { "name": "docker", "status": "online" },
    { "name": "github", "status": "online" },
    { "name": "store", "status": "online" }
  ]
}
```

## UI/UX

### Screens
1. **Dashboard** - Overview + quick actions
2. **Sessions** - List of sessions with filters
3. **Session Detail** - Full session info + actions
4. **Create Session** - Form to create new session
5. **Settings** - URL, token, theme

### Navigation
- Bottom navigation: Dashboard | Sessions | Settings
- FAB on Sessions for creating new session

### Theme
- Material Design 3
- Primary: Purple (#6750A4)
- Secondary: Teal (#03DAC6)
- Dark mode support

## Tech Stack
- Kotlin + Jetpack Compose
- Retrofit for API calls
- Hilt for DI
- Coroutines + Flow
- DataStore for preferences
- Android Keystore for token storage

## Build Config
- minSdk: 26
- targetSdk: 34
- compileSdk: 34
