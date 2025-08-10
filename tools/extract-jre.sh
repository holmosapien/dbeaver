#!/bin/bash

# Extract JRE after DBeaver build completes

set -e

PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"

JRE_ARCHIVE="$PROJECT_ROOT/jdk/$( cat "$PROJECT_ROOT/jdk/jdk.json" | jq -r .macos.filename )"
DBEAVER_APP="$PROJECT_ROOT/product/community/target/products/org.jkiss.dbeaver.core.product/macosx/cocoa/aarch64/DBeaver.app"

echo "Extracting JRE to DBeaver.app..."

if [ ! -f "$JRE_ARCHIVE" ]; then
    echo "Error: JRE archive not found at $JRE_ARCHIVE"
    exit 1
fi

if [ ! -d "$DBEAVER_APP" ]; then
    echo "Error: DBeaver.app not found at $DBEAVER_APP"
    exit 1
fi

# Create JRE directory
mkdir -p "$DBEAVER_APP/Contents/Eclipse/jre"

# Extract JRE
echo "Extracting JRE contents..."
tar --strip-components 2 -xzf "$JRE_ARCHIVE" -C "$DBEAVER_APP/Contents/Eclipse/jre"

echo "JRE extraction complete!"
echo "JRE location: $DBEAVER_APP/Contents/Eclipse/jre"
