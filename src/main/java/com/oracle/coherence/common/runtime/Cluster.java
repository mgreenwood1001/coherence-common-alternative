/*
 * File: Cluster.java
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

import java.util.List;

/**
 * A {@link Cluster} represents an Coherence Cluster as an {@link ApplicationGroup}.
 *
 * @author Brian Oliver
 */
public class Cluster extends AbstractApplicationGroup<ClusterMember>
{
    /**
     * Constructs a {@link Cluster} given a collection of {@link ClusterMember}s.
     *
     * @param members  The collection of {@link ClusterMember}s.
     */
    Cluster(List<ClusterMember> members)
    {
        super(members);
    }

    public void waitForCluster() {
        waitForClusterSize(this.m_applications.size());
    }

    public void waitForClusterSize(int size) {
        ClusterMember clusterMember = this.m_applications.values().iterator().next();
        clusterMember.waitForClusterSize(size);
    }
}
