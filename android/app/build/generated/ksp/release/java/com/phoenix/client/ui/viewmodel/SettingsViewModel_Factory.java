package com.phoenix.client.ui.viewmodel;

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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<ConfigRepository> configRepositoryProvider;

  public SettingsViewModel_Factory(Provider<ConfigRepository> configRepositoryProvider) {
    this.configRepositoryProvider = configRepositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(configRepositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<ConfigRepository> configRepositoryProvider) {
    return new SettingsViewModel_Factory(configRepositoryProvider);
  }

  public static SettingsViewModel newInstance(ConfigRepository configRepository) {
    return new SettingsViewModel(configRepository);
  }
}
