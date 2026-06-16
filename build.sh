#!/bin/bash

# Runs all Gradle tasks
./gradlew

# Removes previous builds
rm -f build/libs/*

# Move all successful builds to build output folder
mv bukkit/build/libs/tebex*.jar build/libs/
mv bungeecord/build/libs/tebex*.jar build/libs/
mv fabric-26.2/build/libs/tebex*.jar build/libs/
mv neoforge-26.1/build/libs/tebex*.jar build/libs/
mv forge-26.1/build/libs/tebex*.jar build/libs/
mv folia/build/libs/tebex*.jar build/libs/
mv velocity/build/libs/tebex*.jar build/libs/
mv sdk/build/libs/tebex*.jar build/libs/
