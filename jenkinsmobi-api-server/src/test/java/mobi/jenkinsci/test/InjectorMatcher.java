package mobi.jenkinsci.test;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import static org.hamcrest.core.IsNull.notNullValue;

public class InjectorMatcher<T> extends BaseMatcher<Injector> {

  private final Key<T> typeKey;
  private final Matcher<?> instanceMatcher;
  private final Class<?> expectedType;
  private Description matcherDescription;

  public static <T> Matcher<? super Injector> hasInstance(Key<T> instanceType, Matcher<?> injectedInstanceMatcher) {
    return new InjectorMatcher(instanceType, injectedInstanceMatcher);
  }

  public static <T> Matcher<? super Injector> hasInstance(Class<T> instanceClass, Matcher<?> injectedInstanceMatcher) {
    return new InjectorMatcher(Key.get(instanceClass), injectedInstanceMatcher);
  }

  public static <T> Matcher<? super Injector> hasInstance(Key<T> instanceType) {
    return new InjectorMatcher(instanceType, notNullValue(instanceType.getTypeLiteral().getRawType()));
  }

  public static <T> Matcher<? super Injector> hasInstance(Class<T> instanceClass) {
    return new InjectorMatcher(Key.get(instanceClass), notNullValue(instanceClass));
  }

  public InjectorMatcher(Key<T> typeKey, Matcher<?> instanceMatcher) {
    this.typeKey = typeKey;
    this.instanceMatcher = instanceMatcher;
    this.expectedType = typeKey.getTypeLiteral().getRawType();
  }

  @Override
  final public void describeMismatch(Object item, Description description) {
    description.appendText(matcherDescription == null ? "Matching was OK": matcherDescription.toString());
  }

  @Override
  public boolean matches(Object item) {
    try {
      if (item == null) {
        return matchingError("Guice injector is null");
      }

      if (!Injector.class.isAssignableFrom(item.getClass())) {
        return matchingError("Object " + item + " is not a Guice Injector");
      }

      T instance = ((Injector) item).getInstance(typeKey);

      if (instance == null) {
        return matchingError("Instance " + typeKey + " is null");
      }

      Class<?> instanceClass = instance.getClass();
      if (!expectedType.isAssignableFrom(instanceClass)) {
        return matchingError("Instance " + typeKey + " type mismatch: expected " + expectedType + " but was " +
                "" + instanceClass);
      }


      if (!instanceMatcher.matches(instance)) {
        matcherDescription = new StringDescription();
        matcherDescription.appendText("Instance " + typeKey + " does not match " +
                "expectations: ");
        instanceMatcher.describeMismatch(item, matcherDescription);
        return false;
      } else {
        return true;
      }
    } catch (ConfigurationException | ProvisionException ex) {
      return matchingError("Cannot get instance of " + typeKey + " from Guice Injector\n" +
              ex.getMessage() + "\n\n" +
              "Error stack trace: \n" +
              "-------------------\n" +
              getStackTrace(ex));
    }
  }

  private boolean matchingError(String errorText) {
    matcherDescription = errorDescription(errorText);
    return false;
  }

  private Description errorDescription(String errorText) {
    Description description = new StringDescription();
    description.appendText(errorText);
    return description;
  }

  private String getStackTrace(Throwable ex) {
    if (ex == null) {
      return "";
    }

    StringBuilder stackTrace = new StringBuilder();
    for (StackTraceElement stack : ex.getStackTrace()) {
      stackTrace.append(stack.toString() + "\n");
    }

    Throwable cause = ex.getCause();
    if (cause != null) {
      stackTrace.append("Caused by: " + ex.toString() + "\n\n");
      stackTrace.append(getStackTrace(cause));
    }

    return stackTrace.toString();
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("Guice injected instance of " + typeKey.toString());
  }
}
