/**
 * Copyright (c) 2018-present, A2 Rešitve d.o.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package eu.solutions.a2.cdc.oracle;

import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.junit.Test;

import eu.solutions.a2.cdc.oracle.data.OraBlob;
import eu.solutions.a2.cdc.oracle.utils.TargetDbSqlUtils;

public class OraCdcSinkSqlInsertTest {

	@Test
	public void test() {

		final Field deptNo = new Field("DEPTNO", 0, Schema.INT8_SCHEMA);
		final Field deptId = new Field("DEPTID", 0, Schema.INT8_SCHEMA);
		final List<Field> keyFields = new ArrayList<>();
		keyFields.add(deptNo);
		keyFields.add(deptId);

		final Field dName = new Field("DNAME", 0, Schema.OPTIONAL_STRING_SCHEMA);
		final Field loc = new Field("LOC", 1, Schema.OPTIONAL_STRING_SCHEMA);
		final List<Field> valueFields = new ArrayList<>();
		valueFields.add(dName);
		valueFields.add(loc);

		final Field deptCodePdf = new Field("DEPT_CODE_PDF", 2, OraBlob.builder().build());
		final Field deptCodeDocx = new Field("DEPT_CODE_DOCX", 2, OraBlob.builder().build());
		final List<Field> lobFields = new ArrayList<>();
		lobFields.add(deptCodePdf);
		lobFields.add(deptCodeDocx);

		final List<OraColumn> allColumns = new ArrayList<>();
		final Map<String, OraColumn> pkColumns = new HashMap<>();
		final Map<String, OraColumn> lobColumns = new HashMap<>();

		for (Field field : keyFields) {
			try {
				final OraColumn column = new OraColumn(field, true);
				pkColumns.put(column.getColumnName(), column);
			} catch (SQLException sqle) {
				sqle.printStackTrace();
			}
		}
		// Only non PK columns!!!
		for (Field field : valueFields) {
			if (!pkColumns.containsKey(field.name())) {
				try {
					final OraColumn column = new OraColumn(field, false);
					allColumns.add(column);
				} catch (SQLException sqle) {
					sqle.printStackTrace();
				}
			}
		}
		for (Field field : lobFields) {
			try {
				final OraColumn column = new OraColumn(field, true);
				lobColumns.put(column.getColumnName(), column);
			} catch (SQLException sqle) {
				sqle.printStackTrace();
			}
		}

		final Map<String, String> sqlTextsOra = TargetDbSqlUtils.generateSinkSql(
				"DEPT", OraCdcJdbcSinkConnectionPool.DB_TYPE_ORACLE, pkColumns, allColumns, lobColumns);
		final Map<String, String> sqlTextsPg = TargetDbSqlUtils.generateSinkSql(
				"DEPT", OraCdcJdbcSinkConnectionPool.DB_TYPE_POSTGRESQL, pkColumns, allColumns, lobColumns);
		final Map<String, String> sqlTextsMySql = TargetDbSqlUtils.generateSinkSql(
				"DEPT", OraCdcJdbcSinkConnectionPool.DB_TYPE_MYSQL, pkColumns, allColumns, lobColumns);

		final String sinkUpsertSqlOra = sqlTextsOra.get(TargetDbSqlUtils.UPSERT);
		final String sinkUpsertSqlPg = sqlTextsPg.get(TargetDbSqlUtils.UPSERT);
		final String sinkUpsertSqlMySql = sqlTextsMySql.get(TargetDbSqlUtils.UPSERT);

		System.out.println(sinkUpsertSqlOra);
		System.out.println(sinkUpsertSqlPg);
		System.out.println(sinkUpsertSqlMySql);

		assertTrue(sinkUpsertSqlOra.contains("when matched then update"));
		assertTrue(sinkUpsertSqlPg.contains("on conflict"));
		assertTrue(sinkUpsertSqlMySql.contains("on duplicate key update"));

	}

}
