#include <jni.h>
#include <GLES3/gl3.h>
#include "imgui.h"
#include "backends/imgui_impl_opengl3.h"

void drawJevImGuiPanel();
static int width = 1, height = 1;
static bool initialized = false;
static float uiScale = 2.7f;
static bool down = false;
static float touchX = 0, touchY = 0;

extern "C" JNIEXPORT void JNICALL Java_com_jev_toolkit_MainActivity_nativeInit(JNIEnv*, jobject) {
    if (initialized) { ImGui_ImplOpenGL3_Shutdown(); ImGui::DestroyContext(); initialized = false; }
    IMGUI_CHECKVERSION();
    ImGui::CreateContext();
    ImGui::GetIO().DisplaySize = ImVec2((float)width, (float)height);
    ImGui::StyleColorsDark();
    ImGui::GetStyle().ScaleAllSizes(uiScale);
    ImGuiIO& io = ImGui::GetIO();
    io.Fonts->Clear();
    ImFontConfig config;
    config.SizePixels = 13.0f * uiScale;
    io.Fonts->AddFontDefault(&config);
    initialized = ImGui_ImplOpenGL3_Init("#version 300 es");
}
extern "C" JNIEXPORT void JNICALL Java_com_jev_toolkit_MainActivity_nativeResize(JNIEnv*, jobject, jint w, jint h) {
    width = w; height = h;
}
extern "C" JNIEXPORT void JNICALL Java_com_jev_toolkit_MainActivity_nativeTouch(JNIEnv*, jobject, jfloat x, jfloat y, jboolean pressed) {
    touchX = x; touchY = y; down = pressed;
}
extern "C" JNIEXPORT void JNICALL Java_com_jev_toolkit_MainActivity_nativeFrame(JNIEnv*, jobject) {
    glViewport(0, 0, width, height);
    glClearColor(0.08f, 0.10f, 0.14f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);
    if (!initialized) return;
    ImGuiIO& io = ImGui::GetIO();
    io.DisplaySize = ImVec2((float)width, (float)height);
    io.DeltaTime = 1.0f / 60.0f;
    io.AddMousePosEvent(touchX, touchY);
    io.AddMouseButtonEvent(0, down);
    ImGui_ImplOpenGL3_NewFrame();
    ImGui::NewFrame();
    drawJevImGuiPanel();
    ImGui::Render();
    ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());
}
