package com.turkerozturk.dtt.configuration.environment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AppConfigReader {

    private static Environment staticEnv;

    @Autowired
    public void setEnv(Environment env) {
        AppConfigReader.staticEnv = env;
    }

    public static boolean isDebugIntervalRuleEnabled() {
        return "1".equals(staticEnv.getProperty("debug.interval.rule", "0"));
    }
}
