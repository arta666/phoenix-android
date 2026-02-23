package com.phoenix.client.data.repository;

import com.phoenix.client.data.datastore.ConfigDataStore;
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
public final class ConfigRepositoryImpl_Factory implements Factory<ConfigRepositoryImpl> {
  private final Provider<ConfigDataStore> dataStoreProvider;

  public ConfigRepositoryImpl_Factory(Provider<ConfigDataStore> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public ConfigRepositoryImpl get() {
    return newInstance(dataStoreProvider.get());
  }

  public static ConfigRepositoryImpl_Factory create(Provider<ConfigDataStore> dataStoreProvider) {
    return new ConfigRepositoryImpl_Factory(dataStoreProvider);
  }

  public static ConfigRepositoryImpl newInstance(ConfigDataStore dataStore) {
    return new ConfigRepositoryImpl(dataStore);
  }
}
