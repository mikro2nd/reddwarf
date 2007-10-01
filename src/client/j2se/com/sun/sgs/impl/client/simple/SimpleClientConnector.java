/*
 * Copyright (c) 2007, Sun Microsystems, Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.sun.sgs.impl.client.simple;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Properties;

import com.sun.sgs.impl.client.comm.ClientConnectionListener;
import com.sun.sgs.impl.client.comm.ClientConnector;
import com.sun.sgs.impl.io.SocketEndpoint;
import com.sun.sgs.impl.io.TransportType;
import com.sun.sgs.impl.sharedutil.MessageBuffer;
import com.sun.sgs.impl.sharedutil.PropertiesWrapper;
import com.sun.sgs.io.Connector;

/**
 * A basic implementation of a {@code ClientConnector} which uses an 
 * {@code Connector} to establish connections. <p>
 *
 * The {@link #SimpleClientConnector constructor} supports the
 * following properties: <p>
 *
 * <ul>
 *
 * <li> <i>Key:</i> {@code host} <br>
 *	<i>No default &mdash; required</i> <br>
 *	Specifies the server host. <p>
 *
 * <li> <i>Key:</i> {@code port} <br>
 *	<i>No default &mdash; required</i> <br>
 *	Specifies the server port. <p>
 *
 * <li> <i>Key:</i> {@code connectTimeout} <br>
 *	<i>Default:</i> {@code 5000} <br>
 *	Specifies the timeout (in milliseconds) for a connect attempt
 *	to the server. <p>
 *
 * </ul> <p>
 */
class SimpleClientConnector extends ClientConnector {
    
    private final long DEFAULT_CONNECT_TIMEOUT = 5000;
    private final String DEFAULT_CONNECT_FAILURE_MESSAGE =
	"Unable to connect to server";
    
    private final Connector<SocketAddress> connector;
    private final long connectTimeout;
    private Thread connectWatchdog;
    
    SimpleClientConnector(Properties properties) {
        
        String host = properties.getProperty("host");
        if (host == null) {
            throw new IllegalArgumentException("Missing Property: host");
        }
        
        String portStr = properties.getProperty("port");
        if (portStr == null) {
            throw new IllegalArgumentException("Missing Property: port");
        }
        int port = Integer.parseInt(portStr);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Bad port number: " + port);
        }

	PropertiesWrapper wrappedProperties = new PropertiesWrapper(properties);
	connectTimeout =
	    wrappedProperties.getLongProperty(
		"connectTimeout", DEFAULT_CONNECT_TIMEOUT);
        // TODO only RELIABLE supported for now.
        TransportType transportType = TransportType.RELIABLE;

        SocketAddress socketAddress = new InetSocketAddress(host, port);
        connector = 
            new SocketEndpoint(socketAddress, transportType).createConnector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() throws IOException {
        // TODO implement
        throw new UnsupportedOperationException("Cancel not yet implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect(ClientConnectionListener connectionListener)
        throws IOException
    {
        SimpleClientConnection connection = 
            new SimpleClientConnection(connectionListener);
        
        connector.connect(connection);
	connectWatchdog = new ConnectWatchdogThread(connectionListener);
	connectWatchdog.start();
    }

    private class ConnectWatchdogThread extends Thread {

	private final ClientConnectionListener listener;

	ConnectWatchdogThread(ClientConnectionListener listener) {
	    super("ConnectWatchdogThread-" +
		  connector.getEndpoint().toString());
	    this.listener = listener;
	    setDaemon(true);
	}

	public void run() {

	    boolean connectComplete = false;
	    String connectFailureMessage = null;
	    try {
		connectComplete = connector.waitForConnect(connectTimeout);
	    } catch (IOException e) {
		connectFailureMessage = e.getMessage();
	    } catch (InterruptedException e) {
		// ignore
	    }
	    try {
		if (! connector.isConnected()) {
		    String reason =
			connectFailureMessage != null ?
			connectFailureMessage :
			DEFAULT_CONNECT_FAILURE_MESSAGE;
		    MessageBuffer buf =
			new MessageBuffer(MessageBuffer.getSize(reason));
		    buf.putString(reason);
		    listener.disconnected(false, buf.getBuffer());
		    if (! connectComplete) {
			try {
			    connector.shutdown();
			} catch (IllegalStateException e) {
			    // ignore; connect attempt may have completed
			}
		    }
		}
	    } catch (Exception e) {
		// TBD: log exception
	    }
	}
    }
}
