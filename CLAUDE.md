# Hubitat Resideo T10 Integration

## Project Overview
This is a Hubitat Elevation integration for Resideo T10 smart thermostats using the official Honeywell/Resideo API.

**Tech Stack:**
- Groovy: Hubitat app (`hubitat-resideo-app.groovy`) and driver (`hubitat-resideo-driver.groovy`)
- Python: Backend API client, OAuth helpers, and CLI tools
- GitHub: https://github.com/mathewbeall/hubitat-resideo_T10-integration

## Issue Tracking

**When asked about issues, ALWAYS check GitHub first:**
```bash
gh issue list --repo mathewbeall/hubitat-resideo_T10-integration
```

## Agent Workflow Requirements

**IMPORTANT: When working on new features or issues, ALWAYS use agents:**

### 1. Planning Agent (Required First Step)
Before implementing any feature or fix, use the Plan agent to design the approach:
```
Task tool with subagent_type='Plan'
```
- Analyze requirements and existing code
- Identify affected files (Groovy and/or Python)
- Design implementation approach
- Present plan for user approval

### 2. Coding Agent (After Plan Approval)
After planning is approved, use a general-purpose agent for implementation:
```
Task tool with subagent_type='general-purpose'
```
- Implement the planned changes
- Follow existing code patterns
- Test changes where possible

### 3. Explore Agent (For Research)
For codebase exploration and understanding:
```
Task tool with subagent_type='Explore'
```
- Finding relevant code sections
- Understanding existing patterns
- Researching how features work

## Key Files

### Groovy (Hubitat)
- `hubitat-resideo-app.groovy` - Main parent app (OAuth, discovery, API calls)
- `hubitat-resideo-driver.groovy` - Thermostat device driver

### Python
- `resideo_direct_api.py` - Direct Resideo API client
- `thermostat_controller.py` - Seam API controller
- `oauth_helper.py` - OAuth 2.0 utilities
- `interactive_direct_control.py` - CLI testing tool

### Configuration
- `packageManifest.json` - Hubitat Package Manager config
- `repository.json` - GitHub package repository

## Coding Standards
- Groovy: Follow Hubitat conventions (preferences, capabilities, commands)
- Python: Use existing patterns in resideo_direct_api.py
- Always maintain OAuth token handling properly
- Test API changes with the interactive CLI tool when possible

## Releasing New Versions

**IMPORTANT: When merging features to main, ALWAYS update for Hubitat Package Manager:**

1. Update `packageManifest.json`:
   - Bump `version` (follow semver: major.minor.patch)
   - Update `dateReleased` to current date (YYYY-MM-DD)
   - Update `releaseNotes` with summary of changes

2. Update `README.md`:
   - Add entry to Version History section

3. Commit and push to main - HPM users will see the update automatically
