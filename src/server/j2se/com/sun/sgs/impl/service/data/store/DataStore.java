/*
 * Copyright 2007 Sun Microsystems, Inc.
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

package com.sun.sgs.impl.service.data.store;

import com.sun.sgs.app.NameNotBoundException;
import com.sun.sgs.app.ObjectNotFoundException;
import com.sun.sgs.app.TransactionAbortedException;
import com.sun.sgs.app.TransactionNotActiveException;
import com.sun.sgs.impl.service.data.DataServiceImpl;
import com.sun.sgs.service.Transaction;
import java.io.ObjectStreamClass;

/**
 * Defines the interface to the underlying persistence mechanism that {@link
 * DataServiceImpl} uses to store byte data. <p>
 *
 * Objects are identified by object IDs, which are positive
 * <code>long</code>s.  Names are mapped to object IDs.
 */
public interface DataStore {

    /**
     * Reserves an object ID for a new object.  Note that calling other
     * operations using this ID are not required to find the object until
     * {@link #setObject setObject} is called.  Aborting a transaction is also
     * not required to unassign the ID so long as other operations treat it as
     * a non-existent object.
     *
     * @param	txn the transaction under which the operation should take place
     * @return	the new object ID
     * @throws	TransactionAbortedException if the transaction was aborted due
     *		to a lock conflict or timeout
     * @throws	TransactionNotActiveException if the transaction is not active
     * @throws	IllegalStateException if the operation failed because of a
     *		problem with the current transaction
     */
    long createObject(Transaction txn);

    /**
     * Notifies the <code>DataStore</code> that an object is going to be
     * modified.  The implementation can use this information to obtain an
     * exclusive lock on the object in order avoid contention when the object
     * is modified.
     *
     * @param	txn the transaction under which the operation should take place
     * @param	oid the object ID
     * @throws	IllegalArgumentException if <code>oid</code> is negative
     * @throws	ObjectNotFoundException if the object is not found
     * @throws	TransactionAbortedException if the transaction was aborted due
     *		to a lock conflict or timeout
     * @throws	TransactionNotActiveException if the transaction is not active
     * @throws	IllegalStateException if the operation failed because of a
     *		problem with the current transaction
     */
    void markForUpdate(Transaction txn, long oid);

    /**
     * Obtains the data associated with an object ID.  If the
     * <code>forUpdate</code> parameter is <code>true</code>, the caller is
     * stating its intention to modify the object.  The implementation can use
     * that information to obtain an exclusive lock on the object in order
     * avoid contention when the object is modified.
     *
     * @param	txn the transaction under which the operation should take place
     * @param	oid the object ID
     * @param	forUpdate whether the caller intends to modify the object
     * @return	the data associated with the object ID
     * @throws	IllegalArgumentException if <code>oid</code> is negative
     * @throws	ObjectNotFoundException if the object is not found
     * @throws	TransactionAbortedException if the transaction was aborted due
     *		to a lock conflict or timeout
     * @throws	TransactionNotActiveException if the transaction is not active
     * @throws	IllegalStateException if the operation failed because of a
     *		problem with the current transaction
     */
    byte[] getObject(Transaction txn, long oid, boolean forUpdate);

    /**
     * Specifies data to associate with an object ID.
     *
     * @param	txn the transaction under which the operation should take place
     * @param	oid the object ID
     * @param	data the data
     * @throws	IllegalArgumentException if <code>oid</code> is negative
     * @throws	TransactionAbortedException if the transaction was aborted due
     *		to a lock conflict or timeout
     * @throws	TransactionNotActiveException if the transaction is not active
     * @throws	IllegalStateException if the operation failed because of a
     *		problem with the current transaction
     */
    void setObject(Transaction txn, long oid, byte[] data);

    /** 
     * Specifies data to associate with a series of object IDs.
     *
     * @param	txn the transaction under which the operation should take place
     * @param	oids the object IDs
     * @param	dataArray the associated data values
     * @throws	IllegalArgumentException if <code>oids</code> and
     *		<code>data</code> are not the same length, or if
     *		<code>oids</code> contains a value that is negative
     * @throws	TransactionAbortedException if the transaction was aborted due
     *		to a lock conflict or timeout
     * @throws	TransactionNotActiveException if the transaction is not active
     * @throws	IllegalStateException if the operation failed because of a
     *		problem with the current transaction
     */
    void setObjects(Transaction txn, long[] oids, byte[][] dataArray);

