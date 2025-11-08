package com.bcbs239.regtech.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;

@Configuration
public class RouterFunctionDebugConfig {
    private static final Logger logger = LoggerFactory.getLogger(RouterFunctionDebugConfig.class);

    @Bean
    public static  BeanPostProcessor routerFunctionLogger() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof RouterFunction) {
                    logger.info("✅ RouterFunction bean registered: {} of type {}",
                            beanName, bean.getClass().getSimpleName());
                }
                return bean;
            }
        };
    }
}
