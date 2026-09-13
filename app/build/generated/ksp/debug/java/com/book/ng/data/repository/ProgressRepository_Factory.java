package com.book.ng.data.repository;

import com.book.ng.data.db.ProgressDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
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
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class ProgressRepository_Factory implements Factory<ProgressRepository> {
  private final Provider<ProgressDao> progressDaoProvider;

  private ProgressRepository_Factory(Provider<ProgressDao> progressDaoProvider) {
    this.progressDaoProvider = progressDaoProvider;
  }

  @Override
  public ProgressRepository get() {
    return newInstance(progressDaoProvider.get());
  }

  public static ProgressRepository_Factory create(Provider<ProgressDao> progressDaoProvider) {
    return new ProgressRepository_Factory(progressDaoProvider);
  }

  public static ProgressRepository newInstance(ProgressDao progressDao) {
    return new ProgressRepository(progressDao);
  }
}
