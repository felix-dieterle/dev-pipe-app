#!/bin/bash
# Sync script for dev-pipe PHP files to mama-razzi
# Run via cron: 0 * * * * /path/to/sync-devpipe.sh

DEVPIPE_DIR="/E/dev-pipe-app"
TARGET_DIR="/tmp/scp41405/mama-razzi.org/public/apps/devpipe"

echo "Syncing dev-pipe PHP files to mama-razzi..."

# Create directory if not exists
mkdir -p "$TARGET_DIR"

# Copy PHP files
cp "$DEVPIPE_DIR/api.php" "$TARGET_DIR/"

echo "Done. Files synced:"
ls -la "$TARGET_DIR/"
