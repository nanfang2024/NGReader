package com.book.ng;

import com.book.ng.ui.settings.JellySettingsRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<JellySettingsRepository> settingsRepositoryProvider;

  private MainActivity_MembersInjector(
      Provider<JellySettingsRepository> settingsRepositoryProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSettingsRepository(instance, settingsRepositoryProvider.get());
  }

  public static MembersInjector<MainActivity> create(
      Provider<JellySettingsRepository> settingsRepositoryProvider) {
    return new MainActivity_MembersInjector(settingsRepositoryProvider);
  }

  @InjectedFieldSignature("com.book.ng.MainActivity.settingsRepository")
  public static void injectSettingsRepository(MainActivity instance,
      JellySettingsRepository settingsRepository) {
    instance.settingsRepository = settingsRepository;
  }
}
