package com.book.ng.data.repository;

import android.content.Context;
import com.book.ng.data.db.BookDao;
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
public final class LibraryRepository_Factory implements Factory<LibraryRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<BookDao> bookDaoProvider;

  private LibraryRepository_Factory(Provider<Context> contextProvider,
      Provider<BookDao> bookDaoProvider) {
    this.contextProvider = contextProvider;
    this.bookDaoProvider = bookDaoProvider;
  }

  @Override
  public LibraryRepository get() {
    return newInstance(contextProvider.get(), bookDaoProvider.get());
  }

  public static LibraryRepository_Factory create(Provider<Context> contextProvider,
      Provider<BookDao> bookDaoProvider) {
    return new LibraryRepository_Factory(contextProvider, bookDaoProvider);
  }

  public static LibraryRepository newInstance(Context context, BookDao bookDao) {
    return new LibraryRepository(context, bookDao);
  }
}
