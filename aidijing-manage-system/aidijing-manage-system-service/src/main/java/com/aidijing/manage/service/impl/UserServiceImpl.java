package com.aidijing.manage.service.impl;

import com.aidijing.common.util.AssertUtils;
import com.aidijing.common.util.LogUtils;
import com.aidijing.manage.bean.domain.User;
import com.aidijing.manage.mapper.UserMapper;
import com.aidijing.manage.service.UserService;
import com.baomidou.mybatisplus.mapper.Condition;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 用户服务实现类
 * <p>
 * 这里 {@code @CacheConfig 的 cacheNames = "userService.user"},表示该类下面所有方法在使用Spring Cache注解后
 * 默认的cacheNames,如果不指定则使用@CacheConfig所指定的cacheNames,如果指定了,则使用指定的.
 *
 * @author 披荆斩棘
 * @since 2017-05-11
 */
@CacheConfig( cacheNames = "userService.user.namespace" )
@Service
public class UserServiceImpl extends ServiceImpl< UserMapper, User > implements UserService {


	/**
	 * 用户分页在缓存中存储名称key的前缀
	 */
	private static final String CACHE_USER_LIST_PAGE_NAME_PREFIX = "UserService.User.namespace";
	@Autowired
	private RedissonClient redissonClient;

	@Cacheable( key = "T(java.lang.String).valueOf(#id)" )
	@Override
	public User get ( Long id ) {
		return super.selectById( id );
	}

	@Caching(
		evict = {
			@CacheEvict( value = CACHE_USER_LIST_PAGE_NAME_PREFIX, allEntries = true, condition = "#result != null" )
		},
		put = {
			@CachePut( key = "T(java.lang.String).valueOf(#user.id)", condition = "#result != null" )
		}
	)
	@Override
	public User update ( User user ) {
		if ( ! super.updateById( user ) ) {
			return null;
		}
		return super.selectById( user.getId() );
	}

	@Caching(
		evict = {
			@CacheEvict( value = CACHE_USER_LIST_PAGE_NAME_PREFIX, allEntries = true, condition = "#result != null" )
		},
		put = {
			@CachePut( key = "T(java.lang.String).valueOf(#user.id)", condition = "#result != null" )
		}
	)
	@Override
	public boolean save ( User user ) {
		return super.insert( user );
	}

	@Caching( evict = {
		@CacheEvict( value = CACHE_USER_LIST_PAGE_NAME_PREFIX, allEntries = true, condition = "#result != null" ) ,
		@CacheEvict( key = "T(java.lang.String).valueOf(#id)" )
	} )
	@Override
	public boolean delete ( Long id ) {
		return super.deleteById( id );
	}

	@Override
	public User findByUsername ( String username ) {
		return this.selectOne( Condition.create().eq( "username" , username ) );
	}

	@Override
	public boolean isExist ( Long userId ) {
		return super.selectCount( Condition.create().eq( "id" , userId ) ) > 0;
	}

	@Override
	public boolean isNotExist ( Long userId ) {
		return ! this.isExist( userId );
	}

	@Async
	@Override
	public Future< Boolean > pay () {
		// 获取分布式锁
		final Lock lock = redissonClient.getLock( "buyLock" );
		
		try {
			// 30秒没有获取到锁
			if ( ! lock.tryLock( 30 , TimeUnit.SECONDS ) ) {
				AssertUtils.isTrue( true , "支付失败,请稍后再试" );
			}
		} catch ( InterruptedException e ) {
			AssertUtils.isTrue( true , "支付失败,请稍后再试" );
		}

		try {
			// 支付处理
			// ... ...
			for ( int i = 0 ; i < 3 ; i++ ) {
				TimeUnit.SECONDS.sleep( 1 );
				LogUtils.getLogger().warn( "线程id : {},支付处理中... " , Thread.currentThread().getId() );
			}
		} catch ( InterruptedException e ) {
			AssertUtils.isTrue( true , "支付失败,请稍后再试" );
		} finally {
			// 解锁
			lock.unlock();
		}
		return new AsyncResult<>( true );
	}


}
