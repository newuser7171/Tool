#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <link.h>
#include <mutex>
#include <string>
#include <vector>
#include <sstream>
#include <cstring>
#include <cstdint>
#include <algorithm>
#include <cmath>
#include <cstdlib>

#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "JEVTool", __VA_ARGS__)
namespace jev {
struct Variable { std::string name; float* ptr; float min, max; };
static std::mutex mu;
static std::vector<Variable> vars;
enum class ValueKind { Int32, Boolean };
struct TypedVariable { std::string name; void* ptr; ValueKind kind; int min, max; };
static std::vector<TypedVariable> typedVars;
void registerInt(const char* name, int32_t* value, int min, int max) {
    if (!name || !value || min > max) return;
    std::lock_guard<std::mutex> lock(mu);
    typedVars.push_back({name, value, ValueKind::Int32, min, max});
}
void registerBool(const char* name, bool* value) {
    if (!name || !value) return;
    std::lock_guard<std::mutex> lock(mu);
    typedVars.push_back({name, value, ValueKind::Boolean, 0, 1});
}
bool setTyped(const std::string& name, const std::string& text) {
    std::lock_guard<std::mutex> lock(mu);
    for (auto& v : typedVars) {
        if (v.name != name) continue;
        if (v.kind == ValueKind::Boolean) {
            if (text != "true" && text != "false" && text != "1" && text != "0") return false;
            *static_cast<bool*>(v.ptr) = text == "true" || text == "1";
            return true;
        }
        char* end = nullptr;
        long n = std::strtol(text.c_str(), &end, 10);
        if (end == text.c_str() || *end != '\0' || n < v.min || n > v.max) return false;
        *static_cast<int32_t*>(v.ptr) = static_cast<int32_t>(n);
        return true;
    }
    return false;
}
static std::string lastReport;
static bool menuVisible=false;
static bool initialized=false;

// Register only fields that your own application explicitly exposes.
void registerFloat(const char* name, float* value, float min, float max) {
    if (!name || !value || min > max) return;
    std::lock_guard<std::mutex> lock(mu);
    vars.push_back({name, value, min, max});
}
bool setFloat(const std::string& name, float value) {
    std::lock_guard<std::mutex> lock(mu);
    for (auto& v : vars) {
        if (v.name == name && std::isfinite(value) && value >= v.min && value <= v.max) {
            *v.ptr=value;
            return true;
        }
    }
    return false;
}
struct Loaded { bool unity=false, il2cpp=false, mono=false, unreal=false; };
static int visit(struct dl_phdr_info* info, size_t, void* ctx) {
    if (!info || !info->dlpi_name) return 0;
    auto* loaded=static_cast<Loaded*>(ctx);
    const char* s=info->dlpi_name;
    if (strstr(s,"libunity.so")) loaded->unity=true;
    if (strstr(s,"libil2cpp.so")) loaded->il2cpp=true;
    if (strstr(s,"libmonobdwgc")) loaded->mono=true;
    if (strstr(s,"libUE4.so") || strstr(s,"libUnreal.so")) loaded->unreal=true;
    return 0;
}
static std::string inspect() {
    Loaded loaded;
    dl_iterate_phdr(visit, &loaded);
    std::ostringstream out;
    out << "Unity=" << loaded.unity << " IL2CPP=" << loaded.il2cpp
        << " Mono=" << loaded.mono << " Unreal=" << loaded.unreal << "\n";
    if (!loaded.il2cpp) return out.str()+"IL2CPP runtime not loaded.\n";
    void* lib=dlopen("libil2cpp.so", RTLD_NOW | RTLD_NOLOAD);
    if (!lib) return out.str()+"IL2CPP present, symbols unavailable.\n";
    using domain_get_t=void*(*)();
    using domain_assemblies_t=const void**(*)(const void*, size_t*);
    using assembly_image_t=const void*(*)(const void*);
    using image_name_t=const char*(*)(const void*);
    auto domain=reinterpret_cast<domain_get_t>(dlsym(lib,"il2cpp_domain_get"));
    auto assemblies=reinterpret_cast<domain_assemblies_t>(dlsym(lib,"il2cpp_domain_get_assemblies"));
    auto image=reinterpret_cast<assembly_image_t>(dlsym(lib,"il2cpp_assembly_get_image"));
    auto name=reinterpret_cast<image_name_t>(dlsym(lib,"il2cpp_image_get_name"));
    if (!domain || !assemblies || !image || !name) {
        dlclose(lib);
        return out.str()+"IL2CPP exports not available.\n";
    }
    auto* d=domain();
    if (!d) { dlclose(lib); return out.str()+"IL2CPP domain not ready.\n"; }
    size_t count=0;
    const void** list=assemblies(d,&count);
    if (count>2048) count=2048;
    out << "Assemblies: " << count << "\n";
    if (list) for(size_t i=0;i<count;++i) {
        const void* img=image(list[i]);
        const char* n=img?name(img):nullptr;
        if(n) out << " - " << n << "\n";
    }
    dlclose(lib);
    return out.str();
}

// Enumerate exported IL2CPP metadata APIs. No offsets or game-specific symbols are assumed.
static std::string explore(const std::string& filter) {
    Loaded loaded;
    dl_iterate_phdr(visit, &loaded);
    if (!loaded.il2cpp) return "IL2CPP runtime is not loaded.";
    void* lib = dlopen("libil2cpp.so", RTLD_NOW | RTLD_NOLOAD);
    if (!lib) return "IL2CPP is loaded but not accessible through dlopen.";
    using ptr0 = void*(*)();
    using assemblies_t = const void**(*)(const void*, size_t*);
    using image_t = const void*(*)(const void*);
    using name_t = const char*(*)(const void*);
    using count_t = size_t(*)(const void*);
    using class_t = void*(*)(const void*, size_t);
    using iterator_t = const void*(*)(void*, void**);
    using attach_t = void*(*)(void*);
    using detach_t = void(*)(void*);
    using field_type_t = const void*(*)(const void*);
    using type_name_t = char*(*)(const void*);
    using free_t = void(*)(void*);
    using method_params_t = uint32_t(*)(const void*);
    auto domain = reinterpret_cast<ptr0>(dlsym(lib,"il2cpp_domain_get"));
    auto assemblies = reinterpret_cast<assemblies_t>(dlsym(lib,"il2cpp_domain_get_assemblies"));
    auto image = reinterpret_cast<image_t>(dlsym(lib,"il2cpp_assembly_get_image"));
    auto imageName = reinterpret_cast<name_t>(dlsym(lib,"il2cpp_image_get_name"));
    auto classCount = reinterpret_cast<count_t>(dlsym(lib,"il2cpp_image_get_class_count"));
    auto imageClass = reinterpret_cast<class_t>(dlsym(lib,"il2cpp_image_get_class"));
    auto className = reinterpret_cast<name_t>(dlsym(lib,"il2cpp_class_get_name"));
    auto classNamespace = reinterpret_cast<name_t>(dlsym(lib,"il2cpp_class_get_namespace"));
    auto methods = reinterpret_cast<iterator_t>(dlsym(lib,"il2cpp_class_get_methods"));
    auto fields = reinterpret_cast<iterator_t>(dlsym(lib,"il2cpp_class_get_fields"));
    auto methodName = reinterpret_cast<name_t>(dlsym(lib,"il2cpp_method_get_name"));
    auto fieldName = reinterpret_cast<name_t>(dlsym(lib,"il2cpp_field_get_name"));
    auto attach = reinterpret_cast<attach_t>(dlsym(lib,"il2cpp_thread_attach"));
    auto detach = reinterpret_cast<detach_t>(dlsym(lib,"il2cpp_thread_detach"));
    auto fieldType = reinterpret_cast<field_type_t>(dlsym(lib,"il2cpp_field_get_type"));
    auto typeName = reinterpret_cast<type_name_t>(dlsym(lib,"il2cpp_type_get_name"));
    auto il2cppFree = reinterpret_cast<free_t>(dlsym(lib,"il2cpp_free"));
    auto paramCount = reinterpret_cast<method_params_t>(dlsym(lib,"il2cpp_method_get_param_count"));
    if (!domain || !assemblies || !image || !imageName || !classCount ||
        !imageClass || !className || !classNamespace || !methods || !fields ||
        !methodName || !fieldName || !attach || !detach) {
        dlclose(lib);
        return "Required IL2CPP exports are unavailable; this Unity version is unsupported.";
    }
    void* d = domain();
    if (!d) { dlclose(lib); return "IL2CPP domain not initialized."; }
    void* thread = attach(d);
    if (!thread) { dlclose(lib); return "Could not attach to IL2CPP thread."; }
    std::ostringstream out;
    size_t n = 0, shown = 0;
    const void** list = assemblies(d, &n);
    if (list) for (size_t i=0; i<std::min(n,size_t(512)) && shown<80; ++i) {
        const void* img = image(list[i]);
        if (!img) continue;
        const char* imgName = imageName(img);
        size_t classes = std::min(classCount(img),size_t(20000));
        for (size_t c=0; c<classes && shown<80; ++c) {
            void* klass = imageClass(img,c);
            if (!klass) continue;
            const char* name = className(klass);
            const char* ns = classNamespace(klass);
            if (!name) continue;
            std::string qualified = std::string(ns ? ns : "") + "." + name;
            if (!filter.empty() && qualified.find(filter)==std::string::npos &&
                (!imgName || std::string(imgName).find(filter)==std::string::npos)) continue;
            out << "[" << (imgName ? imgName : "?") << "] " << qualified << "\n";
            void* iter = nullptr;
            for (int j=0;j<20;++j) {
                const void* method = methods(klass,&iter);
                if (!method) break;
                const char* mn = methodName(method);
                if (mn) {
                    out << "  method: " << mn;
                    if (paramCount) out << " (arguments: " << paramCount(method) << ")";
                    out << "\n";
                }
            }
            iter = nullptr;
            for (int j=0;j<20;++j) {
                const void* field = fields(klass,&iter);
                if (!field) break;
                const char* fn = fieldName(field);
                if (fn) {
                    out << "  field: " << fn;
                    if (fieldType && typeName && il2cppFree) {
                        const void* type = fieldType(field);
                        char* typeText = type ? typeName(type) : nullptr;
                        if (typeText) {
                            out << " : " << typeText;
                            il2cppFree(typeText);
                        }
                    }
                    out << "\n";
                }
            }
            ++shown;
            if (out.tellp()>30000) break;
        }
    }
    detach(thread);
    dlclose(lib);
    if (!shown) return "No matching classes found (or metadata is unavailable).";
    out << "Displayed " << shown << " classes (limit 80).";
    return out.str();
}

}

