package com.book.ng.di;

import com.book.ng.data.db.NGBookDatabase;
import com.book.ng.data.db.ProgressDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AppModule_ProvideProgressDaoFactory implements Factory<ProgressDao> {
  private final Provider<NGBookDatabase> databaseProvider;

  private AppModule_ProvideProgressDaoFactory(Provider<NGBookDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ProgressDao get() {
    return provideProgressDao(databaseProvider.get());
  }

  public static AppModule_ProvideProgressDaoFactory create(
      Provider<NGBookDatabase> databaseProvider) {
    return new AppModule_ProvideProgressDaoFactory(databaseProvider);
  }

  public static ProgressDao provideProgressDao(NGBookDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideProgressDao(database));
  }
}
