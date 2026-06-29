package org.example.ai408.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AppProperties appProperties;

    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = appProperties.cors() == null || appProperties.cors().allowedOrigins() == null
                ? List.of("*")
                : appProperties.cors().allowedOrigins();
        registry.addMapping("/**")
                .allowedOriginPatterns(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String storageDir = ensureTrailingSlash(appProperties.files().storageDir());
        String templateDir = ensureTrailingSlash(appProperties.files().templateDir());
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + storageDir)
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));
        registry.addResourceHandler("/templates/**")
                .addResourceLocations("file:" + templateDir);
    }

    private String ensureTrailingSlash(String value) {
        Path path = Path.of(value).toAbsolutePath().normalize();
        return path.toString().replace("\\", "/") + "/";
    }
}
