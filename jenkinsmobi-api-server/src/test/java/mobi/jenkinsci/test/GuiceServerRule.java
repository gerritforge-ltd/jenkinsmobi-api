package mobi.jenkinsci.test;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.servlet.ServletModule;
import lombok.extern.slf4j.Slf4j;
import mobi.jenkinsci.server.GuiceServer;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mockito.Mock;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;

@Slf4j
public class GuiceServerRule implements MethodRule {

  private final GuiceServer guiceServer;

  public GuiceServerRule(int serverPort, ServletModule servletModule) {
    guiceServer = new GuiceServer(serverPort, servletModule);
  }

  public GuiceServerRule(ServletModule servletModule) {
    guiceServer = new GuiceServer(servletModule);
  }

  @Override
  public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        guiceServer.initInjector(getMocksModule(target)).injectMembers(target);
        guiceServer.start();
        try {
          base.evaluate();
        } finally {
          guiceServer.stop();
        }
      }
    };
  }

  private Module getMocksModule(Object testInstance) {
    Class<?> cls = testInstance.getClass();
    final Map<Class, Object> mocks = Maps.newHashMap();

    for (Field field : cls.getDeclaredFields()) {
      Class type = field.getType();
      String name = field.getName();
      Annotation[] annotations = field.getDeclaredAnnotations();
      for (Annotation annotation : annotations) {
        try {
          if (isMock(field, annotation)) {
            field.setAccessible(true);
            mocks.put(field.getType(), field.get(testInstance));
          }
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException("Field " + field.getName() + " annotated with @Mock is " +
                  "not accessible", e);
        }
      }
    }

    return new AbstractModule() {
      @Override
      protected void configure() {
        for (Map.Entry<Class, Object> mockEntry : mocks.entrySet()) {
          bind(mockEntry.getKey()).toInstance(mockEntry.getValue());
        }
      }
    };
  }

  private boolean isMock(Field field, Annotation annotation) {
    return annotation.annotationType().equals(Mock.class);
  }

}