    /**
     * Removes the object with the specified object ID.  The implementation
     * will make an effort to flag subsequent references to the removed object
     * by throwing {@link ObjectNotFoundException}, although this behavior is
     * not guaranteed.
     *
     * @param	txn the transaction under which the operation should take place
     * @param	oid the object ID
     * @throws	IllegalArgumentException if <code>oid</code> is negative
     * @throws	ObjectNotFoundException if the object is not found
     * @throws	TransactionAbortedException if the transaction was aborted due
     *		to a lock conflict or timeout
     * @throws	TransactionNotActiveException if the transaction is not active
     * @throws	IllegalStateException if the operation failed because of a
     *		problem with the current transaction
     */
    void removeObject(Transaction txn, long oid);

    /**
     * Obtains the object ID bound to a name.
     *
     * @param	txn the transaction under which the operation should take place
     * @param	name the name
     * @return	the object ID
     * @throws	NameNotBoundException if no object ID is bound to the name
     * @throws	TransactionAbortedException if the transaction was aborted due
     *		to a lock conflict or timeout
     * @throws	TransactionNotActiveException if the transaction is not active
     * @throws	IllegalStateException if the operation failed because of a
     *		problem with the current transaction
     */
    long getBinding(Transaction txn, String name);

    /**
     * Binds an object ID to a name.
     *
     * @param	txn the transaction under which the operation should take place
     * @param	name the name
     * @param	oid the object ID
     * @throws	IllegalArgumentException if <code>oid</code> is negative
     * @throws	TransactionAbortedException if the transaction was aborted due
     *		to a lock conflict or timeout
     * @throws	TransactionNotActiveException if the transaction is not active
     * @throws	IllegalStateException if the operation failed because of a
     *		problem with the current transaction
     */
    void setBinding(Transaction txn, String name, long oid);

    /**
     * Removes the binding for a name.
     *
     * @param	txn the transaction under which the operation should take place
     * @param	name the name
     * @throws	NameNotBoundException if the name is not bound
     * @throws	TransactionAbortedException if the transaction was aborted due
     *		to a lock conflict or timeout
     * @throws	TransactionNotActiveException if the transaction is not active
     * @throws	IllegalStateException if the operation failed because of a
     *		problem with the current transaction
     */
    void removeBinding(Transaction txn, String name);

    /**
     * Returns the next name after the specified name that has a binding, or
     * <code>null</code> if there are no more bound names.  If
     * <code>name</code> is <code>null</code>, then the search starts at the
     * beginning.
     *
     * @param	txn the transaction under which the operation should take place
     * @param	name the name to search after, or <code>null</code> to start
     *		at the beginning
     * @return	the next name with a binding following <code>name</code>, or
     *		<code>null</code> if there are no more bound names
     * @throws	TransactionAbortedException if the transaction was aborted due
     *		to a lock conflict or timeout
     * @throws	TransactionNotActiveException if the transaction is not active
     * @throws	IllegalStateException if the operation failed because of a
     *		problem with the current transaction
     */
    String nextBoundName(Transaction txn, String name);

    /** 
     * Attempts to shut down this data store, returning a value that specifies
     * whether the attempt was successful. <p>
     *
     * @return	<code>true</code> if the shut down was successful, else
     *		<code>false</code>
     * @throws	IllegalStateException if the <code>shutdown</code> method has
     *		already been called and returned <code>true</code>
     */
    boolean shutdown();

    /**
     * Returns the class ID to represent classes with the specified class
     * information.  Obtains an existing ID for the class information if
     * present; otherwise, stores the information and returns the new ID
     * associated with it.  Class IDs are always greater than {@code 0}.  The
     * class information is the serialized form of the {@link
     * ObjectStreamClass} instance that serialization uses to represent the
     * class.
     *
     * @param	txn the transaction under which the operation should take place
     * @param	classInfo the class information
     * @return	the associated class ID
     * @throws	TransactionAbortedException if the transaction was aborted due
     *		to a lock conflict or timeout
     * @throws	TransactionNotActiveException if the transaction is not active
     * @throws	IllegalStateException if the operation failed because of a
     *		problem with the current transaction
     */
    int getClassId(Transaction txn, byte[] classInfo);

    /**
     * Returns the class information associated with the specified class ID.
     * The class information is the serialized form of the {@link
     * ObjectStreamClass} instance that serialization uses to represent the
     * class.
     *
     * @param	txn the transaction under which the operation should take place
     * @param	classId the class ID
     * @return	the associated class information
     * @throws	IllegalArgumentException if {@code classId} is not greater than
     *		{@code 0}
     * @throws	ClassInfoNotFoundException if the ID is not found
     * @throws	TransactionAbortedException if the transaction was aborted due
     *		to a lock conflict or timeout
     * @throws	IllegalStateException if the operation failed because of a
     *		problem with the transaction
     */
    byte[] getClassInfo(Transaction txn, int classId)
	throws ClassInfoNotFoundException;
}
