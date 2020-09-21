package eu.europeana.iiif.service;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Enables classes outside the Spring application context to obtain access to spring-managed bean instances.
 */
@Component
public class SpringContext implements ApplicationContextAware {
    private static ApplicationContext context;

    /**
     * Returns the Spring managed bean instance of the given class type if it exists.
     *
     * @param clazz bean class to retrieve
     * @return class instance
     */
    public static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }


    @Override
    public void setApplicationContext(@NonNull ApplicationContext context) throws BeansException {
        SpringContext.context = context;
    }
}
