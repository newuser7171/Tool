// Optional Dear ImGui panel; call drawJevImGuiPanel() from your application's
// existing ImGui render loop, after ImGui::NewFrame() and before ImGui::Render().
// The host owns EGL, the graphics context, input routing and frame lifecycle.
#include "imgui.h"
#include <string>
#include <cstdio>
#include <vector>
#include <mutex>

namespace jev {
    void registerFloat(const char*, float*, float, float);
    bool setFloat(const std::string&, float);
    bool setTyped(const std::string&, const std::string&);
}

void drawJevImGuiPanel() {
    static bool open = false;
    static char field[128] = {};
    static char value[128] = {};
    static char status[128] = {};
    ImGui::SetNextWindowBgAlpha(0.92f);
    if (ImGui::Begin("JEV Launcher", nullptr, ImGuiWindowFlags_AlwaysAutoResize)) {
        if (ImGui::Button(open ? "Hide Toolkit" : "Open Toolkit")) open = !open;
    }
    ImGui::End();
    if (!open) return;
    if (ImGui::Begin("JEV Native Toolkit", &open)) {
        if (ImGui::BeginTabBar("tabs")) {
            if (ImGui::BeginTabItem("Editor")) {
                ImGui::InputText("Registered variable", field, sizeof(field));
                ImGui::InputText("Value", value, sizeof(value));
                if (ImGui::Button("Apply int / bool")) {
                    bool ok = jev::setTyped(field, value);
                    snprintf(status, sizeof(status), "%s", ok ? "Updated" : "Rejected");
                }
                ImGui::TextUnformatted(status);
                ImGui::EndTabItem();
            }
            if (ImGui::BeginTabItem("Explorer")) {
                ImGui::TextWrapped("Use JevBridge.explore() from a worker thread to browse IL2CPP metadata.");
                ImGui::EndTabItem();
            }
            if (ImGui::BeginTabItem("Logs")) {
                ImGui::TextUnformatted("adb logcat -s JEVTool:I");
                ImGui::EndTabItem();
            }
            ImGui::EndTabBar();
        }
    }
    ImGui::End();
}
