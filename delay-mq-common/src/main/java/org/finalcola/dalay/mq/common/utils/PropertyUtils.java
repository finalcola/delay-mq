package org.finalcola.dalay.mq.common.utils;

import org.apache.commons.codec.Resources;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.finalcola.dalay.mq.common.constants.Property;
import org.finalcola.dalay.mq.common.constants.PropertyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: finalcola
 * @date: 2023/3/29 23:22
 */
public class PropertyUtils {
    private static final Logger logger = LoggerFactory.getLogger(PropertyUtils.class);

    private static final Map<Class, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    @Nullable
    public static <T> T readPropertiesConfig(@Nonnull Class<T> klass) {
        Properties properties = readProperties(klass);
        if (properties == null) {
            return null;
        }

        try {
            T instance = klass.getDeclaredConstructor().newInstance();
            List<Field> fields = getFields(klass);
            for (Field field : fields) {
                Property property = field.getAnnotation(Property.class);
                String key = StringUtils.defaultIfBlank(property.value(), field.getName());
                String value = properties.getProperty(key, property.defaultValue());
                // TODO: 2023/3/30 对象映射
            }
            return instance;
        } catch (Exception e) {
            logger.error("read config error,config class:{}", klass.getName(), e);
            return null;
        }
    }

    @Nullable
    public static Properties readProperties(@Nonnull Class<?> klass) {
        return Optional.ofNullable(klass.getAnnotation(PropertyMapping.class))
                .map(PropertyMapping::value)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(PropertyUtils::readResourceProperties)
                .orElse(null);
    }

    @Nullable
    public static Properties readResourceProperties(@Nonnull String filePath) {
        String content = readResourceFile(filePath);
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        Properties properties = new Properties();
        String[] lines = content.split("\n");
        Arrays.stream(lines)
                .map(StringUtils::trimToEmpty)
                .map(s -> {
                    int index = s.indexOf("=");
                    if (index == -1) {
                        return null;
                    }
                    String key = StringUtils.trimToEmpty(s.substring(0, index));
                    String value = index == s.length() - 1 ? "" : StringUtils.trimToEmpty(s.substring(index + 1));
                    return Pair.of(key, value);
                })
                .filter(Objects::nonNull)
                .filter(pair -> StringUtils.isNotEmpty(pair.getKey()))
                .forEach(pair -> properties.setProperty(pair.getKey(), pair.getValue()));
        return properties;
    }

    @Nullable
    public static String readResourceFile(@Nonnull String filePath) {
        try (InputStream configFileInputStream = Resources.getInputStream(filePath);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            IOUtils.copyLarge(configFileInputStream, outputStream);
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("read properties error", e);
            return null;
        }
    }

    private static List<Field> getFields(@Nonnull Class klass) {
        return FIELD_CACHE.computeIfAbsent(klass, k -> {
            List<Field> fields = FieldUtils.getFieldsListWithAnnotation(klass, Property.class);
            fields.forEach(field -> field.setAccessible(true));
            return fields;
        });
    }
}
