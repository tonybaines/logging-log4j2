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
package org.apache.logging.log4j.core.appender;

import java.io.Serializable;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;

/**
 * Appends log events as bytes to a byte output stream. The stream encoding is defined in the layout.
 * 
 * @param <M> The kind of {@link OutputStreamManager} under management
 */
public abstract class AbstractOutputStreamAppender<M extends OutputStreamManager> extends AbstractAppender {

    private static final long serialVersionUID = 1L;

    /**
     * Immediate flush means that the underlying writer or output stream will be flushed at the end of each append
     * operation. Immediate flush is slower but ensures that each append request is actually written. If
     * <code>immediateFlush</code> is set to {@code false}, then there is a good chance that the last few logs events
     * are not actually written to persistent media if and when the application crashes.
     */
    private final boolean immediateFlush;

    private final M manager;

    /**
     * Instantiates a WriterAppender and set the output destination to a new {@link java.io.OutputStreamWriter}
     * initialized with <code>os</code> as its {@link java.io.OutputStream}.
     * 
     * @param name The name of the Appender.
     * @param layout The layout to format the message.
     * @param manager The OutputStreamManager.
     */
    protected AbstractOutputStreamAppender(final String name, final Layout<? extends Serializable> layout,
            final Filter filter, final boolean ignoreExceptions, final boolean immediateFlush, final M manager) {
        super(name, filter, layout, ignoreExceptions);
        this.manager = manager;
        this.immediateFlush = immediateFlush;
    }

    /**
     * Gets the immediate flush setting.
     * 
     * @return immediate flush.
     */
    public boolean getImmediateFlush() {
        return immediateFlush;
    }

    /**
     * Gets the manager.
     * 
     * @return the manager.
     */
    public M getManager() {
        return manager;
    }

    @Override
    public void start() {
        if (getLayout() == null) {
            LOGGER.error("No layout set for the appender named [" + getName() + "].");
        }
        if (manager == null) {
            LOGGER.error("No OutputStreamManager set for the appender named [" + getName() + "].");
        }
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        manager.release();
    }

    /**
     * Actual writing occurs here.
     * <p>
     * Most subclasses of <code>AbstractOutputStreamAppender</code> will need to override this method.
     * </p>
     * 
     * @param event The LogEvent.
     */
    @Override
    public void append(final LogEvent event) {
        try {
            final byte[] bytes = getLayout().toByteArray(event);
            if (bytes != null && bytes.length > 0) {
                manager.write(bytes, this.immediateFlush || event.isEndOfBatch());
            }
        } catch (final AppenderLoggingException ex) {
            error("Unable to write to stream " + manager.getName() + " for appender " + getName());
            throw ex;
        }
    }

}
