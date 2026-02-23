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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<ConfigRepository> configRepositoryProvider;

  public HomeViewModel_Factory(Provider<Application> applicationProvider,
      Provider<ConfigRepository> configRepositoryProvider) {
    this.applicationProvider = applicationProvider;
    this.configRepositoryProvider = configRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(applicationProvider.get(), configRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<ConfigRepository> configRepositoryProvider) {
    return new HomeViewModel_Factory(applicationProvider, configRepositoryProvider);
  }

  public static HomeViewModel newInstance(Application application,
      ConfigRepository configRepository) {
    return new HomeViewModel(application, configRepository);
  }
}
