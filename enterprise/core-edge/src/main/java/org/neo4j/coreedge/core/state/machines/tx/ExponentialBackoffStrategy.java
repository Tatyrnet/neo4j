/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.coreedge.core.state.machines.tx;

import java.util.concurrent.TimeUnit;

public class ExponentialBackoffStrategy implements RetryStrategy
{
    protected final long initialBackoffTimeMillis;

    public ExponentialBackoffStrategy( long initialBackoffTime, TimeUnit timeUnit )
    {
        initialBackoffTimeMillis = timeUnit.toMillis( initialBackoffTime );
    }

    @Override
    public Timeout newTimeout()
    {
        return new Timeout()
        {
            private long backoffTimeMillis = initialBackoffTimeMillis;

            @Override
            public long getMillis()
            {
                return backoffTimeMillis;
            }

            @Override
            public void increment()
            {
                backoffTimeMillis = backoffTimeMillis * 2;
            }
        };
    }
}
