/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.plugins.datasource;
import javax.sql.DataSource;
import net.hasor.core.Environment;
import net.hasor.core.XmlNode;
/**
 * 
 * @version : 2013-10-8
 * @author 赵永春(zyc@hasor.net)
 */
public interface DataSourceFactory {
    public DataSource createDataSource(Environment env, XmlNode configElement) throws Throwable;
}