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
package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.script.Script;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;

import javax.script.SimpleBindings;

/**
 * This filter returns the onMatch result if the script returns True and returns the onMisMatch value otherwise.
 */
@Plugin(name = "ScriptFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public final class ScriptFilter extends AbstractFilter {

    private static final long serialVersionUID = 1L;

    private final Script script;
    private final Configuration configuration;

    private ScriptFilter(final Script script, final Configuration configuration, final Result onMatch,
                         final Result onMismatch) {
        super(onMatch, onMismatch);
        this.script = script;
        this.configuration = configuration;
        configuration.getScriptManager().addScript(script);
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
                         final Object... params) {
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("logger", logger);
        bindings.put("level", level);
        bindings.put("marker", marker);
        bindings.put("message", new SimpleMessage(msg));
        bindings.put("parameters", params);
        bindings.putAll(configuration.getProperties());
        bindings.put("substitutor", configuration.getStrSubstitutor());
        Object object = configuration.getScriptManager().execute(script.getName(), bindings);
        return object == null || !Boolean.TRUE.equals(object) ? onMismatch : onMatch;
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
                         final Throwable t) {
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("logger", logger);
        bindings.put("level", level);
        bindings.put("marker", marker);
        bindings.put("message", msg instanceof String ? new SimpleMessage((String)msg) : new ObjectMessage(msg));
        bindings.put("throwable", t);
        bindings.putAll(configuration.getProperties());
        bindings.put("substitutor", configuration.getStrSubstitutor());
        Object object = configuration.getScriptManager().execute(script.getName(), bindings);
        return object == null || !Boolean.TRUE.equals(object) ? onMismatch : onMatch;
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                         final Throwable t) {
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("logger", logger);
        bindings.put("level", level);
        bindings.put("marker", marker);
        bindings.put("message", msg);
        bindings.put("throwable", t);
        bindings.putAll(configuration.getProperties());
        bindings.put("substitutor", configuration.getStrSubstitutor());
        Object object = configuration.getScriptManager().execute(script.getName(), bindings);
        return object == null || !Boolean.TRUE.equals(object) ? onMismatch : onMatch;
    }

    @Override
    public Result filter(final LogEvent event) {
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("logEvent", event);
        bindings.putAll(configuration.getProperties());
        bindings.put("substitutor", configuration.getStrSubstitutor());
        Object object = configuration.getScriptManager().execute(script.getName(), bindings);
        return object == null || !Boolean.TRUE.equals(object) ? onMismatch : onMatch;
    }

    @Override
    public String toString() {
        return script.getName();
    }

    /**
     * Create the ScriptFilter.
     * @param script The script to run. The script must return a boolean value.
     * @param match The action to take if a match occurs.
     * @param mismatch The action to take if no match occurs.
     * @return A ScriptFilter.
     */
    @PluginFactory
    public static ScriptFilter createFilter(
            @PluginElement("script") final Script script,
            @PluginAttribute("onMatch") final Result match,
            @PluginAttribute("onMismatch") final Result mismatch,
            @PluginConfiguration final Configuration configuration) {

        if (script == null) {
            LOGGER.error("A script must be provided for ScriptFilter");
            return null;
        }
        return new ScriptFilter(script, configuration, match, mismatch);
    }

}