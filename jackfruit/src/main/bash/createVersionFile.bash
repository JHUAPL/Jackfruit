#!/bin/bash

# This script is run from maven.  See the exec-maven-plugin block in the pom.xml file.

package=jackfruit
srcFile="../java/jackfruit/JackfruitVersion.java"

cd "$(dirname "$0")"

version=$1

date=$(date -u +"%y.%m.%d")

mkdir -p "$(dirname "$srcFile")"

touch $srcFile

cat <<EOF > $srcFile

package jackfruit;

public class JackfruitVersion {
    public final static String version = "$version";
    public final static String packageName = "$package";
    public final static String dateString = "$date";
}

EOF
