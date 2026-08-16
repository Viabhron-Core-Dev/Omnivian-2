import re

with open('gradle/libs.versions.toml', 'r') as f:
    content = f.read()

if 'browser =' not in content:
    content = content.replace('[versions]', '[versions]\nbrowser = "1.8.0"')
    content = content.replace('[libraries]', '[libraries]\nandroidx-browser = { group = "androidx.browser", name = "browser", version.ref = "browser" }')

with open('gradle/libs.versions.toml', 'w') as f:
    f.write(content)

with open('app/build.gradle.kts', 'r') as f:
    build_content = f.read()

if 'libs.androidx.browser' not in build_content:
    build_content = build_content.replace('dependencies {', 'dependencies {\n    implementation(libs.androidx.browser)')

with open('app/build.gradle.kts', 'w') as f:
    f.write(build_content)
