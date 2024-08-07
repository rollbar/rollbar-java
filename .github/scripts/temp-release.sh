export GPG_KEY_LOCATION="$(realpath "$GPG_KEY_LOCATION")"

echo "Doing SNAPSHOT release..."

./gradlew -Dorg.gradle.internal.http.socketTimeout=300000 -Dorg.gradle.internal.http.connectionTimeout=300000 publishToSonatype

echo "Release done!"
