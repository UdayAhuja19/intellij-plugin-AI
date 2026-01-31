#!/bin/bash
# Run the IntelliJ plugin in a sandbox IDE
# Works on macOS and Linux
# Note: Build requires Java 17 (Gradle/Kotlin toolchain limitation)

set -e

# Function to find Java home
find_java() {
    local version=$1
    
    # macOS
    if [[ "$OSTYPE" == "darwin"* ]] && command -v /usr/libexec/java_home &> /dev/null; then
        /usr/libexec/java_home -v "$version" 2>/dev/null && return 0
    fi
    
    # Linux: Check common locations
    if [[ -d "/usr/lib/jvm/java-${version}-openjdk" ]]; then
        echo "/usr/lib/jvm/java-${version}-openjdk"
        return 0
    fi
    
    if [[ -d "/usr/lib/jvm/java-${version}" ]]; then
        echo "/usr/lib/jvm/java-${version}"
        return 0
    fi
    
    # Check for Oracle JDK on Linux
    if [[ -d "/usr/lib/jvm/jdk-${version}" ]]; then
        echo "/usr/lib/jvm/jdk-${version}"
        return 0
    fi
    
    # SDKMAN
    if [[ -d "$HOME/.sdkman/candidates/java" ]]; then
        local sdkman_java=$(ls -d "$HOME/.sdkman/candidates/java/"*"$version"* 2>/dev/null | head -1)
        if [[ -n "$sdkman_java" ]]; then
            echo "$sdkman_java"
            return 0
        fi
    fi
    
    return 1
}

echo "🔍 Looking for Java..."

# For building, we need Java 17 (Gradle/Kotlin limitation with Java 25)
# Try to find Java 17 first for the build
FOUND_JAVA=""
for version in 17 21; do
    FOUND_JAVA=$(find_java $version 2>/dev/null) || true
    if [[ -n "$FOUND_JAVA" && -d "$FOUND_JAVA" ]]; then
        export JAVA_HOME="$FOUND_JAVA"
        echo "✓ Found Java $version at: $JAVA_HOME"
        break
    fi
done

# If no Java 17/21 found, check for Java 25 with a warning
if [[ -z "$JAVA_HOME" ]]; then
    FOUND_JAVA=$(find_java 25 2>/dev/null) || true
    if [[ -n "$FOUND_JAVA" && -d "$FOUND_JAVA" ]]; then
        echo "⚠️  Only Java 25 found. Gradle may have compatibility issues."
        echo "   For best results, install Java 17: brew install openjdk@17"
        export JAVA_HOME="$FOUND_JAVA"
    fi
fi

# If JAVA_HOME is still not set, try system default
if [[ -z "$JAVA_HOME" ]]; then
    if command -v java &> /dev/null; then
        # Try to get JAVA_HOME from java command
        JAVA_BIN=$(readlink -f "$(which java)" 2>/dev/null || which java)
        JAVA_HOME=$(dirname "$(dirname "$JAVA_BIN")")
        export JAVA_HOME
        echo "✓ Using system Java at: $JAVA_HOME"
    else
        echo "❌ Java not found. Please install Java 17."
        echo "   - macOS: brew install openjdk@17"
        echo "   - Linux: sudo apt install openjdk-17-jdk"
        exit 1
    fi
fi

# Verify Java version
JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "📦 Java version: $JAVA_VERSION"

if [[ "$JAVA_VERSION" -lt 17 ]]; then
    echo "❌ Java 17 or higher is required (found version $JAVA_VERSION)"
    exit 1
fi

if [[ "$JAVA_VERSION" -ge 25 ]]; then
    echo ""
    echo "⚠️  Java 25 detected. Gradle 8.x doesn't fully support Java 25 yet."
    echo "   Attempting build anyway... If it fails, please install Java 17:"
    echo "   - macOS: brew install openjdk@17"
    echo "   - Linux: sudo apt install openjdk-17-jdk"
    echo ""
fi

echo "🚀 Starting IntelliJ plugin sandbox..."
echo ""

# Run Gradle with the found Java
./gradlew runIde
