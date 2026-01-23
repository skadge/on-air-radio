#!/bin/sh

#
# Gradle start up script for POSIX generated
#

# Resolve links
APP_HOME="$(cd "$(dirname "$0")/" && pwd -P)" || exit

# Determine the Java command to use to start the JVM
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Default JVM opts
DEFAULT_JVM_OPTS='-Xmx64m -Xms64m'

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
