#!/bin/bash

# This script is run from maven.  See the exec-maven-plugin block in the pom.xml file.

package=jackfruit
srcFile="../java/jackfruit/JackfruitVersion.java"

cd $(dirname "$0")

date=$(date -u +"%y.%m.%d")

rev=$(git rev-parse --verify --short HEAD)
if [ $? -gt 0 ]; then
    lastCommit=$(date -u +"%y.%m.%d")
    rev="UNVERSIONED"
else
    lastCommit=$(git log -1 --format=%cd --date=format:%y.%m.%d)
    rev=$(git rev-parse --verify --short HEAD)

    if [[ $(git diff --stat) != '' ]]; then
        if [[ $(git status -s | grep -v pom.xml | grep -v pom.bak | grep -v .m2 | grep -v $srcFile) != '' ]]; then
            rev=${rev}M
        fi
    fi
fi

mkdir -p $(dirname "$srcFile")

touch $srcFile

cat <<EOF > $srcFile

package jackfruit;

public class JackfruitVersion {
    public final static String lastCommit = "$lastCommit";
    // an M at the end of gitRevision means this was built from a "dirty" git repository
    public final static String rev = "$rev";
    public final static String packageName = "$package";
    public final static String dateString = "$date";
}

EOF
