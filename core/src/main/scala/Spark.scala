import org.apache.spark.sql.SparkSession

trait Spark {
  implicit lazy val spark: SparkSession =
    SparkSession.builder().enableHiveSupport().getOrCreate()
}
