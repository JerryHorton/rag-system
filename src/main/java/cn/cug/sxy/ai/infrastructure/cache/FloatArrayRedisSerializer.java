package cn.cug.sxy.ai.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * float[] 数组的Redis序列化器。
 * 使用字节数组序列化，避免Jackson序列化float[]时的类型信息问题。
 * 
 * 注意：Spring Cache期望RedisSerializer<Object>类型，但我们可以使用
 * RedisSerializer<float[]>，Spring Cache会在需要时进行类型转换。
 * 
 * @author jerryhotton
 */
@Slf4j
public class FloatArrayRedisSerializer implements RedisSerializer<Object> {

    @Override
    public byte[] serialize(Object obj) throws SerializationException {
        if (obj == null) {
            return new byte[0];
        }
        
        // 只处理float[]类型
        if (!(obj instanceof float[])) {
            throw new SerializationException("FloatArrayRedisSerializer only supports float[] type, got: " + 
                    obj.getClass().getName());
        }
        
        float[] floats = (float[]) obj;
        
        // 使用ByteBuffer序列化：4字节长度 + float数组数据
        // 每个float占4字节
        ByteBuffer buffer = ByteBuffer.allocate(4 + floats.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(floats.length); // 写入数组长度
        for (float f : floats) {
            buffer.putFloat(f); // 写入每个float值
        }
        return buffer.array();
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        try {
            // 检查是否是我们的格式（至少4字节用于长度）
            if (bytes.length < 4) {
                log.warn("检测到旧格式缓存数据（字节数组长度过小: {}），返回null使缓存失效", bytes.length);
                return null; // 返回null使缓存失效，而不是抛出异常
            }
            
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            int length = buffer.getInt(); // 读取数组长度
            
            // 安全检查：防止异常数据（正常向量长度通常在100-2000之间）
            // 如果长度异常大，很可能是旧格式的JSON数据被误读
            if (length < 0 || length > 100000) {
                log.warn("检测到旧格式缓存数据（数组长度异常: {}），返回null使缓存失效。建议清理旧缓存：rag.cache.cleanup-on-startup=true", length);
                return null; // 返回null使缓存失效，而不是抛出异常
            }
            
            // 验证数据长度是否匹配
            int expectedBytes = 4 + length * 4;
            if (bytes.length != expectedBytes) {
                log.warn("检测到旧格式缓存数据（字节数组长度不匹配: expected {} but got {}），返回null使缓存失效", 
                        expectedBytes, bytes.length);
                return null; // 返回null使缓存失效，而不是抛出异常
            }
            
            // 额外检查：如果前几个字节看起来像JSON（以{或[开头），很可能是旧格式
            if (bytes.length > 0 && (bytes[0] == '{' || bytes[0] == '[' || 
                    (bytes.length > 1 && bytes[0] == '"' && bytes[1] == '@'))) {
                log.warn("检测到旧格式JSON缓存数据，返回null使缓存失效。建议清理旧缓存：rag.cache.cleanup-on-startup=true");
                return null; // 返回null使缓存失效
            }
            
            float[] floats = new float[length];
            for (int i = 0; i < length; i++) {
                floats[i] = buffer.getFloat(); // 读取每个float值
            }
            return floats;
        } catch (Exception e) {
            // 捕获所有异常，记录警告并返回null，使缓存失效
            log.warn("反序列化float数组失败，可能是旧格式数据。返回null使缓存失效: {}", e.getMessage());
            return null; // 返回null使缓存失效，而不是抛出异常
        }
    }
}

