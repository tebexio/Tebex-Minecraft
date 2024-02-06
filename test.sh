#!/bin/bash

# Tests all variants of the plugin

# Test parameters
TEBEX_TEST_SECRET_KEY="" # normal game server secret key to test setting secret

# URLs to the JAR files we can download
BUKKIT_JAR_URL='https://download.getbukkit.org/craftbukkit/craftbukkit-1.20.4.jar'
BUNGEE_JAR_URL='https://ci.md-5.net/job/BungeeCord/lastSuccessfulBuild/artifact/bootstrap/target/BungeeCord.jar'
SPIGOT_JAR_URL='https://download.getbukkit.org/spigot/spigot-1.20.4.jar'
PAPER_JAR_URL='https://api.papermc.io/v2/projects/paper/versions/1.20.4/builds/381/downloads/paper-1.20.4-381.jar'
PURPUR_JAR_URL='https://api.purpurmc.org/v2/purpur/1.20.4/latest/download'
VELOCITY_JAR_URL='https://api.papermc.io/v2/projects/velocity/versions/3.3.0-SNAPSHOT/builds/316/downloads/velocity-3.3.0-SNAPSHOT-316.jar'

# Record the start time
SECONDS=0

function downloadJar() {
    local folder="$1"
    local url="$2"
    local filename="${url##*/}"

    # Ensure the folder path is within the current working directory
    if [[ "$folder" = /* || "$folder" = ../* ]]; then
        echo "Error: The specified folder must be within the current working directory."
        return 1
    fi

    # Check if the folder exists
    if [[ -d "$folder" ]]; then
        echo "Deleting existing folder: $folder"
        #rm -rf "$folder"
    fi

    # Create the folder
    mkdir -p "$folder"

    # Download the file into the specified folder
    echo "Downloading file to $folder/$filename"
    wget -P "$folder" "$url"
}

function testCommands() {
  local javaPid=$1
  local pipe=$2
  local command="/tebex help"
}

function testJar() {
  local folder="$1"
  local jarName="$2"

  echo "Testing .jar '$jarName' in '$folder'"
  cd $folder
  echo "eula=true" >> "eula.txt"

  java -Xmx2G -jar "$jarName" nogui
  javaPid=$!
  echo "Waiting for server startup..."

  mkdir -p "./logs/"
  touch "./logs/latest.log"
  tail -f "./logs/latest.log" | while read LINE; do
      echo "$LINE" | grep "Welcome to Tebex" &> /dev/null
      if [ $? -eq 0 ]; then
          echo "> Tebex loaded successfully, terminating server process..."
          #testCommands $javaPid $pipe
          sleep 2
          kill $javaPid
          break
      fi
  done

  kill $javaPid
  echo "Test completed."
  cd ..
}

function installPluginToFolder() {
  pluginType="$1" #ex. "bukkit", corresponding with type in build/libs: tebex-{pluginType}-2.0.0.jar
  toFolder="$2" # root minecraft server folder containing /plugins

  mkdir -p "$toFolder/plugins"
  cp "../build/libs/tebex-$pluginType-2.0.2.jar" "$toFolder/plugins"
}

function copyWorlds() {
  from="./$1"
  to="./$2"

  cp -r "$1/world" "$2/world"
  cp -r "$1/world_nether" "$2/world_nether"
  cp -r "$1/world_the_end" "$2/world_the_end"
}
# Initial setup
rm -rf test
mkdir test
cd test

# Test paper first as it's the fastest to generate the world. We'll copy the world to all other folders to prevent waiting
# on world generation while testing.

# Paper
echo "Testing Paper..."
downloadJar "paper" "$PAPER_JAR_URL"
installPluginToFolder "bukkit" "./paper"
testJar "paper" ${PAPER_JAR_URL##*/} # take filename from end of url

# Bukkit
#echo "Testing Bukkit..."
#downloadJar "bukkit" "$BUKKIT_JAR_URL"
#installPluginToFolder "bukkit" "./bukkit"
#copyWorlds "paper" "bukkit"
#testJar "bukkit" ${BUKKIT_JAR_URL##*/} # take filename from end of url

# Bungee
#echo "Testing BungeeCord..."
#downloadJar "bungeecord" "$BUNGEE_JAR_URL"
#installPluginToFolder "bungeecord" "./bungeecord"
#testJar "bungeecord" ${BUNGEE_JAR_URL##*/} # take filename from end of url

# Spigot
#echo "Testing Spigot..."
#downloadJar "spigot" "$SPIGOT_JAR_URL"
#installPluginToFolder "bukkit" "./spigot"
#copyWorlds "paper" "spigot"
#testJar "spigot" ${SPIGOT_JAR_URL##*/} # take filename from end of url

# Purpur
#echo "Testing Purpur..."
#downloadJar "purpur" "$PURPUR_JAR_URL"
#installPluginToFolder "bukkit" "./purpur"
#copyWorlds "paper" "spigot"
#testJar "purpur" ${PURPUR_JAR_URL##*/} # take filename from end of url

# Velocity
#echo "Testing Velocity..."
#downloadJar "velocity" "$VELOCITY_JAR_URL"
#installPluginToFolder "velocity" "./velocity"
#testJar "velocity" ${VELOCITY_JAR_URL##*/} # take filename from end of url

elapsedTime=$SECONDS
echo "Testing completed in $elapsedTime seconds"
exit