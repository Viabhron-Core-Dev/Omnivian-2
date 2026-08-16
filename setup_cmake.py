import re

path = 'app/build.gradle.kts'
with open(path, 'r') as f:
    content = f.read()

# Add externalNativeBuild config to defaultConfig
if 'externalNativeBuild {' not in content:
    replacement = """    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    externalNativeBuild {
      cmake {
        cppFlags += "-std=c++17"
        // Target modern architectures to keep build fast
        abiFilters.add("arm64-v8a")
        abiFilters.add("x86_64")
      }
    }
  }"""
    content = content.replace('    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"\n  }', replacement)

# Add externalNativeBuild block to android
if 'path = file("src/main/cpp/CMakeLists.txt")' not in content:
    replacement2 = """  buildFeatures {
    compose = true
    buildConfig = true
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }"""
    content = content.replace("""  buildFeatures {
    compose = true
    buildConfig = true
  }""", replacement2)

with open(path, 'w') as f:
    f.write(content)
print("Updated build.gradle.kts")
