package br.com.nfe.processor.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile({"dev", "test"})
public class MockDataConfig implements WebMvcConfigurer {
    
    @Value("${features.mock-data-enabled:false}")
    private boolean mockEnabled;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MockDataInterceptor());
    }
    
    private class MockDataInterceptor implements HandlerInterceptor {
        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response,
                              Object handler, org.springframework.web.servlet.ModelAndView modelAndView) {
            if (mockEnabled) {
                response.setHeader("X-BTF-Mock", "true");
            }
        }
    }
}
