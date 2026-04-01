package com.isga.translator.config;

import org.glassfish.jersey.server.ResourceConfig;

public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        packages("com.isga.translator.resource",
                 "com.isga.translator.security",
                 "com.isga.translator.config");
    }
}
