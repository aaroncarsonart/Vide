#!/bin/zsh

# Setup some directories.
RESOURCE_DIR="target/package/macos"
ICONSET_DIR="target/Vide.iconset"

mkdir -p $RESOURCE_DIR
mkdir -p $ICONSET_DIR

# Build the app icon for the installer.
ICON_SRC_PATH="src/main/resources/images/vide_icon-512x512.png"
ICON_DST_PATH="$ICONSET_DIR/icon_512x512.png"
ICON_OUT_PATH="target/Vide.icns"

cp $ICON_SRC_PATH $ICON_DST_PATH
iconutil --convert icns $ICONSET_DIR --output $ICON_OUT_PATH

# Build the background image for the installer.
BG_OUTFILE="$RESOURCE_DIR/Vide-background.png"
BG_OUTFILE_DARK="$RESOURCE_DIR/Vide-background-darkAqua.png"

sips --resampleHeightWidth 120 120 --padToHeightWidth 175 175 $ICON_SRC_PATH \
--out $BG_OUTFILE > /dev/null
cp $BG_OUTFILE $BG_OUTFILE_DARK

# Run jpackage to build an app installer.
APP_NAME="Vide"
APP_VERSION="`mvn help:evaluate -Dexpression=project.version -q -DforceStdout`"
DEST_DIR="target/"
JAR_PATH="target/Vide-$APP_VERSION.jar"

jpackage --name $APP_NAME --input . --main-jar $JAR_PATH --app-version \
$APP_VERSION --icon $ICON_OUT_PATH --dest $DEST_DIR --resource-dir \
$RESOURCE_DIR --type pkg
