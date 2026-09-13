package com.book.ng.di;

import com.book.ng.data.db.BookDao;
import com.book.ng.data.db.NGBookDatabase;
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
public final class AppModule_ProvideBookDaoFactory implements Factory<BookDao> {
  private final Provider<NGBookDatabase> databaseProvider;

  private AppModule_ProvideBookDaoFactory(Provider<NGBookDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public BookDao get() {
    return provideBookDao(databaseProvider.get());
  }

  public static AppModule_ProvideBookDaoFactory create(Provider<NGBookDatabase> databaseProvider) {
    return new AppModule_ProvideBookDaoFactory(databaseProvider);
  }

  public static BookDao provideBookDao(NGBookDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideBookDao(database));
  }
}
