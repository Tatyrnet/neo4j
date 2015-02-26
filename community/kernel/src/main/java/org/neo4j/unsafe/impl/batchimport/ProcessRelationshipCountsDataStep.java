/**
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.unsafe.impl.batchimport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.neo4j.kernel.impl.api.CountsAccessor;
import org.neo4j.unsafe.impl.batchimport.cache.NodeLabelsCache;
import org.neo4j.unsafe.impl.batchimport.staging.ExecutorServiceStep;
import org.neo4j.unsafe.impl.batchimport.staging.StageControl;

/**
 * Processes relationship count data received from {@link ReadRelationshipCountsDataStep} and keeps
 * the accumulated counts per thread. Aggregated when {@link #done()}.
 */
public class ProcessRelationshipCountsDataStep extends ExecutorServiceStep<long[]>
{
    private final NodeLabelsCache cache;
    private final Map<Thread,RelationshipCountsProcessor> processors = new ConcurrentHashMap<>();
    private final int highLabelId;
    private final int highRelationshipTypeId;
    private final CountsAccessor.Updater countsUpdater;

    public ProcessRelationshipCountsDataStep( StageControl control, NodeLabelsCache cache,
            int workAheadSize, int movingAverageSize, int highLabelId, int highRelationshipTypeId,
            CountsAccessor.Updater countsUpdater )
    {
        super( control, "COUNT", workAheadSize, movingAverageSize, 1, true );
        this.cache = cache;
        this.highLabelId = highLabelId;
        this.highRelationshipTypeId = highRelationshipTypeId;
        this.countsUpdater = countsUpdater;
    }

    @Override
    protected Object process( long ticket, long[] batch )
    {
        RelationshipCountsProcessor processor = processor();
        for ( int i = 0; i < batch.length; i++ )
        {
            processor.process( batch[i++], (int)batch[i++], batch[i] );
        }
        return null; // end of line
    }

    private RelationshipCountsProcessor processor()
    {
        RelationshipCountsProcessor processor = processors.get( Thread.currentThread() );
        if ( processor == null )
        {   // This is OK since in this step implementation we use TaskExecutor which sticks to its threads.
            // deterministically.
            processors.put( Thread.currentThread(), processor = new RelationshipCountsProcessor(
                    cache, highLabelId, highRelationshipTypeId, countsUpdater ) );
        }
        return processor;
    }

    @Override
    protected void done()
    {
        for ( RelationshipCountsProcessor processor : processors.values() )
        {
            processor.done();
        }
    }
}
