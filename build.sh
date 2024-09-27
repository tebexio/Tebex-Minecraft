#!/bin/bash

# Runs all Gradle tasks
./gradlew

# Removes previous builds
rm -f build/libs/*

# Move all successful builds to build output folder
mv bukkit/build/libs/tebex*.jar build/libs/
mv bungeecord/build/libs/tebex*.jar build/libs/
mv fabric-1.20.4/build/libs/tebex*.jar build/libs/
mv velocity/build/libs/tebex*.jar build/libs/
mv sdk/build/libs/tebex*.jar build/libs/