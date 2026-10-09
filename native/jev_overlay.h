#pragma once
#include <stdbool.h>
#ifdef __cplusplus
extern "C" {
#endif
// Call only on the host application's active OpenGL ES 3 rendering thread.
// The host must provide touch coordinates in framebuffer pixels.
bool jev_overlay_init(int framebuffer_width, int framebuffer_height);
void jev_overlay_resize(int framebuffer_width, int framebuffer_height);
void jev_overlay_touch(float x, float y, bool pressed);
void jev_overlay_frame(float delta_seconds);
void jev_overlay_shutdown(void);
#ifdef __cplusplus
}
#endif
