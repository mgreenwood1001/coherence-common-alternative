/*
 * File: JavaApplicationGroup.java
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

package com.oracle.coherence.common.runtime;

/**
 * An {@link JavaApplicationGroup} represents a collection of related {@link JavaApplication}s at runtime.
 * <p>
 * {@link JavaApplicationGroup}s are created using {@link JavaApplicationGroupBuilder}s.
 *
 * @param <A>  The type of the {@link JavaApplication}
 *
 * @author Brian Oliver
 */
public interface JavaApplicationGroup<A extends JavaApplication> extends ApplicationGroup<A>
{
    // this is deliberately empty
}