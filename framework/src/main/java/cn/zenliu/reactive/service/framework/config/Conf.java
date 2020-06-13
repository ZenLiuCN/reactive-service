/*
 *  Copyright (c) 2020.  Zen.Liu .
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 *   @Project: reactive-service-framework
 *   @Module: reactive-service-framework
 *   @File: Conf.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:46:24
 */

package cn.zenliu.reactive.service.framework.config;


import lombok.NonNull;
import lombok.experimental.Delegate;
import reactor.util.annotation.Nullable;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValueType;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigBeanFactory;
import java.util.*;


public final class Conf implements ConfigValue {
    public static Conf load() {
        return new Conf();
    }

    @Delegate(types = {ConfigValue.class})
    private final ConfigValue config;
    private final ConfigOrigin origin;
    private final ConfigValueType type;
    private final ConfigObject obj;
    private final ConfigList lst;

    private Conf() {
        final Config cfg = ConfigFactory.load();
        config = cfg.root();
        origin = cfg.origin();
        type = config.valueType();
        if (type == ConfigValueType.LIST) {
            lst = (ConfigList) config;
            obj = null;
        } else if (type == ConfigValueType.OBJECT) {
            obj = (ConfigObject) config;
            lst = null;
        } else {
            obj = null;
            lst = null;
        }
    }

    private Conf(Config conf) {
        config = conf.getValue(null);
        origin = conf.origin();
        type = config.valueType();
        if (type == ConfigValueType.LIST) {
            lst = (ConfigList) config;
            obj = null;
        } else if (type == ConfigValueType.OBJECT) {
            obj = (ConfigObject) config;
            lst = null;
        } else {
            obj = null;
            lst = null;
        }
    }

    private Conf(ConfigValue conf) {
        config = conf;
        this.origin = conf.origin();
        type = config.valueType();
        if (type == ConfigValueType.LIST) {
            lst = (ConfigList) config;
            obj = null;
        } else if (type == ConfigValueType.OBJECT) {
            obj = (ConfigObject) config;
            lst = null;
        } else {
            obj = null;
            lst = null;
        }
    }

    public Conf getConf(@NonNull String path, int idx) {
        if (obj != null) {
            try {
                return new Conf(obj.toConfig().getObject(path));
            } catch (ConfigException.WrongType e) {
                return new Conf(obj.toConfig().getList(path));
            }
        } else if (lst != null) {
            return new Conf(lst.get(idx));
        } else {
            return this; //TODO: do what when is a raw value
        }
    }

    public boolean getBoolean() {
        if (type != ConfigValueType.BOOLEAN) return false;
        return (Boolean) config.unwrapped();
    }

    @Nullable
    public Number getNumber() {
        if (type != ConfigValueType.NUMBER) return null;
        return (Number) config.unwrapped();
    }

    public boolean isNull() {
        return type == ConfigValueType.NULL;
    }

    public boolean isValue() {
        return type != ConfigValueType.LIST && type != ConfigValueType.OBJECT;
    }

    public boolean isList() {
        return type == ConfigValueType.LIST;
    }

    public boolean isObject() {
        return type == ConfigValueType.OBJECT;
    }

    public ConfigValue getRaw() {
        return config;
    }

    /**
     * @param clz   container class
     * @param inner inner class of container
     * @param <T>   container class
     * @param <R>   inner class
     * @return bean collection
     * @throws IllegalAccessException container can't access
     * @throws InstantiationException container have not valid access
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends Collection<R>, R> T mapToList(@NonNull Class<T> clz, @NonNull Class<R> inner) throws IllegalAccessException, InstantiationException {
        if (Collection.class.isAssignableFrom(clz) && lst != null) {
            final Collection<R> c = clz.newInstance();
            if (lst.isEmpty()) return (T) c;
            if (lst.get(0).valueType() != ConfigValueType.OBJECT) return (T) c;
            lst.forEach(e -> ConfigBeanFactory.create(((ConfigObject) e).toConfig(), inner));
            return (T) c;
        } else return null;
    }

    /**
     * @param clz class of bean type
     * @param <T> bean type
     * @return null if is collection else return clz instance
     */
    @Nullable
    public <T> T mapToObject(@NonNull Class<T> clz) {
        if (!Collection.class.isAssignableFrom(clz) && obj != null) {
            return ConfigBeanFactory.create(obj.toConfig(), clz);
        } else return null;
    }

    /**
     * setOf contains pair
     * if conf is value: return empty set
     * if conf is list: return index as String key
     * if conf is object: return raw key as key
     *
     * @return set of key value pair
     */
    @NonNull
    public Set<Map.Entry<String, Conf>> entries() {
        final HashMap<String, Conf> c = new HashMap<>();
        if (obj != null) {
            obj.forEach((key, value) -> c.put(key, new Conf(value)));
        } else if (lst != null) {
            final int[] idx = {-1};
            lst.forEach(e -> {
                idx[0] += 1;
                c.put(String.valueOf(idx[0]), new Conf(e));
            });
        }
        return c.entrySet();
    }

    @Override
    public String toString() {
        return config.render();
    }

    private void objectToProperties(Properties prop, ConfigObject obj, String parent) {
        final String parentKey = parent == null ? "" : parent + ".";
        obj.forEach((k, v) -> {
            final String p = parentKey + k;
            final ConfigValueType t = v.valueType();
            switch (t) {
                case OBJECT:
                    objectToProperties(prop, (ConfigObject) v, p);
                    break;
                case LIST:
                    collectionToProperties(prop, (ConfigList) v, p);
                    break;
                default:
                    prop.setProperty(p, v.unwrapped().toString());
            }
        });
    }

    private void collectionToProperties(Properties prop, ConfigList obj, @NonNull String parent) {
        final StringBuilder b = new StringBuilder();
        obj.forEach(v -> {
            final ConfigValueType t = v.valueType();
            if (t == ConfigValueType.OBJECT || t == ConfigValueType.LIST)
                throw new IllegalArgumentException("configuration with nested object or list in list is not supported");
            b.append(v.unwrapped().toString());
            b.append(",");
        });
        prop.put(parent, b.toString());
    }

    public Properties toProperties(@Nullable String root) {
        final Properties prop = new Properties();
        if (isObject()) {
            objectToProperties(prop, obj, root);
        } else if (isList()) {
            collectionToProperties(prop, lst, root == null ? "value" : root);
        }
        return prop;
    }
}