#!/bin/bash
# Run the IntelliJ plugin in a sandbox IDE

export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew runIde
