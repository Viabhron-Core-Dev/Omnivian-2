path = 'app/src/main/cpp/CMakeLists.txt'
with open(path, 'r') as f:
    content = f.read()
content = content.replace('option(USE_REAL_LLAMA "Fetch and compile real llama.cpp" OFF)', 'option(USE_REAL_LLAMA "Fetch and compile real llama.cpp" ON)')
with open(path, 'w') as f:
    f.write(content)
