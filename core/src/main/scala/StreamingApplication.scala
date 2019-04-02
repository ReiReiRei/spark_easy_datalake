import org.apache.spark.sql.functions.from_json
import org.apache.spark.sql.streaming.{
  StreamingQuery,
  StreamingQueryListener,
  Trigger
}
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

object StreamingApplication extends Logging with Spark {

  def commonListener(
      sink: Sink,
      hive: Option[Hive]
  ): StreamingQueryListener =
    new StreamingQueryListener {
      override def onQueryStarted(
          event: StreamingQueryListener.QueryStartedEvent
      ): Unit =
        logger.info(s"Query id: ${event.id} name: ${event.name}")

      override def onQueryProgress(
          event: StreamingQueryListener.QueryProgressEvent
      ): Unit =
        for {
          conf <- hive
          _ <- sink.partitionBy
        } {
          if (event.progress.numInputRows > 0) {
            logger.info(event.progress.prettyJson)
            HiveTools.repairTable(conf.db, conf.table)
          }
        }
      override def onQueryTerminated(
          event: StreamingQueryListener.QueryTerminatedEvent
      ): Unit =
        logger.info(
          s"Query terminated: ${event.id} with exception: ${event.exception.getOrElse("NONE")}"
        )
    }

  def executeSource(sourceConfig: Source, tableName: String)(
      implicit spark: SparkSession
  ): DataFrame = {
    import sourceConfig._
    import spark.implicits._

    val emptyMap: Map[String, String] = Map.empty
    val queryName = "source_" + spark.conf.get("spark.app.name")
    val raw = spark.readStream
      .format(format)
      .options(options.getOrElse(emptyMap))
      .option("queryName", queryName)
      .load()
    raw.createOrReplaceTempView(tableName)

    if (format == "kafka") { // not only kafka
      val sch: String = schema.getOrElse(
        throw new IllegalArgumentException("Schema not specified")
      )
      raw
        .selectExpr("cast (value as string) as json") //not only json
        .select(from_json($"json", sch, emptyMap) as "data")
        .select("data.*")
        .createOrReplaceTempView(tableName)
    }

    val cols = columns.getOrElse(Seq("*")).mkString(", ")
    val where = filter.fold("")(x => s"where $x")
    spark.sql(s"select $cols from $tableName $where")
  }

  def executeSink(df: DataFrame, sinkConfig: Sink)(
      implicit spark: SparkSession
  ): StreamingQuery = {
    import sinkConfig._
    val numPartitions = finalNumberOfPartitions.getOrElse(1)
    val queryName = "sink_" + spark.conf.get("spark.app.name")
    val stream = df
      .coalesce(numPartitions)
      .writeStream
      .format(format)
      .outputMode(saveMode)
      .option("checkpointLocation", checkpointLocation)
      .option("path", path)
      .option("queryName", queryName)
      .options(options.getOrElse(Map.empty[String, String]))
      .trigger(Trigger.ProcessingTime(triggerInSeconds.seconds))
      .partitionBy(partitionBy.getOrElse(Seq.empty[String]): _*)
      .start()
    stream
  }

  def run[A <: Product: ClassTag: TypeTag, B <: Product: ClassTag: TypeTag](
      convert: A => Option[B]
  ): Unit = {
    val job = StreamingJob.fromConfig()
    import job._
    import spark.implicits._
    val tableName = generateUniqueTableName()

    spark.streams.addListener(commonListener(job.sink, hive))
    val source =
      executeSource(job.source, tableName).as[A].flatMap(x => convert(x)).toDF()
    hive.foreach { conf =>
      import conf._
      HiveTools.recreateDbAndTableIfNotExists(
        warehousePath,
        db,
        table,
        sink.partitionBy,
        sink.format,
        source.schema
      )
    }
    executeSink(source, job.sink)
    spark.streams.awaitAnyTermination()
  }

  def run(): Unit = {
    val job = StreamingJob.fromConfig()
    import job._
    val tableName = generateUniqueTableName()
    spark.streams.addListener(commonListener(job.sink, hive))
    val source = executeSource(job.source, tableName)
    hive.foreach { conf =>
      import conf._
      HiveTools.recreateDbAndTableIfNotExists(
        warehousePath,
        db,
        table,
        sink.partitionBy,
        sink.format,
        source.schema
      )
    }
    executeSink(source, job.sink)
    spark.streams.awaitAnyTermination()
  }

  def generateUniqueTableName(): String =
    s"tbl${java.util.UUID.randomUUID.toString.replace('-', 'n')}"
}
