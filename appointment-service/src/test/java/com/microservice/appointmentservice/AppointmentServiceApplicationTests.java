package com.microservice.appointmentservice;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.microservice.appointmentservice.entity.OrderInfo;
import com.microservice.appointmentservice.mapper.OrderInfoMapper;
import com.microservice.appointmentservice.pojo.CancelRedis;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
class AppointmentServiceApplicationTests {
    @Autowired
    OrderInfoMapper orderInfoMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String CANCEL_COUNT_KEY_PREFIX = "cancelCount:";
    private static final Integer EXPIRE_TIME_SECONDS = 24 * 60 * 60; // 24 小时
    @Test
    void getCancelCount() {
        Integer orderId = 14;
        // 查询对应order的用户
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        String patientId = orderInfo.getPatientId();
        String currentDate = LocalDate.now().toString();

        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();

        System.out.println(hashOperations.get(CANCEL_COUNT_KEY_PREFIX + patientId , currentDate));

    }
    @Test
    void patientCancel() {
        Integer orderId = 15;
        // 查询对应order的用户
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        String patientId = orderInfo.getPatientId();
        String currentDate = LocalDate.now().toString();

        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();

        // 检查用户今天的取消预约次数
        String cancelCountStr = hashOperations.get(CANCEL_COUNT_KEY_PREFIX + patientId ,currentDate);
        Long cancelCount = cancelCountStr != null ? Long.parseLong(cancelCountStr) : null;
        System.out.println(hashOperations.get(CANCEL_COUNT_KEY_PREFIX + patientId , currentDate));
        if (cancelCount == null) {
            // 如果用户今天还没有取消预约，则设置取消预约次数为 1，并设置过期时间
            hashOperations.put(CANCEL_COUNT_KEY_PREFIX + patientId , currentDate, "1");
        } else if (cancelCount < 3) {
            // 如果取消预约次数小于 3，则增加取消预约次数
            hashOperations.increment(CANCEL_COUNT_KEY_PREFIX + patientId ,currentDate, 1L);
        } else {
            // 调用审核微服务
            // 如果审核通过就跳出if，失败直接return
            System.out.println(hashOperations.get(CANCEL_COUNT_KEY_PREFIX + patientId , currentDate));
            return;
        }
        System.out.println(hashOperations.get(CANCEL_COUNT_KEY_PREFIX + patientId , currentDate));
        orderInfoMapper.deleteById(orderId);
    }


}
