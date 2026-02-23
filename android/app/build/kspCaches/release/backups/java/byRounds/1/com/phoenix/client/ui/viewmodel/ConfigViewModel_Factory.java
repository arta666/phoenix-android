package com.phoenix.client.ui.viewmodel;

import android.app.Application;
import com.phoenix.client.domain.repository.ConfigRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class ConfigViewModel_Factory implements Factory<ConfigViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<ConfigRepository> configRepositoryProvider;

  public ConfigViewModel_Factory(Provider<Application> applicationProvider,
      Provider<ConfigRepository> configRepositoryProvider) {
    this.applicationProvider = applicationProvider;
    this.configRepositoryProvider = configRepositoryProvider;
  }

  @Override
  public ConfigViewModel get() {
    return newInstance(applicationProvider.get(), configRepositoryProvider.get());
  }

  public static ConfigViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<ConfigRepository> configRepositoryProvider) {
    return new ConfigViewModel_Factory(applicationProvider, configRepositoryProvider);
  }

  public static ConfigViewModel newInstance(Application application,
      ConfigRepository configRepository) {
    return new ConfigViewModel(application, configRepository);
  }
}
