/*
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved
 */

package com.sun.sgs.app.util;

import java.io.Serializable;
import java.nio.ByteBuffer;

import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ExceptionRetryStatus;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.Task;
import com.sun.sgs.app.TaskManager;

/**
 * Listener for messages received on a channel.  A channel can be
 * created with a {@code UtilChannelListener} which is notified when
 * any client session sends a message on that channel.  Additionally,
 * a server can specify a per-session listener (to be notified when
 * messages are sent by an individual client session on a channel)
 * when joining a client session to a channel.
 *
 * <p>An implementation of a {@code UtilChannelListener} should implement
 * the {@link Serializable} interface, so that channel listeners
 * can be stored persistently.  If a given listener has mutable state,
 * that listener should also implement the {@link ManagedObject}
 * interface.
 *
 * <p>The methods of this listener are called within the context of a
 * {@link Task} being executed by the {@link TaskManager}.  If, during
 * such an execution, a task invokes one of this listener's methods
 * and that method throws an exception, that exception implements
 * {@link ExceptionRetryStatus}, and its {@link
 * ExceptionRetryStatus#shouldRetry shouldRetry} method returns
 * {@code true}, then the {@code TaskManager} will make
 * further attempts to retry the task that invoked the listener's
 * method.  It will continue those attempts until either an attempt
 * succeeds or it notices an exception is thrown that is not
 * retryable.
 *
 * <p>For a full description of task execution behavior, see the
 * documentation for {@link TaskManager#scheduleTask(Task)}.
 */
public interface UtilChannelListener {

    /**
     * Notifies this listener that the specified message, sent on the
     * specified channel by the specified session, was received.
     *
     * @param channel a channel
     * @param sender a client session
     * @param message a message
     */
    void receivedMessage(UtilChannel channel,
                         ClientSession sender,
                         byte[] message);
}
