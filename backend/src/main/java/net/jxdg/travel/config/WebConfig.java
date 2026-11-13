package net.jxdg.travel.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.local-dir}")
    private String localDir;

    private final net.jxdg.travel.config.PublicTenantInterceptor publicTenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(publicTenantInterceptor)
                .addPathPatterns("/public/**", "/pay/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String abs = Paths.get(localDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/upload/**").addResourceLocations(abs.endsWith("/") ? abs : abs + "/");
    }
}
