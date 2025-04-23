package com.example.stock.config;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig {

	@Bean
	RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		// Redis用のJSONシリアライザ（Jackson）を作成
		Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);

		// Jackson用のObjectMapperを構成
		ObjectMapper objectMapper = new ObjectMapper();

		// Java 8以降の日付型（LocalDateTimeなど）を扱えるようにする
		objectMapper.registerModule(new JavaTimeModule());

		// 型情報を保存して、復元時に正確な型が分かるようにする（ObjectMapperの設定）
		objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

		serializer.setObjectMapper(objectMapper);

		// Redis Cacheにおける値のシリアライザとしてJacksonを設定
		RedisSerializationContext.SerializationPair<Object> pair = RedisSerializationContext.SerializationPair
				.fromSerializer(serializer);

		// キャッシュの設定：Jacksonで値をシリアライズ
		// RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
		// 		.serializeValuesWith(pair);

		// 12時までのTTLを設定
		Duration ttl = Duration.ofSeconds(calculateTTLUntilNoon());

		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
				.serializeValuesWith(pair)
				.entryTtl(ttl);

		// RedisCacheManagerを構成（@Cacheableで使用される）
		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(config)
				.build();
	}

	// 現在時刻から12時までの残り秒数を計算
	private long calculateTTLUntilNoon() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime noon = now.toLocalDate().atTime(12, 0);
		if (now.isAfter(noon)) {
			noon = noon.plusDays(1);
		}
		return Duration.between(now, noon).getSeconds();
	}

	@Bean
	RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		// RedisTemplateのインスタンスを作成
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// Jacksonベースのシリアライザを作成
		Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
		ObjectMapper objectMapper = new ObjectMapper();

		objectMapper.activateDefaultTyping(
				LaissezFaireSubTypeValidator.instance,
				ObjectMapper.DefaultTyping.NON_FINAL);
		serializer.setObjectMapper(objectMapper);

		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(serializer);
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(serializer);

		template.afterPropertiesSet();
		return template;
	}
}