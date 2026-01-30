#!/bin/bash
# Run the IntelliJ plugin in a sandbox IDE
# Requires: Java 17

# macOS: automatically find Java 17
if [[ "$OSTYPE" == "darwin"* ]] && command -v /usr/libexec/java_home &> /dev/null; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
fi

# Verify Java version
if ! java -version 2>&1 | grep -q "version \"17"; then
    echo "⚠️  Java 17 is required but not found."
    echo "Please install Java 17 and set JAVA_HOME, or run:"
    echo "  export JAVA_HOME=/path/to/java17"
    exit 1
fi

./gradlew runIde
