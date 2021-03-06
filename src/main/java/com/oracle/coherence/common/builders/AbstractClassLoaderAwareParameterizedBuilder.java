/*
 * File: AbstractClassLoaderAwareParameterizedBuilder.java
 * 
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.
 * 
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 * 
 * This software is the confidential and proprietary information of Oracle
 * Corporation. You shall not disclose such confidential and proprietary
 * information and shall use it only in accordance with the terms of the license
 * agreement you entered into with Oracle Corporation.
 * 
 * Oracle Corporation makes no representations or warranties about the
 * suitability of the software, either express or implied, including but not
 * limited to the implied warranties of merchantability, fitness for a
 * particular purpose, or non-infringement. Oracle Corporation shall not be
 * liable for any damages suffered by licensee as a result of using, modifying
 * or distributing this software or its derivatives.
 * 
 * This notice may not be removed or altered.
 */
package com.oracle.coherence.common.builders;

import com.tangosol.io.ClassLoaderAware;

/**
 * A {@link AbstractClassLoaderAwareParameterizedBuilder} is a base implementation of a {@link ClassLoaderAware}
 * {@link ParameterizedBuilder}.
 *
 * @author Brian Oliver
 */
public abstract class AbstractClassLoaderAwareParameterizedBuilder<T> implements ParameterizedBuilder<T>, ClassLoaderAware
{

    /**
     * The {@link ClassLoader} that should be used for loading classes (defaults to the {@link ClassLoader}
     * used to load this class).
     */
    private ClassLoader contextClassLoader;

    
    /**
     * Standard Constructor.
     */
    public AbstractClassLoaderAwareParameterizedBuilder()
    {
        this.contextClassLoader = getClass().getClassLoader();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getContextClassLoader()
    {
        return contextClassLoader;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setContextClassLoader(ClassLoader contextClassLoader)
    {
       this.contextClassLoader = contextClassLoader;
    }
}
