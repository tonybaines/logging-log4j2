/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.test.appender;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.test.SomethingThatUsesLogging;

/**
 *
 */
@Plugin(name = "UsesLoggingAppender", category = "Core", elementType = "appender", printObject = true)
public final class UsesLoggingAppender extends AbstractAppender {

    private SomethingThatUsesLogging thing;

    private UsesLoggingAppender(String name, Filter filter, Layout<?> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        thing = new SomethingThatUsesLogging();
    }

    @PluginFactory
    public static UsesLoggingAppender createAppender(@PluginAttribute("name") String name,
                                            @PluginAttribute("ignoreExceptions") String ignore,
                                            @PluginElement("Layout") Layout<?> layout,
                                            @PluginElement("Filters") Filter filter) {

        boolean ignoreExceptions = Boolean.parseBoolean(ignore);

        if (name == null) {
            LOGGER.error("No name provided for MyAppender");
            return null;
        }

        return new UsesLoggingAppender(name, filter, layout, ignoreExceptions);
    }

    public void append(LogEvent event) {
        try {
            for (int i = 0; i < 50; i++) {
                Thread.sleep(10);
                thing.doSomething();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // System.out.print("Log: " + getLayout().toSerializable(event));
    }
}