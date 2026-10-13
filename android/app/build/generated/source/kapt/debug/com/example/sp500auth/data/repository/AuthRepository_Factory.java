package com.example.sp500auth.data.repository;

import com.example.sp500auth.data.api.AuthApiService;
import com.example.sp500auth.data.local.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class AuthRepository_Factory implements Factory<AuthRepository> {
  private final Provider<AuthApiService> authApiServiceProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public AuthRepository_Factory(Provider<AuthApiService> authApiServiceProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.authApiServiceProvider = authApiServiceProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public AuthRepository get() {
    return newInstance(authApiServiceProvider.get(), tokenManagerProvider.get());
  }

  public static AuthRepository_Factory create(Provider<AuthApiService> authApiServiceProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new AuthRepository_Factory(authApiServiceProvider, tokenManagerProvider);
  }

  public static AuthRepository newInstance(AuthApiService authApiService,
      TokenManager tokenManager) {
    return new AuthRepository(authApiService, tokenManager);
  }
}
