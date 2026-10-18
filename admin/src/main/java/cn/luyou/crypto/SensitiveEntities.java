package cn.luyou.crypto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按实体上的 {@link TableName} 加密或解密已登记字段，并在写入后把内存对象恢复成明文。
 */
public final class SensitiveEntities {

    private static final Map<Class<?>, List<Binding>> BINDINGS = new ConcurrentHashMap<>();

    private SensitiveEntities() {
    }

    public static List<Runnable> encryptParameter(Object parameter, FieldEncryptor encryptor) {
        List<Runnable> restores = new ArrayList<>();
        walk(parameter, encryptor, true, restores, new IdentityHashMap<>());
        return restores;
    }

    public static void decryptResult(Object result, FieldEncryptor encryptor) {
        walk(result, encryptor, false, new ArrayList<>(), new IdentityHashMap<>());
    }

    private static void walk(Object value, FieldEncryptor encryptor, boolean encrypt,
                             List<Runnable> restores, IdentityHashMap<Object, Boolean> seen) {
        if (value == null || seen.containsKey(value)) {
            return;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof Enum<?> || value.getClass().isPrimitive()) {
            return;
        }
        seen.put(value, Boolean.TRUE);
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object item = entry.getValue();
                if (item instanceof String text && entry.getKey() instanceof String key) {
                    String updated = transformMappedValue(key, text, encryptor, encrypt);
                    if (!text.equals(updated)) {
                        @SuppressWarnings("unchecked")
                        Map<Object, Object> mutable = (Map<Object, Object>) map;
                        mutable.put(entry.getKey(), updated);
                    }
                    continue;
                }
                walk(item, encryptor, encrypt, restores, seen);
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                walk(item, encryptor, encrypt, restores, seen);
            }
            return;
        }
        if (value.getClass().isArray() && !value.getClass().getComponentType().isPrimitive()) {
            for (Object item : (Object[]) value) {
                walk(item, encryptor, encrypt, restores, seen);
            }
            return;
        }
        TableName tableName = value.getClass().getAnnotation(TableName.class);
        if (tableName == null) {
            return;
        }
        for (Binding binding : bindings(value.getClass(), tableName.value())) {
            try {
                String current = (String) binding.field.get(value);
                if (binding.column.kind() == SensitiveDataRegistry.Kind.ID
                        || binding.column.kind() == SensitiveDataRegistry.Kind.PHONE) {
                    applyFormatFlag(value, binding, current);
                }
                String updated = binding.column.kind() == SensitiveDataRegistry.Kind.JSON
                        ? (encrypt ? encryptor.encryptJson(current) : encryptor.decryptJson(current))
                        : (encrypt ? encryptor.encrypt(current) : encryptor.decrypt(current));
                if (current == null ? updated != null : !current.equals(updated)) {
                    binding.field.set(value, updated);
                    if (encrypt) {
                        String backup = current;
                        restores.add(() -> setQuietly(binding.field, value, backup));
                    }
                }
            } catch (IllegalAccessException ignored) {
                // 字段不可访问时跳过，避免影响主流程
            }
        }
    }

    private static void applyFormatFlag(Object bean, Binding binding, String plain) {
        if (binding.flagField == null) {
            return;
        }
        String source = plain;
        if (source != null && source.startsWith(FieldEncryptor.PREFIX)) {
            return;
        }
        Integer flag = binding.column.kind() == SensitiveDataRegistry.Kind.PHONE
                ? FieldEncryptor.phoneFlag(source)
                : FieldEncryptor.idNumberFlag(source);
        setQuietly(binding.flagField, bean, flag);
    }

    private static String transformMappedValue(String key, String text, FieldEncryptor encryptor, boolean encrypt) {
        boolean json = "epidemicData".equals(key) || "epidemic_data".equals(key)
                || "rawData".equals(key) || "raw_data".equals(key);
        if (json) {
            return encrypt ? encryptor.encryptJson(text) : encryptor.decryptJson(text);
        }
        if (!encrypt && encryptor.isEncrypted(text)) {
            return encryptor.decrypt(text);
        }
        return text;
    }

    private static List<Binding> bindings(Class<?> type, String table) {
        return BINDINGS.computeIfAbsent(type, key -> {
            List<Binding> list = new ArrayList<>();
            for (SensitiveDataRegistry.Column column : SensitiveDataRegistry.columnsOf(table)) {
                Field field = findField(type, column.property());
                if (field == null || field.getAnnotation(TableField.class) != null
                        && !field.getAnnotation(TableField.class).exist()) {
                    continue;
                }
                if (!String.class.equals(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Field flagField = null;
                if (column.kind() == SensitiveDataRegistry.Kind.ID || column.kind() == SensitiveDataRegistry.Kind.PHONE) {
                    if ("idNumber".equals(column.property()) || "phone".equals(column.property())) {
                        flagField = findField(type, column.property() + "Fmt");
                        if (flagField != null) {
                            flagField.setAccessible(true);
                        }
                    }
                }
                list.add(new Binding(column, field, flagField));
            }
            return list;
        });
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                if (!Modifier.isStatic(field.getModifiers())) {
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
                // 继续向父类查找
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static void setQuietly(Field field, Object bean, Object value) {
        try {
            field.set(bean, value);
        } catch (IllegalAccessException ignored) {
            // 恢复明文失败不影响已完成的数据库写入
        }
    }

    private record Binding(SensitiveDataRegistry.Column column, Field field, Field flagField) {
    }
}
