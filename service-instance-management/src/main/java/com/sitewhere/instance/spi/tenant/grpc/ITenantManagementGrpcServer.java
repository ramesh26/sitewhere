/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.instance.spi.tenant.grpc;

import com.sitewhere.spi.microservice.lifecycle.ILifecycleComponent;

/**
 * Binds to a port and listens for tenant management GRPC requests.
 */
public interface ITenantManagementGrpcServer extends ILifecycleComponent {
}