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
 *   @File: LiquibaseManager.java
 *   @Author:  lcz20@163.com
 *   @LastModified:  2020-06-13 14:46:24
 */

package cn.zenliu.reactive.service.plugin.liquibase;


import cn.zenliu.reactive.service.util.SingletonHolder;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * configuration example
 * {@code liquibase.labels  string }
 * {@code liquibase.enable  boolean  }
 * {@code liquibase.tag string }
 * {@code liquibase.rollbackFile string file }
 * {@code liquibase.testRollbackOnUpdate  boolean}
 * {@code liquibase.dropAll boolean default false}
 * {@code liquibase.*  }
 */
public interface LiquibaseManager {
    //region SPI define
    void createAndUpdate(DataSource ds, Properties prop);

    void setEnable(boolean enable);

    boolean isEnable();

    void setHoldInstance(boolean hold);

    @Nullable Liquibase getLiquibase();

    //endregion

    //region SPI template
    static void setUseSPI(boolean useSPI) {
        LiquibaseManagerImpl.disableSPI = !useSPI;
    }

    static LiquibaseManager getInstance() {
        return getHardInstance();
    }

    static LiquibaseManager getSoftInstance() {
        return LiquibaseManagerImpl.disableSPI ? LiquibaseManagerImpl.singletonHolder.getSoftInstance().orElse(null) :
                LiquibaseManagerImpl.singletonHolder.oneFromSPI(null).orElse(null);
    }

    static LiquibaseManager getHardInstance() {
        return LiquibaseManagerImpl.disableSPI ? LiquibaseManagerImpl.singletonHolder.getHardReference().orElse(null) :
                LiquibaseManagerImpl.singletonHolder.oneFromSPI(null).orElse(null);
    }
    //endregion

    @Slf4j
    final class LiquibaseManagerImpl implements LiquibaseManager {

        //region SPI template
        private static final SingletonHolder<LiquibaseManager> singletonHolder = SingletonHolder
                .generate(LiquibaseManagerImpl::new, LiquibaseManager.class);
        @Setter
        @Getter
        static volatile boolean disableSPI = false;

        //endregion


        static final String PROP_LIQUIBASE = "liquibase.";
        static final String PROP_ENABLE = PROP_LIQUIBASE + "enable";
        static final String PROP_DROP_FIRST = PROP_LIQUIBASE + "dropFirst";
        static final String PROP_CLEAR_CHECK_SUM = PROP_LIQUIBASE + "clearCheckSums";
        static final String PROP_LABELS = PROP_LIQUIBASE + "labels";
        static final String PROP_TAG = PROP_LIQUIBASE + "tag";
        static final String PROP_IGNORE_CLASSPATH_PREFIX = PROP_LIQUIBASE + "ignoreClasspathPrefix";
        static final String PROP_DATABASE_CHANGE_LOG_TABLE = PROP_LIQUIBASE + "databaseChangeLogTable";
        static final String PROP_DATABASE_CHANGE_LOG_LOCK_TABLE = PROP_LIQUIBASE + "databaseChangeLogLockTable";
        static final String PROP_TEST_ROLLBACK_ON_UPDATE = PROP_LIQUIBASE + "testRollbackOnUpdate";
        static final String PROP_ROLLBACK_FILE = PROP_LIQUIBASE + "rollbackFile";
        static final String PROP_DEFAULT_SCHEMA = PROP_LIQUIBASE + "defaultSchema";
        static final String PROP_LIQUIBASE_SCHEMA = PROP_LIQUIBASE + "liquibaseSchema";
        static final String PROP_LIQUIBASE_TABLESPACE = PROP_LIQUIBASE + "liquibaseTablespace";
        static final String PROP_PARAMETERS = PROP_LIQUIBASE + "parameters";

        private static void configuration(Properties prop, Liquibase liq) {
            final boolean ignoreClasspathPrefix = Boolean.parseBoolean(prop.getProperty(PROP_IGNORE_CLASSPATH_PREFIX, ""));
            liq.setIgnoreClasspathPrefix(ignoreClasspathPrefix);
            prop.stringPropertyNames().stream()
                    .filter(s -> s.startsWith(PROP_PARAMETERS))
                    .forEach(k -> {
                        final String name = k.replace(PROP_PARAMETERS + ".", "");
                        final String value = prop.getProperty(name);
                        if (value != null) liq.setChangeLogParameter(name, value);
                    });

        }

