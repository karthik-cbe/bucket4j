/*
 *  Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import io.github.bucket4j.AbstractDistributedBucketTest;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.RecoveryStrategy;
import io.github.bucket4j.grid.hazelcast.HazelcastBucketBuilder;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;

public class HazelcastTest extends AbstractDistributedBucketTest<HazelcastBucketBuilder, io.github.bucket4j.grid.hazelcast.Hazelcast> {

    private static IMap<String, GridBucketState> map;
    private static Cloud cloud;
    private static ViNode server;

    private static HazelcastInstance hazelcastInstance;

    @BeforeClass
    public static void setup() {
        // start separated JVM on current host
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        server = cloud.node("stateful-hazelcast-server");

        server.exec((Runnable & Serializable) () -> {
            Config config = new Config();
            config.setLiteMember(false);
            HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
            hazelcastInstance.getMap("my_buckets");
        });

        // start hazelcast client which works inside current JVM and does not hold data
        Config config = new Config();
        config.setLiteMember(true);
        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        map = hazelcastInstance.getMap("my_buckets");
    }

    @AfterClass
    public static void shutdown() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
        if (cloud != null) {
            cloud.shutdown();
        }
    }

    @Test
    @Override
    public void testThatImpossibleToPassNullCacheToProxyManagerConstructor() {
        Bucket4j.extension(getExtensionClass()).proxyManagerForMap(null);
    }

    @Override
    protected Class<io.github.bucket4j.grid.hazelcast.Hazelcast> getExtensionClass() {
        return io.github.bucket4j.grid.hazelcast.Hazelcast.class;
    }

    @Override
    protected Bucket build(HazelcastBucketBuilder builder, String key, RecoveryStrategy recoveryStrategy) {
        return builder.build(map, key, recoveryStrategy);
    }

    @Override
    protected ProxyManager<String> newProxyManager() {
        return Bucket4j.extension(getExtensionClass()).proxyManagerForMap(map);
    }

    @Override
    protected void removeBucketFromBackingStorage(String key) {
        map.remove(key);
    }

}
