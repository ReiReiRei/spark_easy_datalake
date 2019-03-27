import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.StructType

case class Hive(db: String, table: String, warehousePath: String)

object HiveTools {
  def repairTable(db: String, table: String)(
      implicit spark: SparkSession
  ): Unit = {
    spark.sql(s"msck repair table $db.$table")
    ()
  }
  def createHiveDbIfNotExists(db: String, dbLocation: String)(
      implicit spark: SparkSession
  ): Unit = {
    val dbQuery = s"create database if not exists $db location '$dbLocation'"
    spark.sql(dbQuery)
    ()
  }
  def recreateDbAndTableIfNotExists(
      warehousePath: String,
      db: String,
      table: String,
      partitionBy: Option[Seq[String]],
      format: String,
      schema: StructType
  )(implicit spark: SparkSession): Unit = {
    val dbPath = s"$warehousePath/$db"
    val tablePath = s"$dbPath/$table"
    val allCols =
      schema.map(x => x.name.concat(" ").concat(x.dataType.simpleString))
    val parts = partitionBy
      .getOrElse(Seq.empty[String])
      .map(x => allCols.dropWhile(a => !a.contains(x)))
      .filter(x => x.nonEmpty)
      .map(x => x.head)
    val cols = allCols diff parts
    val partsQuery =
      if (parts.isEmpty) "" else s"partitioned by (${parts.mkString(", ")})"
    val tableQuery =
      s"""create external table if not exists $db.$table
         |(${cols.mkString(", \n")})
         |$partsQuery
         |stored as $format location '$tablePath' tblproperties ('$format.compress'='SNAPPY')
       """.stripMargin
    createHiveDbIfNotExists(db, dbPath)
    spark.sql(s"drop table if exists $db.$table")
    spark.sql(tableQuery)
    if (partitionBy.getOrElse(Seq.empty[String]).nonEmpty)
      repairTable(db, table)
  }

}
