/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.repositories;

import org.elasticsearch.cluster.metadata.RepositoryMetadata;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.plugins.RepositoryPlugin;
import org.elasticsearch.repositories.fs.FsRepository;
import org.elasticsearch.snapshots.RestoreService;
import org.elasticsearch.snapshots.SnapshotShardsService;
import org.elasticsearch.snapshots.SnapshotsService;
import org.elasticsearch.threadpool.ThreadPool;

import io.crate.analyze.repositories.TypeSettings;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sets up classes for Snapshot/Restore.
 */
public class RepositoriesModule extends AbstractModule {

    private final Map<String, Repository.Factory> repositoryTypes;

    public RepositoriesModule(Environment env,
                              List<RepositoryPlugin> repoPlugins,
                              NamedXContentRegistry namedXContentRegistry,
                              ThreadPool threadPool) {
        Map<String, Repository.Factory> factories = new HashMap<>();
        factories.put(FsRepository.TYPE, new Repository.Factory() {

            @Override
            public TypeSettings settings() {
                return new TypeSettings(FsRepository.mandatorySettings(), FsRepository.optionalSettings());
            }

            @Override
            public Repository create(RepositoryMetadata metadata) throws Exception {
                return new FsRepository(metadata, env, namedXContentRegistry, threadPool);
            }
        });
        for (RepositoryPlugin repoPlugin : repoPlugins) {
            Map<String, Repository.Factory> newRepoTypes = repoPlugin.getRepositories(env, namedXContentRegistry, threadPool);
            for (Map.Entry<String, Repository.Factory> entry : newRepoTypes.entrySet()) {
                if (factories.put(entry.getKey(), entry.getValue()) != null) {
                    throw new IllegalArgumentException("Repository type [" + entry.getKey() + "] is already registered");
                }
            }
        }
        repositoryTypes = Collections.unmodifiableMap(factories);
    }

    @Override
    protected void configure() {
        bind(RepositoriesService.class).asEagerSingleton();
        bind(SnapshotsService.class).asEagerSingleton();
        bind(SnapshotShardsService.class).asEagerSingleton();
        bind(RestoreService.class).asEagerSingleton();
        MapBinder<String, Repository.Factory> typesBinder = MapBinder.newMapBinder(binder(), String.class, Repository.Factory.class);
        repositoryTypes.forEach((k, v) -> typesBinder.addBinding(k).toInstance(v));

        MapBinder<String, TypeSettings> typeSettingsBinder = MapBinder.newMapBinder(
            binder(),
            String.class,
            TypeSettings.class);
        for (var e : repositoryTypes.entrySet()) {
            String repoScheme = e.getKey();
            var repoSettings = e.getValue().settings();
            typeSettingsBinder.addBinding(repoScheme).toInstance(repoSettings);
        }
    }
}
