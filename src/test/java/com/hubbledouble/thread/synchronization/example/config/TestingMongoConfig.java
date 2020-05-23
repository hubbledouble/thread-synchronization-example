/*
 *    Copyright (c) 2020, HubbleDouble
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.hubbledouble.thread.synchronization.example.config;

import com.hubbledouble.thread.synchronization.ThreadSynchronization;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.SocketUtils;

import java.io.IOException;

/**
 * Setting up an embedded Mongo DB as an example.
 *
 * @author Jorge Saldivar
 */
@Configuration
public class TestingMongoConfig {

    @Bean
    public MongoTemplate mongoTemplate() throws IOException {

        String bindIp = "localhost";
        int port = SocketUtils.findAvailableTcpPort();

        MongodStarter starter = MongodStarter.getDefaultInstance();
        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(bindIp, port, Network.localhostIsIPv6()))
                .build();

        MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
        MongodProcess mongod = mongodExecutable.start();

        MongoClient mongo = MongoClients.create("mongodb://" + bindIp + ":" + port);
        MongoTemplate mongoTemplate = new MongoTemplate(mongo, "embedded_database");
        return mongoTemplate;

    }

    /**
     * Configuring {@link ThreadSynchronization} as a bean, so we don't have to instantiate it every time.
     *
     * @param mongoOperations
     * @return
     */
    @Bean
    public ThreadSynchronization threadSynchronization(MongoOperations mongoOperations) {
        return new ThreadSynchronization(mongoOperations);
    }

}
