#!/bin/bash

# This script is run from maven.  See the exec-maven-plugin block in the pom.xml file.

cd $(dirname $0)

rev=$(git rev-parse --verify --short=8 HEAD)
if [ $? -gt 0 ]; then
    rev="UNKNOWN"
fi

branch=$(git symbolic-ref --short HEAD)
if [ $? -gt 0 ]; then
    branch="UNKNOWN"
fi

date=$(date -u +"%y.%m.%d")

package=jackfruit
srcFile="../java/jackfruit/JackfruitVersion.java"
mkdir -p $(dirname $srcFile)

touch $srcFile

cat <<EOF > $srcFile

package jackfruit;

public class JackfruitVersion {
    public final static String rev = new String("$rev");
    public final static String packageName = new String("$package");
    public final static String dateString = new String("$date");
    public final static String branch = new String("$branch");
}

EOF