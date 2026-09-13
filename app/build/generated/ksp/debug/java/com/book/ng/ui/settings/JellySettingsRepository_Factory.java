package com.book.ng.ui.settings;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class JellySettingsRepository_Factory implements Factory<JellySettingsRepository> {
  private final Provider<Context> contextProvider;

  private JellySettingsRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public JellySettingsRepository get() {
    return newInstance(contextProvider.get());
  }

  public static JellySettingsRepository_Factory create(Provider<Context> contextProvider) {
    return new JellySettingsRepository_Factory(contextProvider);
  }

  public static JellySettingsRepository newInstance(Context context) {
    return new JellySettingsRepository(context);
  }
}
