package net.frozenorb.apiv3.util;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.env.Environment;

import lombok.Getter;
import lombok.Setter;

public final class SpringUtils {

    @Getter @Setter private static BeanFactory beanFactory;

    public static <T> T getBean(Class<T> type) {
        return beanFactory.getBean(type);
    }

    public static String getProperty(String key) {
        return beanFactory.getBean(Environment.class).getProperty(key);
    }

}