// Copyright (C) 2013 GerritForge www.gerritforge.com
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
package mobi.jenkinsci.net;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.annotation.concurrent.ThreadSafe;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@ThreadSafe
@Slf4j
public class LazyCacheMap<K, V> {

  public interface Loader<K, V> {
    V load(final K key) throws RemoteException;
  }

  private class CachedValue {
    @Getter
    private final V value;
    @Getter
    private final long ts;

    public CachedValue(final V value, final long ts) {
      this.value = value;
      this.ts = ts;
    }
  }

  private final ConcurrentMap<K, Future<CachedValue>> cacheMap;
  private final Loader<K, V> loader;
  private final long cacheTTL;

  public LazyCacheMap(final Loader<K, V> loader, long cacheTTL) {
    this.cacheMap = new ConcurrentHashMap<K, Future<CachedValue>>();
    this.loader = loader;
    this.cacheTTL = cacheTTL;
  }

  public V get(final K key) {
    try {
      Future<CachedValue> cachedValue = cacheMap.get(key);
      if (cachedValue == null
          || (cachedValue.get().getTs() + cacheTTL) > System
              .currentTimeMillis()) {
        if (cachedValue != null) {
          cacheMap.remove(key, cachedValue);
        }
        final FutureTask<CachedValue> futureLoader = getFutureTaskLoader(key);
        cachedValue = cacheMap.putIfAbsent(key, futureLoader);
        if (cachedValue == null) {
          futureLoader.run();
          cachedValue = futureLoader;
        }
      }

      return cachedValue.get().getValue();
    } catch (final InterruptedException e) {
      throw new IllegalStateException("Fetching value for key " + key
          + " has been interrupted", e);
    } catch (final ExecutionException e) {
      log.debug("Cannot get value for key " + key);
      cacheMap.remove(key);
      return null;
    }
  }

  private FutureTask<CachedValue> getFutureTaskLoader(final K key) {
    final Callable<CachedValue> loadCall = new Callable<CachedValue>() {
      @Override
      public CachedValue call() throws Exception {
        final V value = loader.load(key);
        if (value == null) {
          log.debug("Key " + key + " cannot be loaded");
          return null;
        } else {
          log.debug("Key " + key + " has been refreshed");
          return new CachedValue(value, System.currentTimeMillis());
        }
      }
    };

    final FutureTask<CachedValue> loadTask = new FutureTask<CachedValue>(loadCall);
    return loadTask;
  }
}
