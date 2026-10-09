#include "jev_overlay.h"
#include <GLES3/gl3.h>
#include "imgui.h"
#include "backends/imgui_impl_opengl3.h"
#include <string>
#include <vector>
#include <algorithm>
#include <cstring>
extern "C" size_t jev_inspect_loaded_assemblies(char*, size_t);
extern "C" size_t jev_explore_loaded_classes(const char*, char*, size_t);

namespace {
bool ready = false;
bool open = false;
int width = 1, height = 1;
float pointerX = -1, pointerY = -1;
bool pointerDown = false;
std::string inspection = "Press Scan to inspect loaded IL2CPP assemblies.";
char filter[128] = {};
void runScan(bool classes) {
    constexpr size_t maxBytes = 65536;
    std::vector<char> buffer(maxBytes, 0);
    const size_t needed = classes ? jev_explore_loaded_classes(filter, buffer.data(), buffer.size())
                                  : jev_inspect_loaded_assemblies(buffer.data(), buffer.size());
    inspection.assign(buffer.data());
    if (needed >= maxBytes) inspection += "\\n[Results truncated]";
}

}

extern "C" bool jev_overlay_init(int w, int h) {
    if (ready || w <= 0 || h <= 0 || ImGui::GetCurrentContext()) return false;
    width = w; height = h;
    IMGUI_CHECKVERSION();
    ImGui::CreateContext();
    ImGui::StyleColorsDark();
    ImGuiIO& io = ImGui::GetIO();
    io.DisplaySize = ImVec2((float)w, (float)h);
    io.FontGlobalScale = 1.5f;
    if (!ImGui_ImplOpenGL3_Init("#version 300 es")) {
        ImGui::DestroyContext();
        return false;
    }
    ready = true;
    return true;
}

extern "C" void jev_overlay_resize(int w, int h) {
    if (w > 0 && h > 0) { width = w; height = h; }
}
extern "C" void jev_overlay_touch(float x, float y, bool pressed) {
    pointerX = x; pointerY = y; pointerDown = pressed;
}
extern "C" void jev_overlay_frame(float delta_seconds) {
    if (!ready || ImGui::GetCurrentContext() == nullptr) return;
    ImGuiIO& io = ImGui::GetIO();
    io.DisplaySize = ImVec2((float)width, (float)height);
    io.DeltaTime = delta_seconds > 0.0001f ? delta_seconds : 1.f/60.f;
    io.AddMousePosEvent(pointerX, pointerY);
    io.AddMouseButtonEvent(0, pointerDown);
    ImGui_ImplOpenGL3_NewFrame();
    ImGui::NewFrame();
    ImGui::SetNextWindowBgAlpha(0.80f);
    ImGui::SetNextWindowPos(ImVec2(20, 60), ImGuiCond_FirstUseEver);
    ImGuiWindowFlags flags = ImGuiWindowFlags_AlwaysAutoResize | ImGuiWindowFlags_NoCollapse;
    if (ImGui::Begin("JEV##launcher", nullptr, flags)) {
        if (ImGui::Button(open ? "Close JEV" : "Open JEV")) open = !open;
    }
    ImGui::End();
    if (open) {
        ImGui::SetNextWindowSize(ImVec2(520, 450), ImGuiCond_FirstUseEver);
        if (ImGui::Begin("JEV In-Game Toolkit", &open)) {
            ImGui::TextWrapped("Embedded overlay module (read-only host integration).");
            ImGui::Separator();
            if (ImGui::Button("Scan assemblies")) runScan(false);
            ImGui::InputText("Class filter", filter, sizeof(filter));
            if (ImGui::Button("Explore classes")) runScan(true);
            ImGui::Separator();
            ImGui::BeginChild("IL2CPP results", ImVec2(0, 250), true);
            ImGui::TextUnformatted(inspection.c_str());
            ImGui::EndChild();
            ImGui::TextWrapped("Standalone APK analysis remains available in the JEV application.");
        }
        ImGui::End();
    }
    ImGui::Render();
    GLint oldProgram=0, oldVAO=0, oldFBO=0, oldViewport[4]={};
    GLboolean oldBlend=glIsEnabled(GL_BLEND), oldDepth=glIsEnabled(GL_DEPTH_TEST);
    GLboolean oldScissor=glIsEnabled(GL_SCISSOR_TEST), oldCull=glIsEnabled(GL_CULL_FACE);
    glGetIntegerv(GL_CURRENT_PROGRAM, &oldProgram);
    glGetIntegerv(GL_VERTEX_ARRAY_BINDING, &oldVAO);
    glGetIntegerv(GL_FRAMEBUFFER_BINDING, &oldFBO);
    glGetIntegerv(GL_VIEWPORT, oldViewport);
    ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());
    glUseProgram((GLuint)oldProgram);
    glBindVertexArray((GLuint)oldVAO);
    glBindFramebuffer(GL_FRAMEBUFFER, (GLuint)oldFBO);
    glViewport(oldViewport[0], oldViewport[1], oldViewport[2], oldViewport[3]);
    if (oldBlend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
    if (oldDepth) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
    if (oldScissor) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST);
    if (oldCull) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
}
extern "C" void jev_overlay_shutdown(void) {
    if (!ready) return;
    ImGui_ImplOpenGL3_Shutdown();
    ImGui::DestroyContext();
    ready = false;
    open = false;
}
