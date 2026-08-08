package com.example.ecom1.config;

import org.springframework.web.client.RestClient;

/**
 * =============================================================================
 * WHAT'S "INSIDE" SPRING BOOT'S AUTO-CONFIGURATION (teaching replica)
 * =============================================================================
 * This file is NOT registered as a Spring configuration. It is a simplified
 * copy of the internal code Spring Boot uses to create the RestClient.Builder
 * bean. It exists so you can SEE the mechanism behind the magic.
 *
 * The real class lives in spring-boot-autoconfigure and is called
 * `RestClientAutoConfiguration`. When Spring Boot starts, it runs that class
 * (not this one) and registers a RestClient.Builder bean in the container.
 * Your OrderController constructor then receives that bean.
 *
 * The chain is:
 *
 *   RestClientAutoConfiguration#restClientBuilder()
 *       -> RestClient.builder()                 (factory method)
 *       -> RestClientBuilderConfigurer.configure(builder)
 *           -> loops over each RestClientBuilderCustomizer
 *               -> customizer.customize(builder)   (tweak timeouts, etc.)
 *       -> returns the finished RestClient.Builder
 *
 * The final RestClient.Builder is the bean that lands in:
 *       OrderController(RestClient.Builder builder) { ... }
 */
public class RestClientAutoConfigurationDemo {

    /**
     * Step 1 — the @Bean factory method that makes the RestClient.Builder.
     *
     * In the REAL Spring Boot code this method carries these annotations:
     *
     *   @Bean
     *   @Scope("prototype")            // a fresh builder each time it's requested
     *   @ConditionalOnMissingBean      // skip if the user already defined one
     *
     * The class itself is annotated:
     *   @AutoConfiguration
     *   @ConditionalOnClass(RestClient.class)   // only load if RestClient exists
     *
     * 1) `RestClient.builder()` is a static factory method — it returns a brand
     *    new, empty RestClient.Builder object. This is the object that will
     *    become the bean.
     * 2) The configurer takes that builder and lets every registered
     *    "customizer" tweak it (e.g. set timeouts from spring.http.client.*).
     * 3) The configured builder is handed back to Spring, which stores it as a
     *    bean named "restClientBuilder" (the method name).
     */
    public RestClient.Builder restClientBuilder(RestClientBuilderConfigurerDemo configurer) {
        RestClient.Builder builder = RestClient.builder();
        return configurer.configure(builder);
    }

    /**
     * Step 2 — the configurer. It applies every RestClientBuilderCustomizer to
     * the builder, one after another.
     *
     * Real signature: `RestClientBuilderConfigurer` (Spring Boot internal class).
     * The `@Bean` for it in Spring Boot looks like:
     *
     *   @Bean
     *   @ConditionalOnMissingBean
     *   RestClientBuilderConfigurer restClientBuilderConfigurer(
     *           RestClientBuilderCustomizer... customizers) {
     *       return new RestClientBuilderCustomizerConfigurer(customizers);
     *   }
     *
     * The customizers are collected from the container. Users (like you) can add
     * their own by declaring a @Bean of type RestClientBuilderCustomizer — and
     * Spring Boot will call it on every generated builder.
     */
    public static class RestClientBuilderConfigurerDemo {
        private final RestClientBuilderCustomizerDemo[] customizers;

        RestClientBuilderConfigurerDemo(RestClientBuilderCustomizerDemo[] customizers) {
            this.customizers = customizers;
        }

        RestClient.Builder configure(RestClient.Builder builder) {
            for (RestClientBuilderCustomizerDemo customizer : this.customizers) {
                customizer.customize(builder);
            }
            return builder;
        }
    }

    /**
     * Step 3 — the customizer contract (mirrors Spring Boot's real interface).
     * It's a @FunctionalInterface: a lambda can implement it.
     *
     *   builder -> builder.requestFactory(...)   // you could swap the HTTP engine
     *
     * This is the extension point. Spring Boot ships a few built-in customizers,
     * e.g. one that reads properties like:
     *   spring.http.client.connect-timeout=5s
     *   spring.http.client.read-timeout=5s
     * and applies them here via `builder.requestFactory(...)`.
     *
     * (In real Spring Boot this interface lives in spring-boot's
     *  org.springframework.boot.web.client package.)
     */
    @FunctionalInterface
    public interface RestClientBuilderCustomizerDemo {
        void customize(RestClient.Builder restClientBuilder);
    }
}
