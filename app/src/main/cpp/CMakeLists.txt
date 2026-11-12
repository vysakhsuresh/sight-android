cmake_minimum_required(VERSION 3.22.1)
project(ikshana_vision C CXX)

# llama.cpp, pinned as a submodule. Only what the bridge needs: no server, no tests, no network
# (the app has no INTERNET permission; a built-in downloader would be dead code at best).
set(LLAMA_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/llama.cpp)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
# The multimodal library alone, without the tools around it (it is only defined when asked for).
set(LLAMA_BUILD_MTMD ON CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
# The moto g84's cores (A78 and A55) have dot-product and fp16 arithmetic; every arm64 phone that
# runs a model of this size has them too. Without the flag the matrix kernels are several times slower.
set(GGML_CPU_ARM_ARCH "armv8.2-a+dotprod+fp16" CACHE STRING "" FORCE)
add_subdirectory(${LLAMA_ROOT} llama EXCLUDE_FROM_ALL)

add_library(ikshana_vision SHARED vision.cpp)
target_link_libraries(ikshana_vision PRIVATE mtmd llama ggml android log)
target_include_directories(ikshana_vision PRIVATE ${LLAMA_ROOT}/include ${LLAMA_ROOT}/tools/mtmd)
