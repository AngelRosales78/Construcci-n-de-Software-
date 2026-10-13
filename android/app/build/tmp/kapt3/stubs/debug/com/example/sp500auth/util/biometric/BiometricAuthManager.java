package com.example.sp500auth.util.biometric;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J>\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\b2\u0012\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u00040\n2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00040\bJ\u000e\u0010\r\u001a\u00020\u000e2\u0006\u0010\u0005\u001a\u00020\u0006\u00a8\u0006\u000f"}, d2 = {"Lcom/example/sp500auth/util/biometric/BiometricAuthManager;", "", "()V", "authenticate", "", "context", "Landroid/content/Context;", "onSuccess", "Lkotlin/Function0;", "onError", "Lkotlin/Function1;", "", "onFailed", "isBiometricAvailable", "", "app_debug"})
public final class BiometricAuthManager {
    
    @javax.inject.Inject()
    public BiometricAuthManager() {
        super();
    }
    
    public final boolean isBiometricAvailable(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    public final void authenticate(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSuccess, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onError, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onFailed) {
    }
}