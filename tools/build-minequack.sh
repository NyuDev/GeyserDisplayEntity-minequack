#!/bin/bash
# Construit GeyserDisplayEntity-<version>.jar sans Gradle, comme pour le reseau MineQuack.
# Usage : tools/build-minequack.sh <Geyser-Velocity.jar> <velocity.jar> <GeyserDisplayEntityPack.mcpack> [version]
# Il faut javac et jar (JDK 21 ou plus). Le resultat va dans build-minequack/out/.
set -euo pipefail

GEYSER="$1"
VELOCITY="$2"
PACK="$3"
VERSION="${4:-1.0.15-minequack.6}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
B="$ROOT/build-minequack"
SEP=":"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";" ;; esac

rm -rf "$B"
mkdir -p "$B/src" "$B/classes" "$B/out"
cp -r "$ROOT/src/main/java/." "$B/src/"

# Geyser-Velocity embarque configurate sous un autre nom : on y pointe au lieu d'en livrer une copie
find "$B/src" -name '*.java' -exec sed -i \
    's/org\.spongepowered\.configurate/org.geysermc.geyser.platform.velocity.shaded.org.spongepowered.configurate/g' {} +

# Chemins relatifs : javac sous Windows ne lit pas les chemins /c/... d'un fichier @liste
cd "$B"
find src -name '*.java' > files.txt
javac --release 21 -nowarn -encoding UTF-8 -proc:none \
    -cp "$GEYSER$SEP$VELOCITY" -d classes @files.txt
cd "$ROOT"

cp -r "$ROOT/src/main/resources/." "$B/classes/"
sed -i "s/\${version}/$VERSION/" "$B/classes/extension.yml"
cp "$PACK" "$B/classes/GeyserDisplayEntityPack.mcpack"

jar --create --file "$B/out/GeyserDisplayEntity-$VERSION.jar" -C "$B/classes" .
echo "$B/out/GeyserDisplayEntity-$VERSION.jar"
