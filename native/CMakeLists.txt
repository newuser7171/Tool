cmake_minimum_required(VERSION 3.22)
project(jevtool LANGUAGES CXX)
add_library(jevtool SHARED jevtool.cpp)
target_compile_features(jevtool PRIVATE cxx_std_17)
target_link_libraries(jevtool PRIVATE log dl)

option(JEV_WITH_IMGUI "Build optional ImGui panel using vendored Dear ImGui" OFF)
if(JEV_WITH_IMGUI)
    set(IMGUI_DIR "${CMAKE_CURRENT_SOURCE_DIR}/third_party/imgui" CACHE PATH "Dear ImGui source")
    if(NOT EXISTS "${IMGUI_DIR}/imgui.cpp")
        message(FATAL_ERROR "Dear ImGui not found at ${IMGUI_DIR}; vendor it before enabling JEV_WITH_IMGUI")
    endif()
    target_sources(jevtool PRIVATE
        jev_imgui.cpp
        jev_gl_surface.cpp
        jev_overlay.cpp
        jev_unity_bridge.cpp
        "${IMGUI_DIR}/imgui.cpp"
        "${IMGUI_DIR}/imgui_draw.cpp"
        "${IMGUI_DIR}/imgui_tables.cpp"
        "${IMGUI_DIR}/imgui_widgets.cpp")
    target_include_directories(jevtool PRIVATE "${IMGUI_DIR}" "${IMGUI_DIR}/backends")
    target_compile_definitions(jevtool PRIVATE IMGUI_IMPL_OPENGL_ES3)
    target_sources(jevtool PRIVATE "${IMGUI_DIR}/backends/imgui_impl_opengl3.cpp")
    target_link_libraries(jevtool PRIVATE EGL GLESv3 android)
endif()
