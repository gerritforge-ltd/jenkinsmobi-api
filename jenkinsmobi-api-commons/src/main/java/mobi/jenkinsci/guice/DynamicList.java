// Copyright (C) 2012 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package mobi.jenkinsci.guice;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.internal.UniqueAnnotations;
import com.google.inject.name.Named;
import com.google.inject.util.Providers;
import com.google.inject.util.Types;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A list of members that can be modified as plugins reload.
 * <p>
 * DynamicSets are always mapped as singletons in Guice. Sets store Providers
 * internally, and resolve the provider to an instance on demand. This enables
 * registrations to decide between singleton and non-singleton members.
 */
public class DynamicList<T> implements Iterable<T> {
  /**
   * Declare a singleton {@code DynamicSet<T>} with a binder.
   * <p>
   * Sets must be defined in a Guice module before they can be bound:
   *
   * <pre>
   * DynamicSet.setOf(binder(), Interface.class);
   * DynamicSet.bind(binder(), Interface.class).to(Impl.class);
   * </pre>
   *
   * @param binder a new binder created in the module.
   * @param member type of entry in the list.
   */
  public static <T> void listOf(final Binder binder, final Class<T> member) {
    listOf(binder, TypeLiteral.get(member));
  }

  /**
   * Declare a singleton {@code DynamicSet<T>} with a binder.
   * <p>
   * Sets must be defined in a Guice module before they can be bound:
   *
   * <pre>
   * DynamicSet.setOf(binder(), new TypeLiteral&lt;Thing&lt;Foo&gt;&gt;() {});
   * </pre>
   *
   * @param binder a new binder created in the module.
   * @param member type of entry in the list.
   */
  public static <T> void listOf(final Binder binder, final TypeLiteral<T> member) {
    @SuppressWarnings("unchecked")
    final Key<DynamicList<T>> key =
        (Key<DynamicList<T>>) Key.get(Types.newParameterizedType(
            DynamicList.class, member.getType()));
    binder.bind(key).toProvider(new DynamicListProvider<T>(member))
        .in(Scopes.SINGLETON);
  }

  /**
   * Bind one implementation into the list using a unique annotation.
   *
   * @param binder a new binder created in the module.
   * @param type type of entries in the list.
   * @return a binder to continue configuring the new list member.
   */
  public static <T> LinkedBindingBuilder<T> bind(final Binder binder,
      final Class<T> type) {
    return bind(binder, TypeLiteral.get(type));
  }

  /**
   * Bind one implementation into the list using a unique annotation.
   *
   * @param binder a new binder created in the module.
   * @param type type of entries in the list.
   * @return a binder to continue configuring the new list member.
   */
  public static <T> LinkedBindingBuilder<T> bind(final Binder binder,
      final TypeLiteral<T> type) {
    return binder.bind(type).annotatedWith(UniqueAnnotations.create());
  }

  /**
   * Bind a named implementation into the list.
   *
   * @param binder a new binder created in the module.
   * @param type type of entries in the list.
   * @param name {@code @Named} annotation to apply instead of a unique
   *        annotation.
   * @return a binder to continue configuring the new list member.
   */
  public static <T> LinkedBindingBuilder<T> bind(final Binder binder,
      final Class<T> type, final Named name) {
    return bind(binder, TypeLiteral.get(type));
  }

  /**
   * Bind a named implementation into the list.
   *
   * @param binder a new binder created in the module.
   * @param type type of entries in the list.
   * @param name {@code @Named} annotation to apply instead of a unique
   *        annotation.
   * @return a binder to continue configuring the new list member.
   */
  public static <T> LinkedBindingBuilder<T> bind(final Binder binder,
      final TypeLiteral<T> type, final Named name) {
    return binder.bind(type).annotatedWith(name);
  }

  public static <T> DynamicList<T> emptySet() {
    return new DynamicList<T>(
        Collections.<AtomicReference<Provider<T>>> emptySet());
  }

  private final CopyOnWriteArrayList<AtomicReference<Provider<T>>> items;

  DynamicList(final Collection<AtomicReference<Provider<T>>> base) {
    items = new CopyOnWriteArrayList<AtomicReference<Provider<T>>>(base);
  }

  @Override
  public Iterator<T> iterator() {
    final Iterator<AtomicReference<Provider<T>>> itr = items.iterator();
    return new Iterator<T>() {
      private T next;

      @Override
      public boolean hasNext() {
        while (next == null && itr.hasNext()) {
          final Provider<T> p = itr.next().get();
          if (p != null) {
            try {
              next = p.get();
            } catch (final RuntimeException e) {
              // TODO Log failed member of DynamicSet.
            }
          }
        }
        return next != null;
      }

      @Override
      public T next() {
        if (hasNext()) {
          final T result = next;
          next = null;
          return result;
        }
        throw new NoSuchElementException();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * Add one new element to the list.
   *
   * @param item the item to add to the collection. Must not be null.
   * @return handle to remove the item at a later point in time.
   */
  public RegistrationHandle add(final T item) {
    return add(Providers.of(item));
  }

  /**
   * Add one new element to the list.
   *
   * @param item the item to add to the collection. Must not be null.
   * @return handle to remove the item at a later point in time.
   */
  public RegistrationHandle add(final Provider<T> item) {
    final AtomicReference<Provider<T>> ref =
        new AtomicReference<Provider<T>>(item);
    items.add(ref);
    return new RegistrationHandle() {
      @Override
      public void remove() {
        if (ref.compareAndSet(item, null)) {
          items.remove(ref);
        }
      }
    };
  }

  /**
   * Add one new element that may be hot-replaceable in the future.
   *
   * @param key unique description from the item's Guice binding. This can be
   *        later obtained from the registration handle to facilitate matching
   *        with the new equivalent instance during a hot reload.
   * @param item the item to add to the collection right now. Must not be null.
   * @return a handle that can remove this item later, or hot-swap the item
   *         without it ever leaving the collection.
   */
  public ReloadableRegistrationHandle<T> add(final Key<T> key,
      final Provider<T> item) {
    final AtomicReference<Provider<T>> ref =
        new AtomicReference<Provider<T>>(item);
    items.add(ref);
    return new ReloadableHandle(ref, key, item);
  }

  private class ReloadableHandle implements ReloadableRegistrationHandle<T> {
    private final AtomicReference<Provider<T>> ref;
    private final Key<T> key;
    private final Provider<T> item;

    ReloadableHandle(final AtomicReference<Provider<T>> ref, final Key<T> key,
        final Provider<T> item) {
      this.ref = ref;
      this.key = key;
      this.item = item;
    }

    @Override
    public void remove() {
      if (ref.compareAndSet(item, null)) {
        items.remove(ref);
      }
    }

    @Override
    public Key<T> getKey() {
      return key;
    }

    @Override
    public ReloadableHandle replace(final Key<T> newKey,
        final Provider<T> newItem) {
      if (ref.compareAndSet(item, newItem)) {
        return new ReloadableHandle(ref, newKey, newItem);
      }
      return null;
    }
  }
}
