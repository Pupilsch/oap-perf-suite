/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.sql.suites

import org.apache.spark.sql.{BenchmarkConfig, OapStrategyConfigSet, OapTestSuite}
import org.apache.spark.sql.execution.datasources.oap.OapStrategies
import org.apache.spark.sql.internal.oap.OapConf

object OapStrategySuite extends OapTestSuite with OapStrategyConfigSet with OapStrategies {

  override def beforeAll(): Unit = {
    super.beforeAll()
    spark.experimental.extraStrategies = oapStrategies
  }

  override def afterAll(): Unit = {
    spark.experimental.extraStrategies = null
    super.afterAll()
  }

  private def databaseName = {
    val conf = activeConf
    conf.getBenchmarkConf(BenchmarkConfig.FILE_FORMAT) match {
      case "parquet" => "parquet_tpcds_200"
      case "oap" => "oap_tpcds_200"
      case _ => "default"
    }
  }

  private def isDataBaseExists: Boolean = {
    if (spark.sqlContext.sql(s"show databases").collect().exists(_.getString(0) == databaseName)) {
      spark.sqlContext.sql(s"USE $databaseName")
      true
    } else {
      sys.error(s"ERROR: $databaseName does not exist!")
      false
    }
  }

  private def isTableReady: Boolean = {
    if (spark.sqlContext.sql(s"show tables").collect().exists(_.getString(1) == table)) {
      val conf = activeConf
      if (conf.getBenchmarkConf(BenchmarkConfig.INDEX_ENABLE) == "true") {
        if (spark.sqlContext.sql(s"show oindex from $table")
          .collect().exists(_.getString(3) == btreeIndexAttr)) {
          if (spark.sqlContext.sql(s"show oindex from $table")
            .collect().exists(_.getString(3) == bitmapIndexAttr)) {
            true
          } else {
            sys.error(s"ERROR: index on $bitmapIndexAttr does not exist!")
            false
          }
        } else {
          sys.error(s"ERROR: index on $btreeIndexAttr does not exist!")
          false
        }
      } else {
        true
      }
    } else {
      sys.error(s"ERROR: table $table does not exist!")
      false
    }
  }

  private def isDataReady(): Boolean = isDataBaseExists && isTableReady

  private def setRunningParams(): Boolean = {
    val conf = activeConf
    if (conf.getBenchmarkConf(BenchmarkConfig.INDEX_ENABLE) == "false") {
      spark.sqlContext.conf.setConf(OapConf.OAP_ENABLE_OINDEX, false)
    }

    conf.allSqlOptions().foreach{ setting =>
      spark.sqlContext.setConf(setting._1, setting._2)
    }

    spark.sqlContext.sql(s"USE $databaseName")
    true
  }

  override def prepare(): Boolean = {
    if (isDataReady()) {
      setRunningParams()
    } else {
      false
    }
  }

  val table = "store_sales"
  val leftTable = "store_sales_dup"
  val btreeIndexAttr = "ss_ticket_number"
  val bitmapIndexAttr = "ss_item_sk1"
  val range1to10 = (1 to 10).mkString(",")
  val range1to5 = (1 to 5).mkString(",")

  override def testSet: Seq[OapBenchmarkTest] = Seq(
    // Order by limit
    OapBenchmarkTest("Limit 10 from whole table",
      s"SELECT * FROM $table WHERE $btreeIndexAttr > 0 ORDER BY $btreeIndexAttr LIMIT 10"),
    OapBenchmarkTest("Limit 10 in range [0, 20000]",
      s"SELECT * FROM $table WHERE $btreeIndexAttr BETWEEN 0 AND 20000 ORDER BY $btreeIndexAttr LIMIT 10"),

    // Semi join
    // TODO: add semi join test case.

    // Aggregation
    OapBenchmarkTest("Aggregation on bitmap index in range1to5",
       s"SELECT $bitmapIndexAttr, max(${btreeIndexAttr}) FROM $table " +
            s"WHERE $bitmapIndexAttr IN ( $range1to5 ) " +
            s"GROUP BY $bitmapIndexAttr"),
    OapBenchmarkTest("Aggregation on btree index where btreeIndexAtrr < 1000000",
      s"SELECT $btreeIndexAttr, max(${bitmapIndexAttr}) FROM $table " +
        s"WHERE $btreeIndexAttr < 1000000 " +
        s"GROUP BY $btreeIndexAttr")
  )
}