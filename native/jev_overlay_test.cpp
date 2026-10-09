#include <jni.h>
#include <GLES3/gl3.h>
#include "jev_overlay.h"

namespace {
int width = 1;
int height = 1;
bool initialized = false;
}

extern "C" JNIEXPORT void JNICALL
Java_com_jev_toolkit_OverlayTestActivity_nativeOverlayInit(JNIEnv*, jobject) {
    if (initialized) jev_overlay_shutdown();
    initialized = jev_overlay_init(width, height);
}

extern "C" JNIEXPORT void JNICALL
Java_com_jev_toolkit_OverlayTestActivity_nativeOverlayResize(JNIEnv*, jobject, jint w, jint h) {
    width = w;
    height = h;
    jev_overlay_resize(w, h);
}

extern "C" JNIEXPORT void JNICALL
Java_com_jev_toolkit_OverlayTestActivity_nativeOverlayTouch(JNIEnv*, jobject, jfloat x, jfloat y, jboolean down) {
    jev_overlay_touch(x, y, down == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL
Java_com_jev_toolkit_OverlayTestActivity_nativeOverlayFrame(JNIEnv*, jobject) {
    glViewport(0, 0, width, height);
    glClearColor(0.08f, 0.10f, 0.14f, 1.f);
    glClear(GL_COLOR_BUFFER_BIT);
    if (initialized) jev_overlay_frame(1.f / 60.f);
}