        private static void configuration(Properties prop, Database database) {
            final String databaseChangeLogTable = prop.getProperty(PROP_DATABASE_CHANGE_LOG_TABLE, "LIQUIBASE_CHANGE_LOG_TABLE");
            final String databaseChangeLogLockTable = prop.getProperty(PROP_DATABASE_CHANGE_LOG_LOCK_TABLE, "LIQUIBASE_CHANGE_LOCK_TABLE");
            database.setDatabaseChangeLogTableName(databaseChangeLogTable);
            database.setDatabaseChangeLogLockTableName(databaseChangeLogLockTable);
            final String defaultSchema = prop.getProperty(PROP_DEFAULT_SCHEMA);
            final String liquibaseSchema = prop.getProperty(PROP_LIQUIBASE_SCHEMA);
            final String liquibaseTablespace = prop.getProperty(PROP_LIQUIBASE_TABLESPACE);
            try {
                if (defaultSchema != null) database.setDefaultSchemaName(defaultSchema);
                if (liquibaseSchema != null) database.setLiquibaseSchemaName(liquibaseSchema);
                if (liquibaseTablespace != null) database.setLiquibaseTablespaceName(liquibaseTablespace);
            } catch (DatabaseException e) {
                throw new RuntimeException(e);
            }
        }
        //region SPI impl


        @Override
        @SneakyThrows
        public void createAndUpdate(DataSource ds, Properties prop) {
            enable = Boolean.parseBoolean(prop.getProperty(PROP_ENABLE, "false"));
            if (enable) {
                final Liquibase liq = createLiquibase(ds, prop, false);
                final String labels = prop.getProperty(PROP_LABELS);
                final String tag = prop.getProperty(PROP_TAG);
                final String rollbackFile = prop.getProperty(PROP_ROLLBACK_FILE);
                final boolean clearCheckSums = Boolean.parseBoolean(prop.getProperty(PROP_CLEAR_CHECK_SUM));
                final boolean testRollbackOnUpdate = Boolean.parseBoolean(prop.getProperty(PROP_TEST_ROLLBACK_ON_UPDATE));
                final boolean dropAll = Boolean.parseBoolean(prop.getProperty(PROP_DROP_FIRST, "false"));
                final Contexts context = new Contexts();
                final LabelExpression label = new LabelExpression(labels);
                if (clearCheckSums) liq.clearCheckSums();
                if (dropAll) liq.dropAll();
                if (rollbackFile != null && !rollbackFile.trim().isEmpty()) {
                    final File file = new File(rollbackFile);
                    final boolean status = file.createNewFile();
                    try (final FileOutputStream outStream = new FileOutputStream(file)) {
                        final OutputStreamWriter writer = new OutputStreamWriter(outStream, UTF_8);
                        if (tag != null && !tag.trim().isEmpty()) {
                            liq.futureRollbackSQL(tag, context, label, writer);
                        } else liq.futureRollbackSQL(context, label, writer);
                    }
                }
                if (testRollbackOnUpdate) {
                    if (tag != null && !tag.trim().isEmpty()) {
                        liq.updateTestingRollback(tag, context, label);
                    } else liq.updateTestingRollback(context, label);
                } else {
                    if (tag != null && !tag.trim().isEmpty()) {
                        liq.update(tag, context, label);
                    } else liq.update(context, label);
                }
            }
        }

        @Getter
        @Setter
        private boolean enable = true;


        private Liquibase createLiquibase(DataSource ds, Properties prop, boolean forced) {
            if (holdInstance && instanceHolder != null && !forced) return instanceHolder;
            final String changeLogFilePath = prop.getProperty("liquibase.changeLogFile");
            if (changeLogFilePath == null)
                throw new IllegalStateException("needed configuration of liquibase.changeLogFile not found");
            try {
                final Connection conn = ds.getConnection();
                final JdbcConnection dbConn = new JdbcConnection(conn);
                final Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(dbConn);
                configuration(prop, database);
                final ResourceAccessor resourceAccessor = new CompositeResourceAccessor(
                        new ClassLoaderResourceAccessor(),
                        new FileSystemResourceAccessor(new File("./").getAbsolutePath()));
                final Liquibase liq = new Liquibase(changeLogFilePath, resourceAccessor, database);
                configuration(prop, liq);
                if (holdInstance) {
                    instanceHolder = liq;
                }
                return liq;
            } catch (SQLException | DatabaseException e) {
                throw new RuntimeException(e);
            }
        }

        private Liquibase instanceHolder = null;
        private boolean holdInstance = false;

        @Override
        public void setHoldInstance(boolean hold) {
            holdInstance = hold;
        }

        @Override
        public @Nullable Liquibase getLiquibase() {
            return instanceHolder;
        }
        //endregion
    }
}
