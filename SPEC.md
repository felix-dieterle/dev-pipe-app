# Dev-Pipe App Specification

## Overview
Android app to control the dev-pipe pipeline system.

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

### Component Status
```json
{
  "components": [
    { "name": "server", "status": "online|off|error" },
    { "name": "docker", "status": "online|off|error" },
    { "name": "github", "status": "online|off|error" },
    { "name": "store", "status": "online|off|error" }
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
