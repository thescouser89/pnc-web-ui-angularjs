/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.pnc.mock.executor;

import org.jboss.pnc.common.util.RandomUtils;
import org.jboss.pnc.executor.DefaultBuildExecutionSession;
import org.jboss.pnc.spi.BuildExecutionStatus;
import org.jboss.pnc.spi.events.BuildExecutionStatusChangedEvent;
import org.jboss.pnc.spi.executor.BuildExecutionConfiguration;
import org.jboss.pnc.spi.executor.BuildExecutionSession;
import org.jboss.pnc.spi.executor.BuildExecutor;
import org.jboss.pnc.spi.executor.exceptions.ExecutorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BuildExecutorMock implements BuildExecutor {

    private Logger log = LoggerFactory.getLogger(BuildExecutorMock.class);

    private Map<Integer, BuildExecutionSession> runningExecutions = new HashMap<>();

    private ExecutorService executor = Executors.newFixedThreadPool(4);

    @Override
    public BuildExecutionSession startBuilding(
            BuildExecutionConfiguration buildExecutionConfiguration,
            Consumer<BuildExecutionStatusChangedEvent> onBuildExecutionStatusChangedEvent) throws ExecutorException {

        BuildExecutionSession buildExecutionSession = new DefaultBuildExecutionSession(buildExecutionConfiguration, onBuildExecutionStatusChangedEvent);
        buildExecutionSession.setStatus(BuildExecutionStatus.NEW);

        runningExecutions.put(buildExecutionConfiguration.getId(), buildExecutionSession);
        Consumer<BuildExecutionStatus> onCompleteInternal = (buildStatus) -> {
            log.debug("Removing buildExecutionTask [" + buildExecutionConfiguration.getId() + "] form list of running tasks.");
            runningExecutions.remove(buildExecutionConfiguration.getId());
        };

        CompletableFuture.supplyAsync(() -> mockBuild(), executor)
                .thenApplyAsync((buildTook) -> complete(onCompleteInternal), executor);
        return buildExecutionSession;
    }

    private Integer complete(Consumer<BuildExecutionStatus> onCompleteInternal) {
        onCompleteInternal.accept(BuildExecutionStatus.DONE);
        return -1;
    }

    private Integer mockBuild() {

        int sleep = RandomUtils.randInt(50, 500);
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            log.warn("Mock build interrupted.", e);
        }
        return sleep;
    }

    //    private CompletableFuture<Integer> mockBuild() {
//        CompletableFuture<Integer> waitToCompleteFuture = new CompletableFuture<>();
//        int sleep = RandomUtils.randInt(50, 500);
//        try {
//            Thread.sleep(sleep);
//        } catch (InterruptedException e) {
//            log.warn("Mock build interrupted.", e);
//        }
//        waitToCompleteFuture.complete(sleep);
//
//        return waitToCompleteFuture;
//    }
//
    @Override
    public BuildExecutionSession getRunningExecution(int buildExecutionTaskId) {
        return runningExecutions.get(buildExecutionTaskId);
    }

    @Override
    public void shutdown() {

    }
}
