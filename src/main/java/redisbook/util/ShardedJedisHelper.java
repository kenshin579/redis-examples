/******************************************************
 * author : Kris Jeong
 * published : 2013. 6. 18.
 * project name : redis-book
 *
 * Email : smufu@naver.com, smufu1@hotmail.com
 *
 * Develop JDK Ver. 1.6.0_13
 *
 * Issue list.
 *
 * 	function.
 *     1. 
 *
 ********** process *********
 *
 ********** edited **********
 *
 ******************************************************/
package redisbook.util;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.util.Hashing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <A HREF="mailto:smufu@naver.com">kris jeong</A> smufu@naver.com
 *         <p/>
 *         class desc
 */
public class ShardedJedisHelper {
    protected static final String SHARD1_HOST = "localhost";
    protected static final int SHARD1_PORT = 6381;
    protected static final String SHARD2_HOST = "localhost";
    protected static final int SHARD2_PORT = 6382;

    private final Set<ShardedJedis> connectionList = new HashSet<ShardedJedis>(); //1.샤딩된 레디스 서버에 접속된 제디스 연결을 저장함

    private ShardedJedisPool shardedPool; //2. 샤딩된 jedis pool 객체를 선언함

    /**
     * 싱글톤 처리를 위한 홀더 클래스, 제디스 연결풀이 포함된 도우미 객체를 반환한다.
     */
    private static class LazyHolder {
        @SuppressWarnings("synthetic-access")
        private static final ShardedJedisHelper INSTANCE = new ShardedJedisHelper();
    }

    /**
     * 샤딩된 제디스 연결풀 생성을 위한 도우미 클래스 내부 생성자.
     * 싱글톤 패턴이므로 외부에서 호출할 수 없다.
     */
    private ShardedJedisHelper() {
        Config config = new Config();
        config.maxActive = 20;
        config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;

        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(); //3.샤드 서버의 목록을 저장하고 서버 목록을 대상으로 샤딩을 수행함
        JedisShardInfo jedisShardInfo1 = new JedisShardInfo(SHARD1_HOST, SHARD1_PORT);
        jedisShardInfo1.setPassword("cloudoffice");
        JedisShardInfo jedisShardInfo2 = new JedisShardInfo(SHARD2_HOST, SHARD2_PORT);
        jedisShardInfo2.setPassword("cloudoffice");
        shards.add(jedisShardInfo1);
        shards.add(jedisShardInfo2);

        this.shardedPool = new ShardedJedisPool(config, shards, Hashing.MURMUR_HASH); //4.해시 메서드는 MurmurHash를 사용함
    }

    /**
     * 싱글톤 객체를 가져온다.
     *
     * @return 제디스 도우미객체
     */
    @SuppressWarnings("synthetic-access")
    public static ShardedJedisHelper getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * 제디스 클라이언트 연결을 가져온다.
     *
     * @return 제디스 객체
     */
    final public ShardedJedis getConnection() {
        ShardedJedis jedis = this.shardedPool.getResource(); //5.샤딩된 jedis connection pool에서 connection을 가져옴
        this.connectionList.add(jedis);

        return jedis;
    }

    /**
     * 사용이 완료된 제디스 객체를 회수한다.
     *
     * @param jedis 사용 완료된 제디스 객체
     */
    final public void returnResource(ShardedJedis jedis) {
        this.shardedPool.returnResource(jedis);
    }

    /**
     * 제디스 연결풀을 제거한다.
     */
    final public void destoryPool() {
        Iterator<ShardedJedis> jedisList = this.connectionList.iterator();
        while (jedisList.hasNext()) {
            ShardedJedis jedis = jedisList.next();
            this.shardedPool.returnResource(jedis);
        }

        this.shardedPool.destroy();
    }
}
