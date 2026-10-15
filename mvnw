#!/bin/sh
DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -x "$DIR/maven/apache-maven-3.9.9/bin/mvn" ]; then
    exec "$DIR/maven/apache-maven-3.9.9/bin/mvn" "$@"
else
    exec mvn "$@"
fi