extern "C" JNIEXPORT jstring JNICALL
Java_com_jev_toolkit_JevBridge_inspect(JNIEnv* env, jclass) {
    auto result=jev::inspect();
    LOG("%s", result.c_str());
    return env->NewStringUTF(result.c_str());
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_jev_toolkit_JevBridge_setFloat(JNIEnv* env, jclass, jstring key, jfloat value) {
    if(!key) return JNI_FALSE;
    const char* chars=env->GetStringUTFChars(key,nullptr);
    if(!chars) return JNI_FALSE;
    bool ok=jev::setFloat(chars,value);
    env->ReleaseStringUTFChars(key,chars);
    return ok?JNI_TRUE:JNI_FALSE;
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_jev_toolkit_JevBridge_variables(JNIEnv* env, jclass) {
    std::ostringstream out;
    std::lock_guard<std::mutex> lock(jev::mu);
    for(const auto& v:jev::vars) out << "float " << v.name << "=" << *v.ptr << " [" << v.min << "," << v.max << "]\n";
    for(const auto& v:jev::typedVars) {
        if (v.kind == jev::ValueKind::Boolean) out << "bool " << v.name << "=" << (*static_cast<bool*>(v.ptr) ? "true" : "false") << "\n";
        else out << "int " << v.name << "=" << *static_cast<int32_t*>(v.ptr) << " [" << v.min << "," << v.max << "]\n";
    }
    return env->NewStringUTF(out.str().c_str());
}
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM*, void*) {
    LOG("JEV Toolkit loaded");
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_jev_toolkit_JevBridge_explore(JNIEnv* env, jclass, jstring filter) {
    std::string text;
    if (filter) {
        const char* chars = env->GetStringUTFChars(filter, nullptr);
        if (chars) { text = chars; env->ReleaseStringUTFChars(filter, chars); }
    }
    auto result = jev::explore(text);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_jev_toolkit_JevBridge_setTyped(JNIEnv* env, jclass, jstring key, jstring value) {
    if (!key || !value) return JNI_FALSE;
    const char* k = env->GetStringUTFChars(key, nullptr);
    if (!k) return JNI_FALSE;
    const char* v = env->GetStringUTFChars(value, nullptr);
    if (!v) { env->ReleaseStringUTFChars(key, k); return JNI_FALSE; }
    bool ok = jev::setTyped(k, v);
    env->ReleaseStringUTFChars(value, v);
    env->ReleaseStringUTFChars(key, k);
    return ok ? JNI_TRUE : JNI_FALSE;
}
