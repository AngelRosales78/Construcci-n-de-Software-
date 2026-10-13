package com.example.sp500auth.ui.auth;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0013\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\n\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010 \u001a\u00020\u0007J\u000e\u0010!\u001a\u00020\u00072\u0006\u0010\"\u001a\u00020#J\u0006\u0010$\u001a\u00020%J\u0016\u0010&\u001a\u00020\u00072\u0006\u0010\'\u001a\u00020#2\u0006\u0010(\u001a\u00020#J\u0006\u0010)\u001a\u00020\u0007J\u0006\u0010*\u001a\u00020\u0007J6\u0010+\u001a\u00020\u00072\u0006\u0010\'\u001a\u00020#2\u0006\u0010\"\u001a\u00020#2\u0006\u0010(\u001a\u00020#2\u0006\u0010,\u001a\u00020#2\u0006\u0010-\u001a\u00020#2\u0006\u0010.\u001a\u00020#R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R7\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u00068F@BX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\r\u0010\u000e\u001a\u0004\b\t\u0010\n\"\u0004\b\u000b\u0010\fR7\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u00068F@BX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u0012\u0010\u000e\u001a\u0004\b\u0010\u0010\n\"\u0004\b\u0011\u0010\fR7\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u00068F@BX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u0016\u0010\u000e\u001a\u0004\b\u0014\u0010\n\"\u0004\b\u0015\u0010\fR7\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u00068F@BX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u001a\u0010\u000e\u001a\u0004\b\u0018\u0010\n\"\u0004\b\u0019\u0010\fR7\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001b0\u00062\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u001b0\u00068F@BX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u001f\u0010\u000e\u001a\u0004\b\u001d\u0010\n\"\u0004\b\u001e\u0010\f\u00a8\u0006/"}, d2 = {"Lcom/example/sp500auth/ui/auth/AuthViewModel;", "Landroidx/lifecycle/ViewModel;", "authRepository", "Lcom/example/sp500auth/data/repository/AuthRepository;", "(Lcom/example/sp500auth/data/repository/AuthRepository;)V", "<set-?>", "Lcom/example/sp500auth/util/Resource;", "", "forgotPasswordState", "getForgotPasswordState", "()Lcom/example/sp500auth/util/Resource;", "setForgotPasswordState", "(Lcom/example/sp500auth/util/Resource;)V", "forgotPasswordState$delegate", "Landroidx/compose/runtime/MutableState;", "loginState", "getLoginState", "setLoginState", "loginState$delegate", "logoutState", "getLogoutState", "setLogoutState", "logoutState$delegate", "registerState", "getRegisterState", "setRegisterState", "registerState$delegate", "Lcom/example/sp500auth/data/api/models/UserProfile;", "userProfileState", "getUserProfileState", "setUserProfileState", "userProfileState$delegate", "fetchUserProfile", "forgotPassword", "email", "", "isLoggedIn", "", "login", "username", "password", "loginWithBiometrics", "logout", "register", "passwordConfirm", "firstName", "lastName", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class AuthViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.example.sp500auth.data.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState loginState$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState registerState$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState forgotPasswordState$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState logoutState$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState userProfileState$delegate = null;
    
    @javax.inject.Inject()
    public AuthViewModel(@org.jetbrains.annotations.NotNull()
    com.example.sp500auth.data.repository.AuthRepository authRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sp500auth.util.Resource<kotlin.Unit> getLoginState() {
        return null;
    }
    
    private final void setLoginState(com.example.sp500auth.util.Resource<kotlin.Unit> p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sp500auth.util.Resource<kotlin.Unit> getRegisterState() {
        return null;
    }
    
    private final void setRegisterState(com.example.sp500auth.util.Resource<kotlin.Unit> p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sp500auth.util.Resource<kotlin.Unit> getForgotPasswordState() {
        return null;
    }
    
    private final void setForgotPasswordState(com.example.sp500auth.util.Resource<kotlin.Unit> p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sp500auth.util.Resource<kotlin.Unit> getLogoutState() {
        return null;
    }
    
    private final void setLogoutState(com.example.sp500auth.util.Resource<kotlin.Unit> p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sp500auth.util.Resource<com.example.sp500auth.data.api.models.UserProfile> getUserProfileState() {
        return null;
    }
    
    private final void setUserProfileState(com.example.sp500auth.util.Resource<com.example.sp500auth.data.api.models.UserProfile> p0) {
    }
    
    public final void login(@org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String password) {
    }
    
    public final void register(@org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    java.lang.String passwordConfirm, @org.jetbrains.annotations.NotNull()
    java.lang.String firstName, @org.jetbrains.annotations.NotNull()
    java.lang.String lastName) {
    }
    
    public final void forgotPassword(@org.jetbrains.annotations.NotNull()
    java.lang.String email) {
    }
    
    public final void loginWithBiometrics() {
    }
    
    public final void logout() {
    }
    
    public final void fetchUserProfile() {
    }
    
    public final boolean isLoggedIn() {
        return false;
    }
}