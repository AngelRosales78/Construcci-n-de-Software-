package com.example.sp500auth.util.biometric;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class BiometricAuthManager_Factory implements Factory<BiometricAuthManager> {
  @Override
  public BiometricAuthManager get() {
    return newInstance();
  }

  public static BiometricAuthManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static BiometricAuthManager newInstance() {
    return new BiometricAuthManager();
  }

  private static final class InstanceHolder {
    private static final BiometricAuthManager_Factory INSTANCE = new BiometricAuthManager_Factory();
  }
}
