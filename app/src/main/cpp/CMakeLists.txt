cmake_minimum_required(VERSION 3.22.1)

project("llama_bridge")

# Performance flags for ARM NEON and vectorization
if(ANDROID_ABI STREQUAL "arm64-v8a")
    add_compile_options(-O3 -fvectorize -funroll-loops)
endif()

# Enable real llama.cpp compilation permanently in source control
option(USE_REAL_LLAMA "Fetch and compile real llama.cpp" ON)

if(USE_REAL_LLAMA)
    add_compile_definitions(USE_REAL_LLAMA)
    
    set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
    set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

    include(FetchContent)
    FetchContent_Declare(
        llama
        GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
        GIT_TAG b4800
        GIT_SHALLOW TRUE
    )
    FetchContent_MakeAvailable(llama)
    
    add_library(llama_bridge SHARED llama_bridge.cpp)
    target_link_libraries(llama_bridge llama log)
else()
    add_library(llama_bridge SHARED llama_bridge.cpp)
    target_link_libraries(llama_bridge log)
endif()

