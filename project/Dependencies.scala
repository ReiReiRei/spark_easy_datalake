import sbt._
object Versions {
  val sparkV = "2.3.1.3.0.1.0-187"
  val pureconfigV = "0.9.2"
}
object Dependencies {
  import Versions._
  val sparkCore = "org.apache.spark" %% "spark-core" % sparkV
  val sparkSql = "org.apache.spark" %% "spark-sql" % sparkV
  val sparkSqlKafka = "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkV
  val sparkSqlHive = "org.apache.spark" %% "spark-hive" % sparkV
  val pureconfig = "com.github.pureconfig" %% "pureconfig" % pureconfigV
}
