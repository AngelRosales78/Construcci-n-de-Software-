package com.example.sp500auth.data.api;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u001e\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u001e\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\f0\u0003H\u00a7@\u00a2\u0006\u0002\u0010\rJ\u001e\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0010H\u00a7@\u00a2\u0006\u0002\u0010\u0011J\u001e\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u0010\u0014J\u001e\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u000f0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u0010\u0014J\u001e\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0017H\u00a7@\u00a2\u0006\u0002\u0010\u0018J\u001e\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u001aH\u00a7@\u00a2\u0006\u0002\u0010\u001b\u00a8\u0006\u001c"}, d2 = {"Lcom/example/sp500auth/data/api/AuthApiService;", "", "changePassword", "Lretrofit2/Response;", "", "request", "Lcom/example/sp500auth/data/api/models/ChangePasswordRequest;", "(Lcom/example/sp500auth/data/api/models/ChangePasswordRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "forgotPassword", "Lcom/example/sp500auth/data/api/models/ForgotPasswordRequest;", "(Lcom/example/sp500auth/data/api/models/ForgotPasswordRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getUserProfile", "Lcom/example/sp500auth/data/api/models/UserProfile;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "login", "Lcom/example/sp500auth/data/api/models/TokenResponse;", "Lcom/example/sp500auth/data/api/models/LoginRequest;", "(Lcom/example/sp500auth/data/api/models/LoginRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "logout", "Lcom/example/sp500auth/data/api/models/RefreshRequest;", "(Lcom/example/sp500auth/data/api/models/RefreshRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "refreshAccessToken", "register", "Lcom/example/sp500auth/data/api/models/RegisterRequest;", "(Lcom/example/sp500auth/data/api/models/RegisterRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "resetPassword", "Lcom/example/sp500auth/data/api/models/ResetPasswordRequest;", "(Lcom/example/sp500auth/data/api/models/ResetPasswordRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface AuthApiService {
    
    @retrofit2.http.POST(value = "auth/register/")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object register(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sp500auth.data.api.models.RegisterRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<kotlin.Unit>> $completion);
    
    @retrofit2.http.POST(value = "auth/token/")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object login(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sp500auth.data.api.models.LoginRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.example.sp500auth.data.api.models.TokenResponse>> $completion);
    
    @retrofit2.http.POST(value = "auth/refresh/")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object refreshAccessToken(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sp500auth.data.api.models.RefreshRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.example.sp500auth.data.api.models.TokenResponse>> $completion);
    
    @retrofit2.http.POST(value = "auth/logout/")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object logout(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sp500auth.data.api.models.RefreshRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<kotlin.Unit>> $completion);
    
    @retrofit2.http.POST(value = "auth/password/reset/")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object forgotPassword(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sp500auth.data.api.models.ForgotPasswordRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<kotlin.Unit>> $completion);
    
    @retrofit2.http.POST(value = "auth/password/reset/confirm/")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object resetPassword(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sp500auth.data.api.models.ResetPasswordRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<kotlin.Unit>> $completion);
    
    @retrofit2.http.PATCH(value = "auth/change-password/")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object changePassword(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sp500auth.data.api.models.ChangePasswordRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<kotlin.Unit>> $completion);
    
    @retrofit2.http.GET(value = "auth/profile/")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getUserProfile(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.example.sp500auth.data.api.models.UserProfile>> $completion);
}