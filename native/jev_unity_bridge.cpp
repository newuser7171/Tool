#include "jev_overlay.h"
#include <cstdint>
#include <atomic>

// Unity GL.IssuePluginEvent invokes this callback on Unity's render thread.
// This is intended for Unity projects that explicitly include the plugin.
namespace {
std::atomic<int> pendingWidth{1};
std::atomic<int> pendingHeight{1};
std::atomic<float> pendingX{-1.f};
std::atomic<float> pendingY{-1.f};
std::atomic<bool> pendingDown{false};
std::atomic<bool> active{false};
}
extern "C" void jev_unity_set_screen(int width, int height) {
    if (width > 0 && height > 0) {
        pendingWidth.store(width);
        pendingHeight.store(height);
    }
}
extern "C" void jev_unity_set_touch(float x, float y, bool down) {
    pendingX.store(x);
    pendingY.store(y);
    pendingDown.store(down);
}
extern "C" void jev_unity_render_event(int eventId) {
    const int w = pendingWidth.load(), h = pendingHeight.load();
    if (eventId == 1) {
        if (!active.load()) active.store(jev_overlay_init(w, h));
    } else if (eventId == 2 && active.load()) {
        jev_overlay_resize(w, h);
        jev_overlay_touch(pendingX.load(), pendingY.load(), pendingDown.load());
        jev_overlay_frame(1.f / 60.f);
    } else if (eventId == 3 && active.exchange(false)) {
        jev_overlay_shutdown();
    }
}
extern "C" void* jev_unity_get_render_event_func() {
    return reinterpret_cast<void*>(&jev_unity_render_event);
}
