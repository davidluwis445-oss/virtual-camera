#ifndef PLT_HOOK_H
#define PLT_HOOK_H

#include <jni.h>
#include <dlfcn.h>
#include <string>

class PLTHook {
public:
    static bool hookFunction(const std::string& libraryName, 
                           const std::string& functionName, 
                           void* newFunction, 
                           void** originalFunction);
    
    static bool unhookFunction(const std::string& libraryName, 
                              const std::string& functionName);
    
    static bool isFunctionHooked(const std::string& libraryName, 
                                const std::string& functionName);

private:
    static void* getFunctionAddress(const std::string& libraryName, 
                                   const std::string& functionName);
    static bool patchFunction(void* target, void* replacement, void** original);
};

#endif // PLT_HOOK_H
