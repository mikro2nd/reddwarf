/*
 * Copyright 2007-2008 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Server.
 *
 * Project Darkstar Server is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sun.sgs.impl.kernel;

import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.ManagerNotFoundException;
import com.sun.sgs.app.TaskManager;

import com.sun.sgs.kernel.ComponentRegistry;

import com.sun.sgs.service.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.MissingResourceException;


/**
 * This is the implementation of <code>KernelAppContext</code> used by
 * the kernel to manage the context of a single application. It knows
 * the name of an application, its available managers, and its backing
 * services.
 *
 * FIXME:  the context should check that it isn't shutdown before
 *  handing out services and managers
 */

// Cannot be final because tests extend it
class KernelContext {

    // the application's name and cached hash code
    private final String applicationName;
    private final int applicationCode;
    
    // the managers available in this context
    private final ComponentRegistry managerComponents;

    // the services used in this context
    private final ComponentRegistry serviceComponents;

    // the three standard managers, which are cached since they are used
    // extremely frequently
    private final ChannelManager channelManager;
    private final DataManager dataManager;
    private final TaskManager taskManager;

    /**
     * Creates an instance of <code>KernelContext</code>.
     *
     * @param applicationName the name of the application represented by
     *                        this context
     * @param serviceComponents the services available in this context
     * @param managerComponents the managers available in this context
     */
    KernelContext (String applicationName,
                   ComponentRegistry serviceComponents,
                   ComponentRegistry managerComponents) {
        this.applicationName = applicationName;

        // the hash code is the hash of the application name, which never
        // changes, so the hash code gets pre-cached here
        applicationCode = applicationName.hashCode();

        this.serviceComponents = serviceComponents;
        this.managerComponents = managerComponents;

        // pre-fetch the three standard managers...if any of them isn't
        // present then we're running without an application and with a
        // sub-set of services, so just that manager to null

        ChannelManager cm;
        try {
            cm = managerComponents.getComponent(ChannelManager.class);
        } catch (MissingResourceException mre) {
            cm = null;
        }
        channelManager = cm;

        DataManager dm;
        try {
            dm = managerComponents.getComponent(DataManager.class);
        } catch (MissingResourceException mre) {
            dm = null;
        }
        dataManager = dm;
        
        TaskManager tm;
        try {
            tm = managerComponents.getComponent(TaskManager.class);
        } catch (MissingResourceException mre) {
            tm = null;
        }
        taskManager = tm;
    }

    /**
     * Returns the <code>ChannelManager</code> used in this context.
     *
     * @return the context's <code>ChannelManager</code>
     */
    ChannelManager getChannelManager() {
        if (channelManager == null)
            throw new ManagerNotFoundException("this application is running " +
                                               "without a ChannelManager");
        return channelManager;
    }

    /**
     * Returns the <code>DataManager</code> used in this context.
     *
     * @return the context's <code>DataManager</code>
     */
    DataManager getDataManager() {
        if (dataManager == null)
            throw new ManagerNotFoundException("this application is running " +
                                               "without a DataManager");
        return dataManager;
    }

    /**
     * Returns the <code>TaskManager</code> used in this context.
     *
     * @return the context's <code>TaskManager</code>
     */
    TaskManager getTaskManager() {
        if (taskManager == null)
            throw new ManagerNotFoundException("this application is running " +
                                               "without a TaskManager");
        return taskManager;
    }

    /**
     * Returns a manager based on the given type. If the manager type is
     * unknown, or if there is more than one manager of the given type,
     * <code>ManagerNotFoundException</code> is thrown. This may be used
     * to find any available manager, including the three standard
     * managers.
     *
     * @param type the <code>Class</code> of the requested manager
     *
     * @return the requested manager
     *
     * @throws ManagerNotFoundException if there wasn't exactly one match to
     *                                  the requested type
     */
    <T> T getManager(Class<T> type) {
        try {
            return managerComponents.getComponent(type);
        } catch (MissingResourceException mre) {
            throw new ManagerNotFoundException("couldn't find manager: " +
                                               type.getName());
        }
    }

    /**
     * Returns a <code>Service</code> based on the given type. If the type is
     * unknown, or if there is more than one <code>Service</code> of the
     * given type, <code>MissingResourceException</code> is thrown. This is
     * the only way to resolve service components directly, and should be
     * used with care, as <code>Service</code>s should not be resolved and
     * invoked directly outside of a transactional context.
     *
     * @param type the <code>Class</code> of the requested <code>Service</code>
     *
     * @return the requested <code>Service</code>
     *
     * @throws MissingResourceException if there wasn't exactly one match to
     *                                  the requested type
     */
    <T extends Service> T getService(Class<T> type) {
        return serviceComponents.getComponent(type);
    }

    /**
     * Shut down all the service components in the reverse order that
     * they were added.
     */
    void shutdownServices() {
        // reverse the list of services
        ArrayList<Object> list = new ArrayList<Object>();
        for (Object service: serviceComponents) {
            list.add(service);
        }
        Collections.reverse(list);
        for (Object service: list) {
            ((Service) service).shutdown();
        }
    }
    
    /**
     * Returns a unique representation of this context, in this case the
     * name of the application.
     *
     * @return a <code>String</code> representation of the context
     */
    public String toString() {
        return applicationName;
    }

    /**
     * Returns <code>true</code> if the provided object is an instance of
     * <code>AppKernelAppContext</code> that represents the same
     * application context.
     *
     * @param o an instance of <code>KernelContext</code>
     *
     * @return <code>true</code> if the provided object represents the same
     *         context as this object, <code>false</code> otherwise
     */
    public boolean equals(Object o) {
        if ((o == null) || (! (o instanceof KernelContext)))
            return false;

        KernelContext other = (KernelContext)o;

        return other.applicationName.equals(applicationName);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return applicationCode;
    }
}
