package com.phoenix.client.data.datastore;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ConfigDataStore_Factory implements Factory<ConfigDataStore> {
  private final Provider<Context> contextProvider;

  public ConfigDataStore_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ConfigDataStore get() {
    return newInstance(contextProvider.get());
  }

  public static ConfigDataStore_Factory create(Provider<Context> contextProvider) {
    return new ConfigDataStore_Factory(contextProvider);
  }

  public static ConfigDataStore newInstance(Context context) {
    return new ConfigDataStore(context);
  }
}
