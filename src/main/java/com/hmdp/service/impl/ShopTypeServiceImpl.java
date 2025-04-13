package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        // 从Redis查询缓存
        List<ShopType> shopTypes = getShopTypesFromCache();
        if (shopTypes != null) {
            return Result.ok(shopTypes);
        }

        // 从数据库查询
        shopTypes = query().orderByAsc("sort").list();
        if (shopTypes.isEmpty()) {
            // 缓存空值，避免频繁查询数据库
            cacheShopTypes(Collections.emptyList());
            return Result.fail("无法找到店铺类型！");
        }

        // 缓存查询结果
        cacheShopTypes(shopTypes);
        return Result.ok(shopTypes);
    }

    private List<ShopType> getShopTypesFromCache() {
        List<String> typeListCache = stringRedisTemplate.opsForList().range(RedisConstants.CACHE_SHOPTYPE_KEY, 0, -1);
        if (typeListCache != null && !typeListCache.isEmpty()) {
            return typeListCache.stream()
                    .map(item -> JSONUtil.toBean(item, ShopType.class))
                    .collect(Collectors.toList());
        }
        return null;
    }

    private void cacheShopTypes(List<ShopType> shopTypes) {
        List<String> collect = shopTypes.stream()
                .map(JSONUtil::toJsonStr)
                .collect(Collectors.toList());
        stringRedisTemplate.opsForList().rightPushAll(RedisConstants.CACHE_SHOPTYPE_KEY, collect);
        stringRedisTemplate.expire(RedisConstants.CACHE_SHOPTYPE_KEY, RedisConstants.CACHE_SHOPTYPE_TTL, TimeUnit.MINUTES);
    }
}
