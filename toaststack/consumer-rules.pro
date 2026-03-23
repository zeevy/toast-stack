# ToastStack consumer ProGuard/R8 rules
# These rules are automatically applied to apps that depend on this library.

# Keep the AndroidX Startup initializer so the auto initialization system
# can find it by class name at runtime via the manifest provider.
-keep class com.siliconcircuits.toaststack.ToastStackInitializer { *; }

# Keep all public API classes and their members so consumers can call them
# by name and reflection based frameworks (like Compose) can find them.
-keep class com.siliconcircuits.toaststack.ToastStack { *; }
-keep class com.siliconcircuits.toaststack.ToastStackState { *; }
-keep class com.siliconcircuits.toaststack.ToastHandle { *; }
-keep class com.siliconcircuits.toaststack.ToastData { *; }
-keep class com.siliconcircuits.toaststack.ToastType { *; }
-keep class com.siliconcircuits.toaststack.ToastDuration { *; }
-keep class com.siliconcircuits.toaststack.ToastDuration$* { *; }
-keep class com.siliconcircuits.toaststack.ToastPosition { *; }
-keep class com.siliconcircuits.toaststack.ToastAnimation { *; }
-keep class com.siliconcircuits.toaststack.ToastAnimationConfig { *; }
-keep class com.siliconcircuits.toaststack.ToastPriority { *; }
-keep class com.siliconcircuits.toaststack.DismissReason { *; }
-keep class com.siliconcircuits.toaststack.SwipeDismissDirection { *; }
-keep class com.siliconcircuits.toaststack.ToastStackStyle { *; }
-keep class com.siliconcircuits.toaststack.ToastStackDefaults { *; }
-keep class com.siliconcircuits.toaststack.ToastStackConfig { *; }
-keep class com.siliconcircuits.toaststack.ToastBuilder { *; }

# Keep Compose related classes that Compose runtime needs at runtime.
-keep class com.siliconcircuits.toaststack.ToastStackHostKt { *; }
-keep class com.siliconcircuits.toaststack.ToastStackModifierKt { *; }
-keep class com.siliconcircuits.toaststack.ToastStackViewModelKt { *; }
-keep class com.siliconcircuits.toaststack.ToastBuilderKt { *; }
-keep class com.siliconcircuits.toaststack.ToastStackConfigKt { *; }
