# ExecuTorch 1.2.0 AAR contains no consumer ProGuard rules. libexecutorch's
# JNI_OnLoad registers Module/training/LLM bindings by their Java descriptors,
# including entry points not directly referenced by the Kotlin advisory runner.
# Preserve this JNI boundary, not the app or its other dependency packages.
# https://github.com/pytorch/executorch/blob/v1.2.0/extension/android/jni/jni_layer.cpp
-keep class org.pytorch.executorch.** { *; }
-keep class com.facebook.jni.** { *; }
